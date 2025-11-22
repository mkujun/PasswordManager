import crypto.CryptoService;
import persistence.PersistenceService;
import repository.PasswordRepository;
import manager.PasswordManager;
import interfaces.*;

public class Main {
    private static final String FILE_NAME = "passwords.dat";

    public static void main(String[] args) {
        ICryptoService cryptoService = new CryptoService();
        IPersistenceService persistenceService = new PersistenceService(FILE_NAME);
        IPasswordRepository repository = new PasswordRepository(persistenceService);
        IPasswordManager passwordManager = new PasswordManager(cryptoService, repository);

        passwordManager.start();
    }
}


