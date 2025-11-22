package interfaces;

import model.PasswordEntry;

import java.util.Map;

public interface IPasswordRepository {
    boolean add(PasswordEntry passwordEntry);
    boolean remove(String accountName);
    boolean update(String accountName, String username, String encryptedPassword);
    PasswordEntry find(String accountName);
    void save();
    byte[] getSalt();
    void setSalt(byte[] salt);
    String getEncryptedMasterPassword();
    void setEncryptedMasterPassword(String encryptedMasterPassword);
    Map<String, PasswordEntry> getEntries();
}
