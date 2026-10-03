package ca.zerodev.kora;

public class KoraResponse {
    private boolean valid;
    private String message;
    private transient int code;
    private License license;

    public static class Product {
        private long id;
        private String name;

        public long getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
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
}
