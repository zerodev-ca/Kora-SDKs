package kora

import (
	"bytes"
	"context"
	"crypto/ed25519"
	"crypto/x509"
	"encoding/base64"
	"encoding/json"
	"encoding/pem"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type Options struct {
	URL       string
	APIKey    string
	Product   string
	PublicKey string
	Timeout   time.Duration
}

type Client struct {
	options   Options
	base      string
	http      *http.Client
	Licenses  *Resource
	Products  *Resource
	Customers *Resource
}

type Error struct {
	Message string
	Status  int
}

func (e *Error) Error() string {
	return e.Message
}

func New(options Options) *Client {
	timeout := options.Timeout
	if timeout == 0 {
		timeout = 15 * time.Second
	}
	client := &Client{
		options: options,
		base:    strings.TrimSuffix(strings.TrimRight(options.URL, "/"), "/api/v1"),
		http:    &http.Client{Timeout: timeout},
	}
	client.Licenses = &Resource{client: client, name: "licenses", single: "license"}
	client.Products = &Resource{client: client, name: "products", single: "product"}
	client.Customers = &Resource{client: client, name: "customers", single: "customer"}
	return client
}

type reply struct {
	status    int
	body      []byte
	signature string
}

func (c *Client) send(ctx context.Context, method string, path string, body any) (*reply, error) {
	var reader io.Reader
	if body != nil {
		encoded, err := json.Marshal(body)
		if err != nil {
			return nil, err
		}
		reader = bytes.NewReader(encoded)
	}
	request, err := http.NewRequestWithContext(ctx, method, c.base+"/api/v1"+path, reader)
	if err != nil {
		return nil, err
	}
	request.Header.Set("Content-Type", "application/json")
	request.Header.Set("Accept", "application/json")
	if c.options.APIKey != "" {
		request.Header.Set("Authorization", "Bearer "+c.options.APIKey)
	}
	response, err := c.http.Do(request)
	if err != nil {
		return nil, err
	}
	defer response.Body.Close()
	data, err := io.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}
	return &reply{status: response.StatusCode, body: data, signature: response.Header.Get("x-kora-signature")}, nil
}

func (c *Client) fail(r *reply) error {
	var data struct {
		Error string `json:"error"`
	}
	if json.Unmarshal(r.body, &data) == nil && data.Error != "" {
		return &Error{Message: data.Error, Status: r.status}
	}
	return &Error{Message: fmt.Sprintf("Kora answered with status %d", r.status), Status: r.status}
}

func (c *Client) call(ctx context.Context, method string, path string, body any, out any) error {
	r, err := c.send(ctx, method, path, body)
	if err != nil {
		return err
	}
	if r.status >= 400 {
		return c.fail(r)
	}
	if out == nil {
		return nil
	}
	return json.Unmarshal(r.body, out)
}

func (c *Client) key() (ed25519.PublicKey, error) {
	block, _ := pem.Decode([]byte(c.options.PublicKey))
	if block == nil {
		return nil, errors.New("public key is not PEM")
	}
	parsed, err := x509.ParsePKIXPublicKey(block.Bytes)
	if err != nil {
		return nil, err
	}
	key, ok := parsed.(ed25519.PublicKey)
	if !ok {
		return nil, errors.New("public key is not Ed25519")
	}
	return key, nil
}

func (c *Client) verify(r *reply) error {
	if c.options.PublicKey == "" {
		return nil
	}
	key, err := c.key()
	if err != nil {
		return err
	}
	signature, err := base64.StdEncoding.DecodeString(r.signature)
	if err != nil || !ed25519.Verify(key, r.body, signature) {
		return &Error{Message: "Response signature verification failed", Status: r.status}
	}
	return nil
}

func (c *Client) check(ctx context.Context, path string, body map[string]any, out any) error {
	r, err := c.send(ctx, http.MethodPost, path, body)
	if err != nil {
		return err
	}
	if r.status == http.StatusTooManyRequests || r.status >= 500 {
		return c.fail(r)
	}
	if err := c.verify(r); err != nil {
		return err
	}
	return json.Unmarshal(r.body, out)
}

func (c *Client) body(key string, fields map[string]string) map[string]any {
	body := map[string]any{}
	for name, value := range fields {
		body[name] = value
	}
	body["license_key"] = key
	if _, set := fields["product_name"]; !set && c.options.Product != "" {
		body["product_name"] = c.options.Product
	}
	return body
}

func (c *Client) Validate(ctx context.Context, key string, fields map[string]string) (*ValidationResponse, error) {
	var result ValidationResponse
	if err := c.check(ctx, "/licenses/validate", c.body(key, fields), &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (c *Client) Activate(ctx context.Context, key string, fields map[string]string) (*ValidationResponse, error) {
	var result ValidationResponse
	if err := c.check(ctx, "/licenses/activate", c.body(key, fields), &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (c *Client) Deactivate(ctx context.Context, key string, fields map[string]string) (*DeactivationResponse, error) {
	var result DeactivationResponse
	body := c.body(key, fields)
	delete(body, "product_name")
	if err := c.check(ctx, "/licenses/deactivate", body, &result); err != nil {
		return nil, err
	}
	return &result, nil
}

func (c *Client) File(ctx context.Context, id string) (string, error) {
	r, err := c.send(ctx, http.MethodGet, "/licenses/"+url.PathEscape(id)+"/file", nil)
	if err != nil {
		return "", err
	}
	if r.status >= 400 {
		return "", c.fail(r)
	}
	return strings.TrimSpace(string(r.body)), nil
}

func (c *Client) Offline(file string) (*OfflineLicense, error) {
	if c.options.PublicKey == "" {
		return nil, errors.New("a PublicKey is required to read offline licenses")
	}
	parts := strings.Split(strings.TrimSpace(file), ".")
	if len(parts) != 2 {
		return nil, errors.New("offline license file is malformed")
	}
	payload, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return nil, err
	}
	signature, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return nil, err
	}
	key, err := c.key()
	if err != nil {
		return nil, err
	}
	if !ed25519.Verify(key, payload, signature) {
		return nil, errors.New("offline license signature verification failed")
	}
	var license OfflineLicense
	if err := json.Unmarshal(payload, &license); err != nil {
		return nil, err
	}
	if license.RefreshAfter.Before(time.Now()) {
		return nil, errors.New("offline license needs a refresh")
	}
	if license.ExpiresAt != nil && license.ExpiresAt.Before(time.Now()) {
		return nil, errors.New("offline license expired")
	}
	return &license, nil
}

func (c *Client) Merge(ctx context.Context, customer string, duplicate string) (map[string]any, error) {
	var result struct {
		Customer map[string]any `json:"customer"`
	}
	if err := c.call(ctx, http.MethodPost, "/customers/"+url.PathEscape(customer)+"/merge", map[string]any{"customer_id": duplicate}, &result); err != nil {
		return nil, err
	}
	return result.Customer, nil
}
