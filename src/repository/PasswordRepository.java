package repository;

import interfaces.IPasswordRepository;
import interfaces.IPersistenceService;
import persistence.PersistenceService;

import java.util.HashMap;
import java.util.Map;

public class PasswordRepository implements IPasswordRepository {

    private final IPersistenceService persistence;
    private byte[] salt;
    private String encryptedMasterPassword;

    private HashMap<String, String> encryptedEntries;

    public PasswordRepository(IPersistenceService persistence) {
        this.persistence = persistence;
        PersistenceService.LoadedData data = persistence.load();

        if (data != null) {
            this.salt = data.salt;
            this.encryptedMasterPassword = data.encryptedMasterPassword;
            this.encryptedEntries = data.encryptedEntries;
        } else {
            this.encryptedEntries = new HashMap<>();
        }
    }

    @Override
    public boolean addEncryptedEntry(String encryptedPasswordEntry, String accountName) {
        return encryptedEntries.putIfAbsent(accountName, encryptedPasswordEntry) == null;
    }

    public boolean remove(String accountName) {
        return encryptedEntries.remove(accountName) != null;
    }

    public Map<String, String> find(String accountName) {
        Map<String, String> searchEntries = new HashMap<>();

        encryptedEntries.forEach((key, value) -> {
           if (key.contains(accountName)) {
               searchEntries.put(key, value);
           }
        });
        return searchEntries;
    }

    public void save() {
        persistence.save(salt, encryptedMasterPassword, encryptedEntries);
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public HashMap<String, String> getEntries() {
        return encryptedEntries;
    }

    public void dump() {
        this.encryptedEntries.clear();
        this.encryptedMasterPassword = "";
        this.salt = null;
    }

    public void importEntries(Map<String, String> encryptedEntries) {
        this.encryptedEntries = (HashMap<String, String>) encryptedEntries;
    }

    public String getEncryptedMasterPassword() {
        return encryptedMasterPassword;
    }

    public void setEncryptedMasterPassword(String encryptedMasterPassword) {
        this.encryptedMasterPassword = encryptedMasterPassword;
    }
}

