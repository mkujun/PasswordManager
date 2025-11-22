package persistence;

import interfaces.IPersistenceService;
import model.PasswordEntry;

import java.io.*;
import java.util.HashMap;

public class PersistenceService implements IPersistenceService {

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
}

