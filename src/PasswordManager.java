import javax.crypto.*;
import javax.crypto.spec.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.Base64;

public class PasswordManager {
    private static final String FILE_NAME = "passwords.dat";
    private static final String INIT_MESSAGE = "No master password found. Setting up a new master password.";
    private static final int SALT_LENGTH = 16;

    private static SecretKey secretKey;
    private byte[] salt;
    private String encryptedMasterPassword;
    private List<PasswordEntry> entries = new ArrayList<>();

    public static void main(String[] args) {
        PasswordManager manager = new PasswordManager();
        if (manager.initialize()) {
            manager.run();
            manager.saveEntries();
        }
    }

    private boolean initialize() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            System.out.println(INIT_MESSAGE);
            setMasterPassword();
        } else {
            loadEntries();
            if (!authenticate()) {
                return false;
            }
        }
        return true;
    }

    private void setMasterPassword() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Set your Master Password: ");
        String masterPassword = scanner.nextLine();

        salt = generateSalt();
        secretKey = deriveKey(masterPassword, salt);
        encryptedMasterPassword = encrypt(masterPassword);

        saveEntries();
    }

    private boolean authenticate() {
        Scanner scanner = new Scanner(System.in);

        for (int attempts = 0; attempts < 3; attempts++) {
            System.out.print("Enter Master Password: ");
            String enteredPassword = scanner.nextLine();

            secretKey = deriveKey(enteredPassword, salt);
            String encryptedEnteredPassword = encrypt(enteredPassword);

            if (encryptedMasterPassword.equals(encryptedEnteredPassword)) {
                System.out.println("Access Granted.");
                return true;
            }

            System.out.println("Incorrect Password. Access Denied.");
        }

        System.out.println("Too many failed attempts. Exiting.");
        return false;
    }


    private void run() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("\nPassword Manager");
            System.out.println("1. Add Password");
            System.out.println("2. View Passwords");
            System.out.println("3. Remove Password");
            System.out.println("4. Search Account");
            System.out.println("5. Update Entry");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    addPassword(scanner);
                    break;
                case 2:
                    viewPasswords();
                    break;
                case 3:
                    removePassword(scanner);
                    break;
                case 4:
                    searchPasswordsByAccountName(scanner);
                    break;
                case 5:
                    updateEntry(scanner);
                    break;
                case 6:
                    return;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    private void addPassword(Scanner scanner) {
        System.out.print("Enter Account Name: ");
        String accountName = scanner.nextLine();
        System.out.print("Enter Username: ");
        String username = scanner.nextLine();
        System.out.print("Enter Password: ");
        String password = scanner.nextLine();

        if (entryExists(entries, accountName)) {
            System.out.println("Account with that name already exists!");
        }
        else {
            String encryptedPassword = encrypt(password);
            entries.add(new PasswordEntry(accountName, username, encryptedPassword));

            System.out.println("Password added successfully.");
        }
    }

    private boolean entryExists(List<PasswordEntry> entries, String newEntryAccountName) {
        for (PasswordEntry entry : entries) {
            if (entry.getAccountName().equals(newEntryAccountName)) {
                return true;
            }
        }

        return false;
    }

    private void printEntry(PasswordEntry entry) {
        String decrypted = decrypt(entry.getEncryptedPassword());
        System.out.println("Account: " + entry.getAccountName());
        System.out.println("Username: " + entry.getUsername());
        System.out.println("Password: " + decrypted);
        System.out.println();
    }

    private void viewPasswords() {
        for (PasswordEntry entry : entries) {
            printEntry(entry);
        }
    }

    private void removePassword(Scanner scanner) {
        System.out.print("Enter Account Name to remove: ");
        String accountName = scanner.nextLine();
        entries.removeIf(e -> e.getAccountName().equalsIgnoreCase(accountName));
        System.out.println("Password removed successfully.");
    }

    private void updateEntry(Scanner scanner) {
        System.out.print("Enter Account Name to edit: ");
        String accountName = scanner.nextLine();
        Optional<PasswordEntry> result = findEntryByAccountName(entries, accountName);

        if (result.isPresent()) {
            System.out.print("Enter New Username: ");
            String username = scanner.nextLine();
            System.out.print("Enter New Password: ");
            String password = scanner.nextLine();

            entries.stream().filter(p -> p.getAccountName().equals(accountName))
                    .findFirst()
                    .ifPresent(p ->  {
                                p.setUsername(username);
                                p.setEncryptedPassword(encrypt(password));
                            }
                    );

            System.out.println("Entry updated successfully.");
        }
        else {
            System.out.println("Entry not found.");
        }
    }

    private void searchPasswordsByAccountName(Scanner scanner) {
        System.out.print("Enter Account Name: ");
        String accountName = scanner.nextLine();

        Optional<PasswordEntry> result = findEntryByAccountName(entries, accountName);

        if (result.isPresent()) {
            printEntry(result.get());
        }
        else {
            System.out.println("Entry not found.");
        }
    }

    private Optional<PasswordEntry> findEntryByAccountName(List<PasswordEntry> entries, String accountName) {
        return entries.stream()
                .filter(p -> p.getAccountName().equalsIgnoreCase(accountName))
                .findFirst();
    }

    private String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encrypted = cipher.doFinal(plain.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption error", e);
        }
    }

    private String decrypt(String encoded) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decoded = Base64.getDecoder().decode(encoded);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Decryption error", e);
        }
    }

    private void loadEntries() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            salt = (byte[]) ois.readObject();
            encryptedMasterPassword = (String) ois.readObject();
            entries = (List<PasswordEntry>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Failed to load saved data. Starting fresh.");
        }
    }

    private void saveEntries() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(salt);
            oos.writeObject(encryptedMasterPassword);
            oos.writeObject(entries);
        } catch (IOException e) {
            System.out.println("Error saving data: " + e.getMessage());
        }
    }

    private byte[] generateSalt() {
        byte[] s = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(s);
        return s;
    }

    private SecretKey deriveKey(String password, byte[] salt) {
        try {
            int iterations = 65536;
            int keyLen = 128;
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, keyLen);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Key derivation failed", e);
        }
    }
}
