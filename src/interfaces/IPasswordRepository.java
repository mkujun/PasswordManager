package interfaces;

import model.PasswordEntry;

import java.util.Map;

public interface IPasswordRepository {
    boolean addEncryptedEntry(String encryptedPasswordEntry, String accountName);
    boolean remove(String accountName);
    Map<String, String> find(String accountName);
    void save();
    byte[] getSalt();
    void setSalt(byte[] salt);
    String getEncryptedMasterPassword();
    void setEncryptedMasterPassword(String encryptedMasterPassword);
    Map<String, String> getEntries();
    void dump();
    void importEntries(Map<String, String> entries);
}
