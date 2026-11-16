package service;

import interfaces.IPasswordsService;
import interfaces.ICryptoService;
import interfaces.IPasswordRepository;
import model.PasswordEntry;

import javax.crypto.SecretKey;
import java.util.HashMap;
import java.util.Map;

public class PasswordsService implements IPasswordsService {
    private final ICryptoService crypto;
    private final IPasswordRepository repository;
    private SecretKey secretKey;

    public PasswordsService(ICryptoService crypto, IPasswordRepository repository) {
        this.crypto = crypto;
        this.repository = repository;
    }

    @Override
    public Map<String, PasswordEntry> viewPasswords(String account) {
        Map<String, PasswordEntry> decryptedEntries = new HashMap<>();
        Map<String, String> entries = repository.getEntries();

        entries.forEach((key,value) -> {
            PasswordEntry entry = crypto.decryptEntry(value, secretKey);
            if (entry.getAccountName().contains(account)) {
                decryptedEntries.put(key, entry);
            }
        });
        return decryptedEntries;
    }

    @Override
    public boolean login(String enteredPassword) {
        SecretKey candidate = crypto.deriveKey(enteredPassword, repository.getSalt());
        boolean ok = crypto.encrypt(enteredPassword, candidate)
                .equals(repository.getEncryptedMasterPassword());
        if (ok) {
            this.secretKey = candidate;
        }
        return ok;
    }

    @Override
    public boolean masterPasswordExists() {
        return repository.getEncryptedMasterPassword() != null;
    }

    @Override
    public void setMasterPassword(String masterPassword) {
        byte[]salt = crypto.generateSalt();
        repository.setSalt(salt);

        secretKey = crypto.deriveKey(masterPassword, salt);
        repository.setEncryptedMasterPassword(crypto.encrypt(masterPassword, secretKey));

        repository.save();
    }

    @Override
    public boolean accountExists(String account) {
        return repository.accountExists(account);
    }

    @Override
    public void addPasswordEntry(PasswordEntry entry) {
        String encryptedNewEntry = crypto.encryptEntry(entry, secretKey);
        repository.addEncryptedEntry(encryptedNewEntry, entry.getAccountName());
        repository.save();
    }

    @Override
    public void deletePasswordEntry(String account) {
        repository.remove(account);
        repository.save();
    }

    @Override
    public PasswordEntry getPasswordEntry(String account) {
        return crypto.decryptEntry(repository.getEntry(account), secretKey);
    }

    @Override
    public void updatePasswordEntry(String account, PasswordEntry entry) {
        String encryptedEntry = crypto.encryptEntry(entry, secretKey);
        repository.updateEntry(account, encryptedEntry);
        repository.save();
    }

    @Override
    public void deleteAllEntries() {
        repository.dump();
    }
}
