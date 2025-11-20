import javax.crypto.SecretKey;
import java.io.File;
import java.util.Map;
import java.util.Scanner;

public class PasswordManager {
    private static final String FILE_NAME = "passwords.dat";
    private static final String INIT_MESSAGE = "No master password found. Setting up a new master password.";

    private CryptoService crypto;
    private PasswordRepository repository;

    public SecretKey secretKey;

    public static void main(String[] args) {
        new PasswordManager().start();
    }

    public void start() {
        PersistenceService persistence = new PersistenceService(FILE_NAME);
        repository = new PasswordRepository(persistence);
        crypto = new CryptoService();

        if (!initialize(repository)) return;

        run();
        repository.save();
    }

    private boolean initialize(PasswordRepository repository) {
        File file = new File(FILE_NAME);

        if (!file.exists() || repository.getEncryptedMasterPassword() == null) {
            System.out.println(INIT_MESSAGE);
            setMasterPassword();
        } else if (!authenticate()) {
            return false;
        }

        return true;
    }

    private boolean authenticate() {
        Scanner scanner = new Scanner(System.in);
        for (int i = 0; i < 3; i++) {
            String enteredPassword = prompt(scanner, "Enter Master Password: ");
            secretKey = crypto.deriveKey(enteredPassword, repository.getSalt());

            String encryptedEnteredPassword = crypto.encrypt(enteredPassword, secretKey);

            if (encryptedEnteredPassword.equals(repository.getEncryptedMasterPassword())) {
                System.out.println("Access Granted.");
                return true;
            }

            System.out.println("Incorrect Password. Access Denied.");
        }
        System.out.println("Too many failed attempts. Exiting.");

        return false;
    }

    private void setMasterPassword() {
        String masterPassword = prompt(new Scanner(System.in), "Set your Master Password: ");

        byte[]salt = crypto.generateSalt();
        repository.setSalt(salt);

        secretKey = crypto.deriveKey(masterPassword, salt);
        repository.setEncryptedMasterPassword(crypto.encrypt(masterPassword, secretKey));

        repository.save();
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
                case 1: addPassword(scanner);
                    break;
                case 2: viewPasswords();
                    break;
                case 3: removePassword(scanner);
                    break;
                case 4: searchPasswords(scanner);
                    break;
                case 5: updateEntry(scanner);
                    break;
                case 6: { return; }
                default : System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void addPassword(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name: ");

        if (repository.find(account) != null) {
            System.out.println("Account with that name already exists!");
        }
        else {
            String user = prompt(scanner, "Enter Username: ");
            String pass = prompt(scanner, "Enter Password: ");
            String encrypted = crypto.encrypt(pass, secretKey);
            repository.add(new PasswordEntry(account, user, encrypted));
        }
    }

    private void removePassword(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name to remove: ");
        System.out.println(repository.remove(account) ? "Password removed successfully." : "Account not found.");
    }

    private void viewPasswords() {
        Map<String, PasswordEntry> entries = repository.getEntries();
        entries.values().forEach(this::printEntry);
    }

    private void searchPasswords(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name: ");

        PasswordEntry entry = repository.find(account);

        if (entry != null) {
            printEntry(entry);
        }
        else {
            System.out.println("Entry not found.");
        }
    }

    private void updateEntry(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name: ");

        if (repository.find(account) == null) {
            System.out.println("Account with that name does not exists!");
        }
        else {
            String user = prompt(scanner, "Enter New Username: ");
            String pass = prompt(scanner, "Enter New Password: ");
            String encrypted = crypto.encrypt(pass, secretKey);
            if(repository.update(account, user, encrypted)) {
                System.out.println("Account updated!");
            }
        }
    }

    private String prompt(Scanner scanner, String msg) {
        System.out.print(msg);
        return scanner.nextLine().trim();
    }

    private void printEntry(PasswordEntry entry) {
        String decrypted = crypto.decrypt(entry.getEncryptedPassword(), secretKey);
        System.out.println("\nAccount: " + entry.getAccountName());
        System.out.println("Username: " + entry.getUsername());
        System.out.println("Password: " + decrypted);
    }
}
