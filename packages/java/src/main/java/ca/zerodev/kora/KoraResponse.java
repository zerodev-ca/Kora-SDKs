package ca.zerodev.kora;

import java.util.List;
import java.util.Map;

public class KoraResponse {
    private boolean valid;
    private String code;
    private String message;
    private String product;
    private String user;
    private String status;
    private String expires_at;
    private List<String> addons;
    private String nonce;
    private Long timestamp;
    private Map<String, Object> variables;
    private Map<String, Object> user_variables;
    private SessionData session;

    public static class SessionData {
        private String token;

        public String getToken() {
            return this.token;
        }
    }

    public boolean isValid() {
        return this.valid;
    }

    public String getCode() {
        return this.code;
    }

    public boolean hasAddon(String name) {
        return this.addons != null && this.addons.contains(name);
    }

    public String getMessage() {
        return this.message;
    }

    public String getProduct() {
        return this.product;
    }

    public String getUser() {
        return this.user;
    }

    public String getStatus() {
        return this.status;
    }

    public String getExpiresAt() {
        return this.expires_at;
    }

    public List<String> getAddons() {
        return this.addons;
    }

    public String getNonce() {
        return this.nonce;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public Map<String, Object> getVariables() {
        return this.variables;
    }

    public Map<String, Object> getUserVariables() {
        return this.user_variables;
    }

    public String getSessionToken() {
        if (this.session == null) {
            return null;
        }
        return this.session.getToken();
    }
}
