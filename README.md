A lightweight, console-based password manager written in **Java**, designed to store and retrieve passwords securely using **AES encryption**. Instead of saving an encryption key in a file, this app uses **PBKDF2 (Password-Based Key Derivation Function 2)** to derive the AES key from the master password, providing stronger security and improved portability.

---

## 💡 Features

- 🔒 Secure encryption with **AES** (Advanced Encryption Standard)
- 🧠 Key derived via **PBKDF2WithHmacSHA256** from master password
- 🧂 Uses randomly generated **salt** for key uniqueness
- 📁 Stores encrypted passwords in a local file (`passwords.dat`)
- 🖥️ Simple interactive **command-line interface**
- 🧹 Add, view, search, and remove password entries
- ✅ No need to save any key files!

---

## 🚀 Getting Started

### Compile & Run

```bash
javac PasswordManager.java
java PasswordManager