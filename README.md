# 🔐 Password Manager (Java TUI App)

A lightweight password manager written in **Java**, designed to store and retrieve
passwords securely using **AES encryption**.
Instead of saving an encryption key in a file, this app uses
**PBKDF2 (Password-Based Key Derivation Function 2)** to derive the AES key from the
master password — providing stronger security and improved portability.

The app runs as a **text-based user interface (TUI)** built with
[Lanterna](https://github.com/mabe02/lanterna). If the terminal does not support the
TUI, the app automatically **falls back to a plain console interface**.

---

## 💡 Features

* 🔒 Secure encryption with **AES** (Advanced Encryption Standard)
* 🧠 Key derived via **PBKDF2WithHmacSHA256** from the master password
* 🧂 Uses a randomly generated **salt** for key uniqueness
* 📁 Stores encrypted passwords in a local file (`passwords.dat`)
* 🖥️ **TUI by default** (Lanterna), with an automatic **plain console fallback**
* 🧱 Layered architecture
  - Dependency inversion using interfaces
  - Separation of concerns
  - `IPersistenceService` → file I/O
  - `IPasswordRepository` → data management
  - `ICryptoService` → encryption/decryption
  - `IPasswordManager` → console-mode CLI logic
  - `tui/*` → Lanterna screens (TUI mode)
* 🧹 Add, view, search, update, and remove password entries
* ✅ No need to save any key files!
* ✅ Unit tested with **JUnit 4** and **Mockito**

---

## 🖥️ Interface Modes

The entry point is `Main`, which selects the interface at startup:

1. **TUI (default)** — `Main` tries to launch the Lanterna UI (`tui.Lanterna`).
2. **Plain console (fallback)** — if the TUI cannot start (e.g. the terminal does not
   support it), `Main` falls back to the console flow driven by
   `manager.PasswordManager`.

> ℹ️ The master password must already be set before the TUI can be used. If no master
> password exists yet, run the app in console mode once to create it.

---

## 🚀 Getting Started

### 🧩 Folder Structure

```
project-root/
 ├── src/
 │   ├── interfaces/
 │   │   ├── ICryptoService.java
 │   │   ├── IPersistenceService.java
 │   │   ├── IPasswordRepository.java
 │   │   └── IPasswordManager.java
 │   ├── crypto/
 │   │   └── CryptoService.java
 │   ├── persistence/
 │   │   └── PersistenceService.java
 │   ├── repository/
 │   │   └── PasswordRepository.java
 │   ├── manager/
 │   │   └── PasswordManager.java        # console-mode orchestration
 │   ├── model/
 │   │   └── PasswordEntry.java
 │   ├── tui/                            # Lanterna TUI (default mode)
 │   │   ├── Lanterna.java               # TUI entry / flow controller
 │   │   ├── Login.java
 │   │   ├── MainMenu.java
 │   │   ├── PasswordList.java
 │   │   ├── AddPassword.java
 │   │   ├── AppScreen.java              # base screen (shared window/exit handling)
 │   │   └── Theme.java
 │   └── Main.java                       # app entry point (TUI, console fallback)
 ├── test/
 │   ├── PasswordManagerTest.java
 │   ├── CryptoServiceTest.java
 │   ├── PasswordRepositoryTest.java
 │   ├── PersistenceServiceTest.java
 │   └── PasswordEntryTest.java
 ├── lib/
 │   ├── lanterna-3.1.5.jar
 │   ├── junit-4.13.1.jar
 │   ├── hamcrest-core-1.3.jar
 │   ├── mockito-core-3.12.4.jar
 │   ├── byte-buddy-1.10.22.jar
 │   ├── byte-buddy-agent-1.10.22.jar
 │   └── objenesis-3.2.jar
 ├── out/        # compiled classes (git-ignored)
 ├── wslBuild/   # compiled classes for WSL builds (git-ignored)
 └── README.md
```

---

### ⚙️ Compile & Run (Main App)

From the project root directory. The Lanterna jar must be on the classpath, and **all**
source packages (including `tui/` and the sub-packages such as `crypto/`, `model/`,
`repository/`, ...) must be compiled. A plain `src/*.java` glob only matches `Main.java`,
so compile the whole source tree.

**Windows (PowerShell)** — classpath separator is `;`:

```powershell
# Compile (collect every .java under src/ so sub-packages are included)
if (!(Test-Path out)) { New-Item -ItemType Directory out | Out-Null }
$src = Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName }
javac -cp ".;lib/*" -d out $src

# Run (entry point is the Main class)
java -cp ".;lib/*;out" Main
```

**macOS/Linux** — classpath separator is `:`:

```bash
# Compile (find every .java under src/ so sub-packages are included)
mkdir -p out
javac -cp ".:lib/*" -d out $(find src -name "*.java")

# Run (entry point is the Main class)
java -cp ".:lib/*:out" Main
```

`Main` launches the Lanterna TUI by default and falls back to the plain console
interface if the TUI cannot start.

---

## 🧪 Running Tests

This project uses **JUnit 4** and **Mockito**, both included in the `lib/` folder.

### Compile tests

```powershell
# Windows (PowerShell) — compile all sources plus the tests
if (!(Test-Path out)) { New-Item -ItemType Directory out | Out-Null }
$files = Get-ChildItem -Recurse -Filter *.java src, test | ForEach-Object { $_.FullName }
javac -cp ".;lib/*" -d out $files
```

```bash
# macOS/Linux
mkdir -p out
javac -cp ".:lib/*" -d out $(find src test -name "*.java")
```

### Run tests

```bash
# Windows
java -cp ".;lib/*;out" org.junit.runner.JUnitCore ^
  PasswordEntryTest ^
  PersistenceServiceTest ^
  PasswordRepositoryTest ^
  CryptoServiceTest ^
  PasswordManagerTest
```

> 💡 On macOS/Linux, replace `;` with `:` in the classpath and `^` with `\` for line
> continuation.

If all tests pass, you’ll see something like:

```
JUnit version 4.13.1
........
Time: 2.8xx

OK (42 tests)
```

---

## 🧰 Notes

* The `lib/` folder contains all runtime and test dependencies (Lanterna, JUnit,
  Mockito and its transitive jars).
* `out/` and `wslBuild/` hold compiled classes and are git-ignored.
* You can add more tests under the `test/` folder to validate encryption logic,
  file I/O, or edge cases.
* IntelliJ users should **mark `src/` as Sources Root** and **`test/` as Test Sources
  Root**, and add the `lib/` jars as module dependencies.

---

## 🧾 License

This project is open-source and free to use for educational and personal projects.
