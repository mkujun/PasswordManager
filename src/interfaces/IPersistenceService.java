package interfaces;

import model.PasswordEntry;
import persistence.PersistenceService;

import java.util.HashMap;

public interface IPersistenceService {
    void save(byte[] salt, String encryptedMasterPassword, HashMap<String, PasswordEntry> entries);
    PersistenceService.LoadedData load();

    class LoadedData {
        public final byte[] salt;
        public final String encryptedMasterPassword;
        public final HashMap<String, PasswordEntry> entries;

        public LoadedData(byte[] salt, String encryptedMasterPassword, HashMap<String,PasswordEntry> entries) {
            this.salt = salt;
            this.encryptedMasterPassword = encryptedMasterPassword;
            this.entries = entries;
        }
    }
}
