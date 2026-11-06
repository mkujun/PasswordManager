import crypto.CryptoService;
import persistence.PersistenceService;
import repository.PasswordRepository;
import manager.PasswordManager;
import interfaces.*;
import tui.Lanterna;

public class Main {
    private static final String FILE_NAME = "passwords.dat";

    public static void main(String[] args) {
        ICryptoService cryptoService = new CryptoService();
        IPersistenceService persistenceService = new PersistenceService(FILE_NAME);
        IPasswordRepository repository = new PasswordRepository(persistenceService);
        IPasswordManager passwordManager = new PasswordManager(cryptoService, repository);

        try {
            System.out.println("Trying to run Lanterna");
            new Lanterna().run();
        } catch (Exception e) {
            System.out.println("Error running Lanterna");
            System.out.println("Running in plain console mode");
            passwordManager.start();
        }
    }
}


