import interfaces.IPasswordsService;
import model.PasswordEntry;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Scanner;

public class Konzola {
    private final IPasswordsService passwordsService;

    public Konzola(IPasswordsService passwordsService) {
        this.passwordsService = passwordsService;
    }

    public void auth() {
        Scanner scanner = new Scanner(System.in);

        if (!passwordsService.masterPasswordExists()) {
            boolean masterPasswordCreated = createMasterPassword(scanner);
            if (masterPasswordCreated) {
                start(scanner);
            }
        }
        else {
            login(scanner);
        }
    }

    private boolean createMasterPassword(Scanner scanner) {
        while (true) {
            String answer = prompt(scanner, "Do you want to create account? Type 'yes' or 'no': ");
            if (answer.equals("yes")) {
                return signup(scanner);
            }
            else if (answer.equals("no")) {
                return false;
            }
            else {
                System.out.println("Type yes or no");
            }
        }
    }

    private void login(Scanner scanner) {
        while(true) {
            String enteredPassword = prompt(scanner, "Enter Master Password: ");
            boolean loggedIn = passwordsService.login(enteredPassword);

            if (loggedIn) {
                start(scanner);
                return;
            }
            else {
                System.out.println("Incorrect Password. Access Denied.");
            }
        }
    }

    private boolean signup(Scanner scanner) {
        while (true) {
            String masterPassword = prompt(scanner, "Set your Master Password: ");

            if (masterPassword.isEmpty()) {
                System.out.println("Password should not be empty!");
                continue;
            }

            String repeatPassword = prompt(scanner, "Repeat your Master Password: ");
            if (!masterPassword.equals(repeatPassword)) {
                System.out.println("Passwords don't match! Enter master password again.");
                continue;
            }

            passwordsService.setMasterPassword(masterPassword);
            break;
        }
        return true;
    }

    private void start(Scanner scanner) {
        System.out.println("Access Granted.");
        while (true) {
            System.out.println("\nKonzola");
            System.out.println("1. Add Password");
            System.out.println("2. View Passwords");
            System.out.println("3. Remove Password");
            System.out.println("4. Search Account");
            System.out.println("5. Update Entry");
            System.out.println("6. Update master password");
            System.out.println("7. Exit");
            System.out.print("Choose an option: ");

            try {
                int choice = Integer.parseInt(scanner.nextLine());
                switch (choice) {
                    case 1: addPassword(scanner);
                        break;
                    case 2: viewPasswords();
                        break;
                    case 3: removePassword(scanner);
                        break;
                    case 4: searchPassword(scanner);
                        break;
                    case 5: updateEntry(scanner);
                        break;
                    case 6: updateMasterPassword(scanner);
                        break;
                    case 7: {
                        return;
                    }
                    default : System.out.println("Invalid option. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("You did not enter a number. Try again.");
            }
        }
    }

    private void updateMasterPassword(Scanner scanner) {
        // backup
        Map<String, PasswordEntry> decryptedEntries = passwordsService.viewPasswords("");

        // delete everything
        passwordsService.deleteAllEntries();

        // create new master password and salt
        signup(scanner);

        // import entries with new master password
        decryptedEntries.forEach((key, entry) -> {
            passwordsService.addPasswordEntry(entry);
        });

        System.out.println("Master Password has been updated.");
    }

    private void searchPassword(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name: ");
        Map<String, PasswordEntry> entries = passwordsService.viewPasswords(account);

        if (entries.isEmpty()) {
            System.out.println("Entry not found.");
        }
        else {
            entries.forEach((key, value) -> {
                printEntry(value);
            });
        }
    }

    private void removePassword(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name to remove: ");

        if (!passwordsService.accountExists(account)) {
            System.out.println("Account does not exist. Try again.");
            return;
        }

        passwordsService.deletePasswordEntry(account);

        if (!passwordsService.accountExists(account)) {
            System.out.println("Password deleted successfully");
        }
    }

    private void addPassword(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name: ");

        if (account.isEmpty()) {
            System.out.println("Account name is empty. Try again.");
            return;
        }

        boolean accountExists = passwordsService.accountExists(account);

        if (accountExists) {
            System.out.println("Account with that name already exists!");
            return;
        }

        String user = prompt(scanner, "Enter Username: ");
        String pass = prompt(scanner, "Enter Password: ");
        String notes = prompt(scanner, "Enter Notes: ");
        String url = prompt(scanner, "Enter Url: ");

        if (!isInputValid(user, pass, account)) {
            System.out.println("Username and password fields are required!");
        }
        else {
            PasswordEntry entry = new PasswordEntry.Builder(account)
                    .username(user)
                    .password(pass)
                    .notes(notes)
                    .url(url)
                    .build();

            passwordsService.addPasswordEntry(entry);

            if (passwordsService.accountExists(account)) {
                System.out.println("Password added successfully");
            }
        }
    }

    private void updateEntry(Scanner scanner) {
        String account = prompt(scanner, "Enter Account Name: ");

        if (!passwordsService.accountExists(account)) {
            System.out.println("Account with that name does not exists!");
        }
        else {
            PasswordEntry entryToUpdate = passwordsService.getPasswordEntry(account);

            String user = prompt(scanner, "Enter New Username: ");
            String pass = prompt(scanner, "Enter New Password: ");
            String notes = prompt(scanner, "Enter New Notes: ");
            String url = prompt(scanner, "Enter New Url: ");

            if (!isInputValid(user, pass, account)) {
                System.out.println("Username and password fields are required!");
                return;
            }

            entryToUpdate.setUsername(user);
            entryToUpdate.setPassword(pass);
            entryToUpdate.setNotes(notes);
            entryToUpdate.setUrl(url);
            entryToUpdate.setUpdatedAt(LocalDateTime.now());

            passwordsService.updatePasswordEntry(account, entryToUpdate);

            if (passwordsService.accountExists(account)) {
                System.out.println("Password updated successfully");
            }
        }
    }

    private boolean isInputValid(String user, String pass, String account) {
        return !user.isEmpty() && !pass.isEmpty() && !account.isEmpty();
    }

    private void viewPasswords() {
        Map<String, PasswordEntry> entries = passwordsService.viewPasswords("");

        entries.forEach((k, v) -> printEntry(v));
    }

    private static void printEntry(PasswordEntry entry) {
        System.out.println("\nAccount: " + entry.getAccountName());
        System.out.println("Username: " + entry.getUsername());
        System.out.println("Password: " + entry.getPassword());
        System.out.println("Notes: " + entry.getNotes());
        System.out.println("Url: " + entry.getUrl());
        System.out.println("Updated at: " + entry.getUpdatedAt());
        System.out.println("Created at: " + entry.getCreatedAt());
        System.out.println("Account active: " + entry.isAccountActive());
    }

    private String prompt(Scanner scanner, String msg) {
        System.out.print(msg);
        return scanner.nextLine().trim();
    }
}
