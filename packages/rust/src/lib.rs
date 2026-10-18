use std::fmt;
use std::time::Duration;

use base64::engine::general_purpose::{STANDARD, URL_SAFE_NO_PAD};
use base64::Engine;
use chrono::{DateTime, Utc};
use ed25519_dalek::{Signature, Verifier, VerifyingKey};
use serde::de::DeserializeOwned;
use serde::Deserialize;
use serde_json::{json, Map, Value};

#[derive(Debug)]
pub enum Error {
    Http(String),
    Status(u16, String),
    Signature,
    Offline(String),
    Json(String),
}

impl fmt::Display for Error {
    fn fmt(&self, formatter: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Error::Http(message) => write!(formatter, "Request to Kora failed: {message}"),
            Error::Status(status, message) => write!(formatter, "{message} ({status})"),
            Error::Signature => write!(formatter, "Response signature verification failed"),
            Error::Offline(message) => write!(formatter, "{message}"),
            Error::Json(message) => write!(formatter, "Kora sent an unexpected answer: {message}"),
        }
    }
}

impl std::error::Error for Error {}

#[derive(Debug, Deserialize)]
pub struct Validation {
    pub valid: bool,
    pub code: String,
    pub message: String,
    pub license: Option<Value>,
    pub timestamp: i64,
}

#[derive(Debug, Deserialize)]
pub struct Deactivation {
    pub success: bool,
    pub code: String,
    pub message: String,
    pub removed: i64,
    pub timestamp: i64,
}

pub struct Client {
    base: String,
    api_key: Option<String>,
    product: Option<String>,
    public_key: Option<String>,
    agent: ureq::Agent,
}

pub struct Reply {
    pub status: u16,
    pub text: String,
    pub signature: Option<String>,
}

impl Client {
    pub fn new(url: &str) -> Self {
        let trimmed = url.trim_end_matches('/');
        Client {
            base: match trimmed.strip_suffix("/api/v1") {
                Some(base) => base.to_string(),
                None => trimmed.to_string(),
            },
            api_key: None,
            product: None,
            public_key: None,
            agent: ureq::AgentBuilder::new().timeout(Duration::from_secs(15)).build(),
        }
    }

    pub fn api_key(mut self, key: impl Into<String>) -> Self {
        self.api_key = Some(key.into());
        self
    }

    pub fn product(mut self, product: impl Into<String>) -> Self {
        self.product = Some(product.into());
        self
    }

    pub fn public_key(mut self, key: impl Into<String>) -> Self {
        self.public_key = Some(key.into());
        self
    }

    pub fn licenses(&self) -> Resource<'_> {
        Resource { client: self, name: "licenses", single: "license" }
    }

    pub fn products(&self) -> Resource<'_> {
        Resource { client: self, name: "products", single: "product" }
    }

    pub fn customers(&self) -> Resource<'_> {
        Resource { client: self, name: "customers", single: "customer" }
    }

    pub fn send(&self, method: &str, path: &str, query: &[(&str, &str)], body: Option<Value>) -> Result<Reply, Error> {
        let mut request = self.agent.request(method, &format!("{}/api/v1{}", self.base, path)).set("Accept", "application/json");
        if let Some(key) = &self.api_key {
            request = request.set("Authorization", &format!("Bearer {key}"));
        }
        for (name, value) in query {
            request = request.query(name, value);
        }
        let result = match body {
            Some(value) => request.send_json(value),
            None => request.call(),
        };
        let response = match result {
            Ok(response) => response,
            Err(ureq::Error::Status(_, response)) => response,
            Err(error) => return Err(Error::Http(error.to_string())),
        };
        let status = response.status();
        let signature = response.header("x-kora-signature").map(str::to_string);
        let text = response.into_string().map_err(|error| Error::Http(error.to_string()))?;
        Ok(Reply { status, text, signature })
    }

    fn fail(reply: &Reply) -> Error {
        let message = serde_json::from_str::<Value>(&reply.text).ok().and_then(|value| value.get("error").and_then(Value::as_str).map(str::to_string));
        match message {
            Some(text) => Error::Status(reply.status, text),
            None => Error::Status(reply.status, format!("Kora answered with status {}", reply.status)),
        }
    }

    pub fn call(&self, method: &str, path: &str, query: &[(&str, &str)], body: Option<Value>) -> Result<Value, Error> {
        let reply = self.send(method, path, query, body)?;
        if reply.status >= 400 {
            return Err(Self::fail(&reply));
        }
        if reply.text.is_empty() {
            return Ok(Value::Null);
        }
        serde_json::from_str(&reply.text).map_err(|error| Error::Json(error.to_string()))
    }

    fn key(&self) -> Result<VerifyingKey, Error> {
        let pem = match &self.public_key {
            Some(key) => key,
            None => return Err(Error::Offline("A public key is required".to_string())),
        };
        let body: String = pem.lines().filter(|line| !line.starts_with("-----")).map(str::trim).collect();
        let der = STANDARD.decode(body).map_err(|_| Error::Signature)?;
        if der.len() < 32 {
            return Err(Error::Signature);
        }
        let raw: [u8; 32] = der[der.len() - 32..].try_into().map_err(|_| Error::Signature)?;
        VerifyingKey::from_bytes(&raw).map_err(|_| Error::Signature)
    }

    fn verify(&self, message: &[u8], signature: &[u8]) -> Result<(), Error> {
        let signature = Signature::from_slice(signature).map_err(|_| Error::Signature)?;
        self.key()?.verify(message, &signature).map_err(|_| Error::Signature)
    }

    fn check<T: DeserializeOwned>(&self, path: &str, body: Value) -> Result<T, Error> {
        let reply = self.send("POST", path, &[], Some(body))?;
        if reply.status == 429 || reply.status >= 500 {
            return Err(Self::fail(&reply));
        }
        if self.public_key.is_some() {
            let signature = reply.signature.as_deref().ok_or(Error::Signature)?;
            self.verify(reply.text.as_bytes(), &STANDARD.decode(signature).map_err(|_| Error::Signature)?)?;
        }
        serde_json::from_str(&reply.text).map_err(|error| Error::Json(error.to_string()))
    }

    fn body(&self, key: &str, fields: &[(&str, &str)], product: bool) -> Value {
        let mut body: Map<String, Value> = fields.iter().map(|(name, value)| (name.to_string(), json!(value))).collect();
        body.insert("license_key".to_string(), json!(key));
        if let (true, Some(name), false) = (product, &self.product, body.contains_key("product_name")) {
            body.insert("product_name".to_string(), json!(name));
        }
        Value::Object(body)
    }

    pub fn validate(&self, key: &str, fields: &[(&str, &str)]) -> Result<Validation, Error> {
        self.check("/licenses/validate", self.body(key, fields, true))
    }

    pub fn activate(&self, key: &str, fields: &[(&str, &str)]) -> Result<Validation, Error> {
        self.check("/licenses/activate", self.body(key, fields, true))
    }

    pub fn deactivate(&self, key: &str, fields: &[(&str, &str)]) -> Result<Deactivation, Error> {
        self.check("/licenses/deactivate", self.body(key, fields, false))
    }

    pub fn file(&self, id: &str) -> Result<String, Error> {
        let reply = self.send("GET", &format!("/licenses/{id}/file"), &[], None)?;
        if reply.status >= 400 {
            return Err(Self::fail(&reply));
        }
        Ok(reply.text.trim().to_string())
    }

    fn date(license: &Value, field: &str) -> Result<Option<DateTime<Utc>>, Error> {
        match license.get(field).and_then(Value::as_str) {
            Some(text) => DateTime::parse_from_rfc3339(text).map(|date| Some(date.with_timezone(&Utc))).map_err(|error| Error::Offline(error.to_string())),
            None => Ok(None),
        }
    }

    pub fn offline(&self, file: &str) -> Result<Value, Error> {
        let parts: Vec<&str> = file.trim().split('.').collect();
        if parts.len() != 2 {
            return Err(Error::Offline("Offline license file is malformed".to_string()));
        }
        let payload = URL_SAFE_NO_PAD.decode(parts[0]).map_err(|_| Error::Offline("Offline license file is malformed".to_string()))?;
        let signature = URL_SAFE_NO_PAD.decode(parts[1]).map_err(|_| Error::Offline("Offline license file is malformed".to_string()))?;
        self.verify(&payload, &signature)?;
        let license: Value = serde_json::from_slice(&payload).map_err(|error| Error::Json(error.to_string()))?;
        let now = Utc::now();
        if matches!(Self::date(&license, "refresh_after")?, Some(date) if date < now) {
            return Err(Error::Offline("Offline license needs a refresh".to_string()));
        }
        if matches!(Self::date(&license, "expires_at")?, Some(date) if date < now) {
            return Err(Error::Offline("Offline license expired".to_string()));
        }
        Ok(license)
    }

    pub fn merge(&self, customer: &str, duplicate: &str) -> Result<Value, Error> {
        let data = self.call("POST", &format!("/customers/{customer}/merge"), &[], Some(json!({ "customer_id": duplicate })))?;
        Ok(data["customer"].clone())
    }
}

pub struct Resource<'a> {
    client: &'a Client,
    name: &'static str,
    single: &'static str,
}

impl Resource<'_> {
    pub fn list(&self, query: &[(&str, &str)]) -> Result<(i64, Vec<Value>), Error> {
        let data = self.client.call("GET", &format!("/{}", self.name), query, None)?;
        let rows = data[self.name].as_array().cloned().ok_or_else(|| Error::Json(format!("missing {}", self.name)))?;
        let total = data["total"].as_i64().ok_or_else(|| Error::Json("missing total".to_string()))?;
        Ok((total, rows))
    }

    pub fn get(&self, id: &str) -> Result<Value, Error> {
        self.client.call("GET", &format!("/{}/{}", self.name, id), &[], None)
    }

    pub fn create(&self, fields: Value) -> Result<Value, Error> {
        Ok(self.client.call("POST", &format!("/{}", self.name), &[], Some(fields))?[self.single].clone())
    }

    pub fn update(&self, id: &str, fields: Value) -> Result<Value, Error> {
        Ok(self.client.call("PATCH", &format!("/{}/{}", self.name, id), &[], Some(fields))?[self.single].clone())
    }

    pub fn delete(&self, id: &str, force: bool) -> Result<(), Error> {
        let query: &[(&str, &str)] = if force { &[("force", "true")] } else { &[] };
        self.client.call("DELETE", &format!("/{}/{}", self.name, id), query, None).map(|_| ())
    }
}
