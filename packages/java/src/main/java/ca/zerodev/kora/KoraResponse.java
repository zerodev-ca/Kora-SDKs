package ca.zerodev.kora;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class KoraResponse {
    private boolean valid;
    private String message;
    private transient int code;
    private License license;
    private String nonce;
    private Long timestamp;
    private JsonObject offline;
    private boolean update_available;
    private String current;
    private Release latest;
    private String download_url;

    public static class Product {
        private long id;
        private String name;
        private String version;

        public long getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String getVersion() {
            return this.version;
        }
    }

    public static class Customer {
        private long id;
        private String name;
        private String email;
        private String discord_id;

        public long getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public String getEmail() {
            return this.email;
        }

        public String getDiscordId() {
            return this.discord_id;
        }
    }

    public static class License {
        private String key;
        private String status;
        private String expires_at;
        private Product product;
        private Customer customer;
        private JsonObject data;

        public String getKey() {
            return this.key;
        }

        public String getStatus() {
            return this.status;
        }

        public String getExpiresAt() {
            return this.expires_at;
        }

        public Product getProduct() {
            return this.product;
        }

        public Customer getCustomer() {
            return this.customer;
        }

        public JsonObject getData() {
            return this.data;
        }
    }

    public static class Release {
        private String version;
        private String channel;
        private String notes;
        private boolean required;
        private String file_name;
        private long size;
        private String sha256;
        private String published_at;

        public String getVersion() {
            return this.version;
        }

        public String getChannel() {
            return this.channel;
        }

        public String getNotes() {
            return this.notes;
        }

        public boolean isRequired() {
            return this.required;
        }

        public String getFileName() {
            return this.file_name;
        }

        public long getSize() {
            return this.size;
        }

        public String getSha256() {
            return this.sha256;
        }

        public String getPublishedAt() {
            return this.published_at;
        }
    }

    KoraResponse settle(int code, String fallback) {
        this.code = code;
        if (this.message == null) {
            this.message = fallback;
        }
        return this;
    }

    public boolean isValid() {
        return this.valid;
    }

    public String getMessage() {
        return this.message;
    }

    public int getCode() {
        return this.code;
    }

    public License getLicense() {
        return this.license;
    }

    public String getNonce() {
        return this.nonce;
    }

    public Long getTimestamp() {
        return this.timestamp;
    }

    public String getOffline() {
        if (this.offline == null) {
            return null;
        }
        return new GsonBuilder().disableHtmlEscaping().create().toJson(this.offline);
    }

    public boolean isUpdateAvailable() {
        return this.update_available;
    }

    public String getCurrent() {
        return this.current;
    }

    public Release getLatest() {
        return this.latest;
    }

    public String getDownloadUrl() {
        return this.download_url;
    }
}
