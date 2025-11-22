package interfaces;

import java.util.Scanner;

public interface IPasswordManager {
    void run();
    void start();
    boolean initialize();
    boolean authenticate();
    void setMasterPassword();
    void removePassword(Scanner scanner);
    void addPassword(Scanner scanner);
    void viewPasswords();
    void searchPassword(Scanner scanner);
    void updateEntry(Scanner scanner);
}
