# 🛠️ adminReadme.md — Architecture & Design Notes

This document describes the **architectural decisions, design principles, and testing strategy**
used in the **Java Password Manager** project.

The application runs as a **TUI (Lanterna) by default**, with an automatic **plain
console fallback**. Both front-ends sit on top of the same layered core.

It is intended for **developers and maintainers**, not end users.

---

## 1. High-Level Architecture

The application follows a **layered architecture** with **explicit separation of concerns**.

There are two front-ends on top of a shared core. `Main` picks the front-end at
startup: it tries the TUI first and falls back to the console flow if the TUI cannot
start.

```
┌──────────────────────────────┐        ┌──────────────────────────────┐
│   tui.Lanterna (TUI, default) │        │  PasswordManager (console)    │ ← Front-ends
└───────────────▲──────────────┘        └───────────────▲──────────────┘
                │                                        │
                └────────────────┬───────────────────────┘
                                 │
                   ┌─────────────┴───────────────┐
                   │          Interfaces          │ ← Contracts (ICryptoService,
                   │                              │   IPasswordRepository, ...)
                   └─────────────▲───────────────┘
                                 │
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

The single entry point is `Main`. It wires up the core services and then chooses the
user interface:

```java
try {
    // Default: launch the Lanterna TUI.
    new Lanterna().run();
} catch (Exception e) {
    // Fallback: plain console interface.
    passwordManager.start();
}
```

### Consequences
- The **TUI is the default** experience; the console mode is a **fallback** for
  terminals that cannot run Lanterna.
- `tui.Lanterna` currently **constructs its own core services**
  (`CryptoService`, `PasswordRepository`, `PersistenceService`) instead of reusing the
  instances created in `Main`. This is a known duplication (see §10).

---

## 3. Why Use Interfaces

### Interfaces Used
- `ICryptoService`
- `IPasswordRepository`
- `IPersistenceService`
- `IPasswordManager`

### Reasons

#### 1️⃣ Decoupling
The front-ends do **not care**:
- how encryption is implemented
- how data is stored
- where persistence lives

They only care about **what operations are available**.

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

### 4.1 Front-ends (Application Layer)

Two interchangeable front-ends share the same responsibilities toward the user:
- user interaction (TUI screens or console prompts)
- authentication flow
- business rules / input validation
- orchestration between services (including triggering encryption/decryption)

**tui.Lanterna** — the default TUI. Owns a set of screens:
- `Login` — master-password entry and validation
- `MainMenu` — navigation hub
- `PasswordList` — view / edit / delete entries
- `AddPassword` — create / edit a single entry
- `AppScreen` — shared base screen (full-screen window, Q/ESC exit handling)
- `Theme` — color palette for the terminal UI

**manager.PasswordManager** — the console fallback. Same orchestration role, driven by
text prompts instead of screens.

What the front-ends do **NOT** do:
- encryption internals
- file I/O
- data storage logic

This follows the Single Responsibility Principle (SRP).

### 4.2 CryptoService (Security Layer)

#### Responsibilities
- Key derivation using PBKDF2
- Salt generation
- AES encryption / decryption (including whole-entry encrypt/decrypt)

#### Why it is isolated
- Crypto code is fragile and security-sensitive
- Easier to audit
- Easier to replace

Explicitly avoid mixing crypto logic into repositories.

### 4.3 PasswordRepository (Domain Storage Layer)

#### Responsibilities
- In-memory storage of password entries
- Enforcing uniqueness
- Managing master password metadata (salt, encrypted master password)
- Delegating persistence

#### Design choice
- Uses `Map<String, PasswordEntry>` instead of `List`

#### Why Map
- O(1) lookup by account name
- Natural uniqueness constraint
- Cleaner update/remove logic
- Repository does NOT encrypt/decrypt — it stores what it is given.

### 4.4 PersistenceService (Infrastructure Layer)

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

## 5. Why Encryption Happens in the Application Layer (Not the Repository)

### Decision
> The application/UI layer encrypts → the repository stores.

In console mode `PasswordManager` performs the encryption; in TUI mode `tui.Lanterna`
calls `CryptoService` directly (e.g. `encryptEntry` / `decryptEntry`). In both cases the
repository only ever receives already-encrypted data.

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

### Master Password Flow
1. User enters password
2. Key is derived using the stored salt
3. Entered password is encrypted
4. Encrypted value is compared against the stored encrypted master password

Both front-ends use the same logic: the TUI `Login` flow in `tui.Lanterna.validateUser()`
mirrors `PasswordManager.authenticate()`.

### Security Decisions
- PBKDF2WithHmacSHA256
- Random per-user salt
- No plaintext passwords stored
- 3 login attempts limit (see the limitation in §10)

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
| PasswordManager    | Mock-based tests       | Business flow, orchestration |
| PasswordEntry      | Unit tests             | Model/builder behavior       |

> The TUI layer (`tui.*`) is currently **not covered** by automated tests.

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

Dependencies are injected manually:
```java
new PasswordManager(new CryptoService(), new PasswordRepository(...))
```

### Why?
- No framework overhead
- Educational clarity
- Easy to reason about
- Good fit for a small terminal application

---

## 10. Known Limitations / TODO

- **TUI builds its own services.** `tui.Lanterna` instantiates `CryptoService`,
  `PasswordRepository` and `PersistenceService` itself instead of receiving the
  instances created in `Main`. Consider injecting them for consistency with the
  console path.
- **Duplicated auth logic.** `tui.Lanterna.validateUser()` and
  `PasswordManager.authenticate()` implement the same master-password check. Consider
  extracting a shared authentication helper.
- **Login attempt limit is cosmetic.** The 3-attempt limit only ends the current
  session; it does not lock the app against repeated restarts.
- **Settings screen is a placeholder.** `tui.Lanterna.showSettingsScreen()` only shows
  a "not implemented yet" dialog.
- **No automated tests for the TUI layer.**

---

## 11. Current & Future Extension Points

### Already implemented
- **TUI front-end** (Lanterna) alongside the console interface.

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
