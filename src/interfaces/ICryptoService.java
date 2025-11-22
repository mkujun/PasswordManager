package interfaces;

import javax.crypto.SecretKey;

public interface ICryptoService {
    SecretKey deriveKey(String password, byte[] salt);
    byte[] generateSalt();
    String encrypt(String plain, SecretKey secretKey);
    String decrypt(String encoded, SecretKey secretKey);
}
