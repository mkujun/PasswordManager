# 🛠️ adminReadme.md — Architecture & Design Notes

This document describes the **architectural decisions, design principles, and testing strategy**
used in the **Java Password Manager** project.

The application runs as a **TUI (Lanterna) by default**, with an automatic **plain
console fallback**. Both front-ends sit on top of the same layered core.

It is intended for **developers and maintainers**, not end users.

---

## 1. High-Level Architecture

The application follows a **layered architecture** with **explicit separation of concerns**.

There are two front-ends on top of a shared service layer. `Main` picks the front-end
at startup: it tries the TUI first and falls back to the console UI if the TUI cannot
start.

```
┌──────────────────────────────┐        ┌──────────────────────────────┐
│   tui.Lanterna (TUI, default) │        │   Konzola (console fallback)  │ ← Front-ends (UI only)
└───────────────▲──────────────┘        └───────────────▲──────────────┘
                │                                        │
                └────────────────┬───────────────────────┘
                                 │  IPasswordsService
                   ┌─────────────┴───────────────┐
                   │   service.PasswordsService   │ ← Business logic
                   │                              │   (login, master password,
                   │                              │   entry CRUD, encryption calls)
                   └─────────────▲───────────────┘
                                 │  ICryptoService, IPasswordRepository
                   ┌─────────────┴───────────────┐
                   │       Implementations        │ ← CryptoService,
                   │                              │   PasswordRepository,
                   │                              │   PersistenceService
                   └─────────────▲───────────────┘
                                 │
                   ┌─────────────┴───────────────┐
                   │         File system          │ ← passwords.dat
                   └──────────────────────────────┘
```

### Core idea
> **High-level logic depends on abstractions, not implementations**

This keeps the system **testable, maintainable, and extensible**.

---

## 2. Entry Point & UI Selection

The single entry point is `Main`. It wires up the core services once, creates one
`PasswordsService`, and passes it to whichever front-end runs:

```java
IPasswordsService passwordsService = new PasswordsService(cryptoService, repository);

try {
    // Default: launch the Lanterna TUI.
    new Lanterna(passwordsService).run();
} catch (Exception e) {
    // Fallback: plain console interface.
    new Konzola(passwordsService).auth();
}
```

### Consequences
- The **TUI is the default** experience; the console mode is a **fallback** for
  terminals that cannot run Lanterna.
- Both front-ends share **the same service instance**, so login, master-password and
  entry logic exist in exactly one place.

---

## 3. Why Use Interfaces

### Interfaces Used
- `ICryptoService`
- `IPasswordRepository`
- `IPersistenceService`
- `IPasswordsService`

### Reasons

#### 1️⃣ Decoupling
The front-ends do **not care**:
- how encryption is implemented
- how data is stored
- where persistence lives

They only call `IPasswordsService` operations.

#### 2️⃣ Testability
Interfaces allow us to:
- mock dependencies (Mockito)
- test components **in isolation**
- avoid file system and crypto during unit tests

Example:
```java
IPasswordRepository repo = mock(IPasswordRepository.class);
ICryptoService crypto = mock(ICryptoService.class);
```

#### 3️⃣ Replaceability

Later it can:
- replace `CryptoService` with a stronger algorithm
- replace file persistence with a database
- add a cloud sync service

➡️ Without touching the front-end logic.

---

## 4. Responsibility Breakdown

### 4.1 Front-ends (UI Layer)

Two interchangeable front-ends. They handle only user interaction and input
validation (e.g. empty fields, password confirmation) and delegate everything else to
`IPasswordsService`.

**tui.Lanterna** — the default TUI and flow controller. Owns a set of screens:
- `Signup` — first-start master-password creation (double entry, show/hide)
- `Login` — master-password entry
- `MainMenu` — navigation hub
- `PasswordList` — view / edit / delete entries
- `AddPassword` — create / edit a single entry
- `AppScreen` — shared base screen (full-screen window, Q/ESC exit handling)
- `Theme` — color palette for the terminal UI

**Konzola** — the console fallback. Same flow (signup / login / menu), driven by text
prompts instead of screens.

What the front-ends do **NOT** do:
- encryption or key handling
- file I/O
- data storage logic

### 4.2 PasswordsService (Business Logic Layer)

#### Responsibilities
- Master password: `masterPasswordExists`, `setMasterPassword`, `login`
- Holds the derived `SecretKey` for the session (set only after a successful login
  or signup)
- Entry operations: `viewPasswords` (with account-name filter), `getPasswordEntry`,
  `accountExists`, `addPasswordEntry`, `updatePasswordEntry`,
  `deletePasswordEntry`, `deleteAllEntries`
- Encrypts entries before handing them to the repository and decrypts them on read
- Saves the repository after every change

### 4.3 CryptoService (Security Layer)

#### Responsibilities
- Key derivation using PBKDF2
- Salt generation
- AES encryption / decryption (including whole-entry encrypt/decrypt)

#### Why it is isolated
- Crypto code is fragile and security-sensitive
- Easier to audit
- Easier to replace

Explicitly avoid mixing crypto logic into repositories.

### 4.4 PasswordRepository (Domain Storage Layer)

#### Responsibilities
- In-memory storage of encrypted password entries
- Enforcing uniqueness
- Managing master password metadata (salt, encrypted master password)
- Delegating persistence

#### Design choice
- Uses `Map<String, String>` (account name → encrypted entry) instead of `List`

#### Why Map
- O(1) lookup by account name
- Natural uniqueness constraint
- Cleaner update/remove logic
- Repository does NOT encrypt/decrypt — it stores what it is given.

### 4.5 PersistenceService (Infrastructure Layer)

#### Responsibilities
- Save/load data from disk
- Serialize:
    - salt
    - encrypted master password
    - entries map

#### Why separate from repository
- Repository stays testable without file I/O
- Persistence can later be replaced (DB, JSON, cloud)

---

## 5. Why Encryption Happens in the Service Layer (Not the Repository)

### Decision
> `PasswordsService` encrypts → the repository stores.

Neither front-end calls `CryptoService` directly. The repository only ever receives
already-encrypted data.

### Why?
- Encryption is a business rule, not storage logic
- Repository should not depend on `CryptoService`
- Keeps the repository reusable and simple

This avoids:
- circular dependencies
- hidden security behavior
- hard-to-test storage logic

---

## 6. Authentication Design

### Signup (first start)
1. A random salt is generated and stored
2. The key is derived from the master password and the salt
3. The master password is encrypted with that key and stored
4. The repository is saved

### Login
1. User enters password
2. Key is derived using the stored salt
3. Entered password is encrypted
4. Encrypted value is compared against the stored encrypted master password
5. Only on success is the derived key kept for the session

Both front-ends use the same logic: `PasswordsService.setMasterPassword()` and
`PasswordsService.login()`.

### Security Decisions
- PBKDF2WithHmacSHA256
- Random per-user salt
- No plaintext passwords stored
- No login attempt limit (deliberate decision)

---

## 7. Testing Strategy

### Tools
- JUnit 4.13
- Mockito

### What is Tested

| Component          | Test Type              | Why                          |
| ------------------ | ---------------------- | ---------------------------- |
| CryptoService      | Real unit tests        | Validate crypto correctness  |
| PasswordRepository | Unit tests             | Business rules, map behavior |
| PersistenceService | Integration-like tests | File save/load integrity     |
| PasswordsService   | Mock-based tests       | Business flow, orchestration |
| PasswordEntry      | Unit tests             | Model/builder behavior       |

> The UI layer (`tui.*` and `Konzola`) is currently **not covered** by automated tests.

### Why Mock
- Avoid file system dependency
- Avoid real crypto during logic tests
- Isolate failures
- Faster tests

Example:
```java
when(crypto.encrypt(any(), any())).thenReturn("encrypted");
```

---

## 8. Why We Do NOT Mock CryptoService Everywhere

> CryptoService is deterministic and self-contained.

We:
- test it with real encryption
- mock it only when testing higher layers

This balances:
- correctness
- performance
- test clarity

---

## 9. Why Manual Dependency Injection

Dependencies are injected manually in `Main`:
```java
new PasswordsService(new CryptoService(), new PasswordRepository(...))
```

### Why?
- No framework overhead
- Educational clarity
- Easy to reason about
- Good fit for a small terminal application

---

## 10. Known Limitations / TODO

- **Master password change is not atomic.** `Konzola.updateMasterPassword()` reads
  all entries, calls `deleteAllEntries()`, then sets the new master password and
  re-adds the entries. A failure in between can lose data. Better: compute everything
  new first (salt, key, re-encrypted entries) and only then replace the repository
  state and save, ideally as one `PasswordsService` operation. The TUI has no master
  password change yet.
- **Settings screen is a placeholder.** `tui.Lanterna.showSettingsScreen()` only shows
  a "not implemented yet" dialog.
- **No service-side validation.** Empty master password / required fields are checked
  only in the UI.
- **No automated tests for the UI layer.**

---

## 11. Current & Future Extension Points

### Already implemented
- **TUI front-end** (Lanterna) alongside the console interface, both on one shared
  service.

### Future
Because of the current design, it is straightforward to add:
- Multiple crypto implementations
- Database persistence
- Password strength policies
- Audit logging
- REST API frontend
- Backup / recovery mechanisms

---

## 12. Design Principles Used

- SOLID principles
- Dependency Inversion
- Single Responsibility
- Explicit dependencies
- Fail-fast behavior
- Test-first mindset

---

## 13. Summary

This project intentionally favors:
- **clarity over cleverness**
- **separation over convenience**
- **testability over speed of writing**

The result is a small but professionally structured Java application
that can grow without collapsing under its own complexity.
