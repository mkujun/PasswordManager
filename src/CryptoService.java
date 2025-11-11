import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.Scanner;

public class CryptoService {
    private static final int SALT_LENGTH = 16;
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 128;

    private final PasswordRepository repository;
    private SecretKey secretKey;

    public CryptoService(PasswordRepository repository) {
        this.repository = repository;
    }

    public boolean authenticate() {
        Scanner scanner = new Scanner(System.in);
        byte[] salt = repository.getSalt();

        for (int i = 0; i < 3; i++) {
            System.out.print("Enter Master Password: ");
            String enteredPassword = scanner.nextLine();

            secretKey = deriveKey(enteredPassword, salt);
            String encryptedEnteredPassword = encrypt(enteredPassword);

            if (encryptedEnteredPassword.equals(repository.getEncryptedMasterPassword())) {
                System.out.println("Access Granted.");
                return true;
            }

            System.out.println("Incorrect Password. Access Denied.");
        }

        System.out.println("Too many failed attempts. Exiting.");
        return false;
    }

    public void setMasterPassword(String masterPassword) {
        byte[] salt = generateSalt();
        repository.setSalt(salt);
        secretKey = deriveKey(masterPassword, salt);
        String encryptedMasterPassword = encrypt(masterPassword);
        repository.setEncryptedMasterPassword(encryptedMasterPassword);
        repository.save();
    }

    private SecretKey deriveKey(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }

    private byte[] generateSalt() {
        byte[] s = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(s);
        return s;
    }

    public String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plain.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }

    // CRUD delegations
    public void addPassword(String accountName, String username, String password) {
        String encrypted = encrypt(password);
        if (repository.add(new PasswordEntry(accountName, username, encrypted))) {
            System.out.println("Password added successfully.");
        } else {
            System.out.println("Account with that name already exists!");
        }
    }

    public boolean removePassword(String accountName) {
        return repository.remove(accountName);
    }

    public void updateEntry(String accountName, String username, String password) {
        boolean updated = repository.update(accountName, username, encrypt(password));
        System.out.println(updated ? "Entry updated successfully." : "Entry not found.");
    }

    public Optional<PasswordEntry> findEntryByAccountName(String accountName) {
        return repository.findByAccountName(accountName);
    }

    public java.util.List<PasswordEntry> getAllEntries() {
        return repository.getAll();
    }

    public void saveAll() {
        repository.save();
    }
}
