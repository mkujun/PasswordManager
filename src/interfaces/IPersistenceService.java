package interfaces;

import persistence.PersistenceService;

import java.util.HashMap;

public interface IPersistenceService {
    void save(byte[] salt, String encryptedMasterPassword, HashMap<String, String> encryptedEntries);
    PersistenceService.LoadedData load();

    class LoadedData {
        public final byte[] salt;
        public final String encryptedMasterPassword;
        public final HashMap<String, String> encryptedEntries;

        public LoadedData(byte[] salt, String encryptedMasterPassword, HashMap<String,String> encryptedEntries) {
            this.salt = salt;
            this.encryptedMasterPassword = encryptedMasterPassword;
            this.encryptedEntries = encryptedEntries;
        }
    }
}
