import java.io.*;
import java.util.HashMap;

public class PersistenceService {

    private final String fileName;

    public PersistenceService(String fileName) {
        this.fileName = fileName;
    }

    public void save(byte[] salt, String encryptedMasterPassword, HashMap<String, PasswordEntry> entries) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(salt);
            oos.writeObject(encryptedMasterPassword);
            oos.writeObject(entries);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public LoadedData load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            byte[] salt = (byte[]) ois.readObject();
            String encryptedMasterPassword = (String) ois.readObject();
            HashMap<String, PasswordEntry> entries = (HashMap<String, PasswordEntry>) ois.readObject();

            return new LoadedData(salt, encryptedMasterPassword, entries);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing data found. Starting fresh.");
            return null;
        }
    }

    public static class LoadedData {
        public final byte[] salt;
        public final String encryptedMasterPassword;
        public final HashMap<String, PasswordEntry> entries;

        public LoadedData(byte[] salt, String encryptedMasterPassword, HashMap<String,PasswordEntry> entries) {
            this.salt = salt;
            this.encryptedMasterPassword = encryptedMasterPassword;
            this.entries = entries;
        }
    }
}

