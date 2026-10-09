package persistence;

import interfaces.IPersistenceService;

import java.io.*;
import java.util.HashMap;

public class PersistenceService implements IPersistenceService {

    private final String fileName;

    public PersistenceService(String fileName) {
        this.fileName = fileName;
    }

    public void save(byte[] salt, String encryptedMasterPassword, HashMap<String, String> encryptedEntries) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fileName))) {
            oos.writeObject(salt);
            oos.writeObject(encryptedMasterPassword);
            oos.writeObject(encryptedEntries);
        } catch (IOException e) {
            System.err.println("Error saving data: " + e.getMessage());
        }
    }

    public LoadedData load() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fileName))) {
            byte[] salt = (byte[]) ois.readObject();
            String encryptedMasterPassword = (String) ois.readObject();
            HashMap<String, String> encryptedEntries = (HashMap<String, String>) ois.readObject();

            return new LoadedData(salt, encryptedMasterPassword, encryptedEntries);
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No existing data found. Starting fresh.");
            return null;
        }
    }
}

