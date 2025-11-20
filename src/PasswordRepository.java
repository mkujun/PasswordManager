import java.util.HashMap;

public class PasswordRepository {

    private final PersistenceService persistence;
    private byte[] salt;
    private String encryptedMasterPassword;

    private final HashMap<String, PasswordEntry> entries;

    public PasswordRepository(PersistenceService persistence) {
        this.persistence = persistence;
        PersistenceService.LoadedData data = persistence.load();

        if (data != null) {
            this.salt = data.salt;
            this.encryptedMasterPassword = data.encryptedMasterPassword;
            this.entries = data.entries;
        } else {
            this.entries = new HashMap<String, PasswordEntry>();
        }
    }

    public boolean add(PasswordEntry entry) {
        return entries.putIfAbsent(entry.getUsername(), entry) == null;
    }

    public boolean remove(String accountName) {
        return entries.remove(accountName) != null;
    }

    public boolean update(String accountName, String username, String encryptedPassword) {
        return entries.replace(accountName, new PasswordEntry(accountName, username, encryptedPassword)) != null;
    }

    public PasswordEntry find(String accountName) {
        return entries.get(accountName);
    }

    public void save() {
        persistence.save(salt, encryptedMasterPassword, entries);
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public HashMap<String, PasswordEntry> getEntries() {
        return entries;
    }

    public String getEncryptedMasterPassword() {
        return encryptedMasterPassword;
    }

    public void setEncryptedMasterPassword(String encryptedMasterPassword) {
        this.encryptedMasterPassword = encryptedMasterPassword;
    }
}

