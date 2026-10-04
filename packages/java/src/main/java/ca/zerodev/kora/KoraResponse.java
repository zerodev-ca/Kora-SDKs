package ca.zerodev.kora;

import java.util.List;

public class KoraResponse {
    private boolean valid;
    private String code;
    private String message;
    private License license;
    private long timestamp;

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

    public static class Addon {
        private long id;
        private String name;
        private double price;
        private String currency;

        public long getId() {
            return this.id;
        }

        public String getName() {
            return this.name;
        }

        public double getPrice() {
            return this.price;
        }

        public String getCurrency() {
            return this.currency;
        }
    }

    public static class License {
        private String key;
        private String status;
        private String expires_at;
        private Product product;
        private Customer customer;
        private List<Addon> addons;

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

        public List<Addon> getAddons() {
            return this.addons;
        }
    }

    public boolean isValid() {
        return this.valid;
    }

    public String getCode() {
        return this.code;
    }

    public String getMessage() {
        return this.message;
    }

    public License getLicense() {
        return this.license;
    }

    public long getTimestamp() {
        return this.timestamp;
    }
}
