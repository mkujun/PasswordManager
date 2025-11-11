import java.util.List;
import java.util.Optional;

public class PasswordRepository {

    private final PersistenceService persistence;
    private byte[] salt;
    private String encryptedMasterPassword;
    private List<PasswordEntry> entries;

    public PasswordRepository(PersistenceService persistence) {
        this.persistence = persistence;
        PersistenceService.LoadedData data = persistence.load();

        if (data != null) {
            this.salt = data.salt;
            this.encryptedMasterPassword = data.encryptedMasterPassword;
            this.entries = data.entries;
        } else {
            this.entries = new java.util.ArrayList<>();
        }
    }

    public List<PasswordEntry> getAll() {
        return entries;
    }

    public Optional<PasswordEntry> findByAccountName(String accountName) {
        return entries.stream()
                .filter(e -> e.getAccountName().equalsIgnoreCase(accountName))
                .findFirst();
    }

    public boolean add(PasswordEntry entry) {
        if (findByAccountName(entry.getAccountName()).isPresent()) {
            return false;
        }
        entries.add(entry);
        return true;
    }

    public boolean remove(String accountName) {
        return entries.removeIf(e -> e.getAccountName().equalsIgnoreCase(accountName));
    }

    public boolean update(String accountName, String username, String encryptedPassword) {
        return findByAccountName(accountName)
                .map(e -> {
                    e.setUsername(username);
                    e.setEncryptedPassword(encryptedPassword);
                    return true;
                })
                .orElse(false);
    }

    public void save() {
        persistence.save(salt, encryptedMasterPassword, entries);
    }

    // --- Accessors for CryptoService ---
    public byte[] getSalt() { return salt; }
    public void setSalt(byte[] salt) { this.salt = salt; }

    public String getEncryptedMasterPassword() { return encryptedMasterPassword; }
    public void setEncryptedMasterPassword(String encryptedMasterPassword) {
        this.encryptedMasterPassword = encryptedMasterPassword;
    }
}

