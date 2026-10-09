package interfaces;

import model.PasswordEntry;

import javax.crypto.SecretKey;

public interface ICryptoService {
    SecretKey deriveKey(String password, byte[] salt);
    byte[] generateSalt();
    String encrypt(String plain, SecretKey secretKey);
    String encryptEntry(PasswordEntry entry, SecretKey secretKey);
    PasswordEntry decryptEntry(String encryptedEntry, SecretKey secretKey);
}
