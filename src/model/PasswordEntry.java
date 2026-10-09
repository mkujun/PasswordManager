package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PasswordEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private String accountName;
    private String username;
    private String password;
    private String notes;
    private String url;
    private LocalDateTime createdAt =  LocalDateTime.now();
    private LocalDateTime updatedAt =   LocalDateTime.now();
    private boolean accountActive = true;

    private PasswordEntry(Builder builder) {
        this.accountName = builder.accountName;
        this.username = builder.username;
        this.password = builder.password;
        this.notes = builder.notes;
        this.url = builder.url;
        this.createdAt = builder.createdAt;
        this.updatedAt = builder.updatedAt;
        this.accountActive = builder.accountActive;
    }

    public static class Builder {
        private String accountName;
        private String username;
        private String password;
        private String notes;
        private String url;
        private LocalDateTime createdAt =  LocalDateTime.now();

        private LocalDateTime updatedAt =   LocalDateTime.now();
        private boolean accountActive = true;

        public Builder(String accountName) {
            this.accountName = accountName;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }
        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        public Builder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        public Builder accountActive(boolean accountActive) {
            this.accountActive = accountActive;
            return this;
        }

        public PasswordEntry build() {
            return new PasswordEntry(this);
        }
    }

    public String getAccountName() {
        return accountName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isAccountActive() {
        return accountActive;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

}


