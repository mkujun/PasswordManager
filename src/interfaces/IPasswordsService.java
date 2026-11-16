package interfaces;

import model.PasswordEntry;

import java.util.Map;

public interface IPasswordsService {
    Map<String, PasswordEntry> viewPasswords(String account);
    boolean login(String masterPassword);
    boolean masterPasswordExists();
    void setMasterPassword(String masterPassword);
    boolean accountExists(String account);
    void addPasswordEntry(PasswordEntry entry);
    void deletePasswordEntry(String account);
    PasswordEntry getPasswordEntry(String account);
    void updatePasswordEntry(String account, PasswordEntry entry);
    void deleteAllEntries();
}
