# 🔐 Password Manager (Java Console App)

A lightweight, console-based password manager written in **Java**, designed to store and retrieve passwords securely using **AES encryption**.
Instead of saving an encryption key in a file, this app uses **PBKDF2 (Password-Based Key Derivation Function 2)** to derive the AES key from the master password — providing stronger security and improved portability.

---

## 💡 Features

* 🔒 Secure encryption with **AES** (Advanced Encryption Standard)
* 🧠 Key derived via **PBKDF2WithHmacSHA256** from master password
* 🧂 Uses randomly generated **salt** for key uniqueness
* 📁 Stores encrypted passwords in a local file (`passwords.dat`)
* 🧱 Layered architecture
  - Dependency inversion using interfaces
  - Separation of concerns
  - IPersistenceService → file I/O
  - IPasswordRepository → data management
  - ICryptoService → encryption/decryption
  - IPasswordManager → user-facing CLI logic
* 🖥️ Simple interactive **command-line interface**
* 🧹 Add, view, search, update, and remove password entries
* ✅ No need to save any key files!
* ✅ Fully unit tested with JUnit 4

---

## 🚀 Getting Started

### 🧩 Folder Structure

```
project-root/
 ├── src/
 │   ├── interfaces/
 │      ├── ICryptoService.java
 │      ├── IPersistenceService.java
 │      ├── IPasswordRepository.java
 │      └── IPasswordManager.java
 │   ├── crypto/
 │      └── CryptoService.java
 │   ├── persistence/
 │      └── PersistenceService.java
 │   ├── repository/
 │      └── PasswordRepository.java
 │   ├── manager/
 │      └── PasswordManager.java
 │   ├── model/
 │      └── PasswordEntry.java
 │   └── Main.java
 ├── test/
 │   ├── PasswordManagerTest.java
 │   ├── CryptoServiceTest.java
 │   ├── PasswordRepositoryTest.java
 │   ├── PasswordServiceTest.java
 │   └── PasswordEntryTest.java
 ├── lib/
 │   ├── junit-4.13.2.jar
 │   └── hamcrest-core-1.3.jar
 ├── out/
 └── README.md
```

---

### ⚙️ Compile & Run (Main App)

From the project root directory:

```bash
# Compile
javac src/*.java -d out

# Run
java -cp out manager.PasswordManager
```

---

## 🧪 Running Tests

This project uses **JUnit 4** for testing, included in the `lib/` folder.

### Compile tests

```bash
javac -cp ".;lib/*" src/*.java test/*.java -d out
```

### Run tests

```bash
java -cp ".;lib/*;out" org.junit.runner.JUnitCore PasswordEntryTest
```

> 💡 On macOS/Linux, replace `;` with `:` in the classpath.

Example:

```bash
javac -cp ".:lib/*" src/*.java test/*.java -d out
java -cp ".;lib/*;out" org.junit.runner.JUnitCore ^
  PersistenceServiceTest ^
  PasswordRepositoryTest ^
  CryptoServiceTest ^
  PasswordManagerTest
```

If all tests pass, you’ll see:

```
JUnit version 4.13.2
....
Time: 0.002

OK (4 tests)
```

---

## 🧰 Notes

* The `lib/` folder contains the necessary JUnit dependencies.
* You can add more tests under the `test/` folder to validate encryption logic, file I/O, or edge cases.
* IntelliJ users should **mark `src/` as Sources Root** and **`test/` as Test Sources Root** for the IDE to compile and run tests automatically.

---

## 🧾 License

This project is open-source and free to use for educational and personal projects.
