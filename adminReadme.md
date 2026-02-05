# 🛠️ adminReadme.md — Architecture & Design Notes

This document describes the **architectural decisions, design principles, and testing strategy**
used in the **Java Console Password Manager** project.

It is intended for **developers and maintainers**, not end users.

---

## 1. High-Level Architecture

The application follows a **layered architecture** with **explicit separation of concerns**.
````
┌─────────────────────┐
│   PasswordManager    │ ← Application / orchestration layer
└─────────▲───────────┘
│
┌─────────┴───────────┐
│      Interfaces      │ ← Contracts (ICryptoService, IPasswordRepository, etc.)
└─────────▲───────────┘
│
┌─────────┴───────────┐
│    Implementations   │ ← CryptoService, PasswordRepository, PersistenceService
└─────────▲───────────┘
│
┌─────────┴───────────┐
│     File system      │ ← passwords.dat
└──────────────────────┘
````

### Core idea
> **High-level logic depends on abstractions, not implementations**

This keeps the system **testable, maintainable, and extensible**.

---

## 2. Why Use Interfaces

### Interfaces Used
- `ICryptoService`
- `IPasswordRepository`
- `IPersistenceService`
- `IPasswordManager`

### Reasons

#### 1️⃣ Decoupling
The `PasswordManager` does **not care**:
- how encryption is implemented
- how data is stored
- where persistence lives

It only cares about **what operations are available**.

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

3️⃣ Replaceability

Later it can:

- replace CryptoService with a stronger algorithm

- replace file persistence with a database

- add a cloud sync service

➡️ Without touching PasswordManager logic

## 3. Responsibility Breakdown
### 3.1 PasswordManager (Application Layer)

#### Responsibilities

- User interaction (CLI)

- Authentication flow

- Business rules

- Orchestration between services

#### What it does NOT do

- Encryption internals

- File I/O

- Data storage logic

This follows the Single Responsibility Principle (SRP).

### 3.2 CryptoService (Security Layer)

#### Responsibilities

- Key derivation using PBKDF2

- Salt generation

- AES encryption / decryption

#### Why it is isolated

- Crypto code is fragile and security-sensitive

- Easier to audit

- Easier to replace

Explicitly avoid mixing crypto logic into repositories or managers.

### 3.3 PasswordRepository (Domain Storage Layer)

#### Responsibilities

- In-memory storage of password entries

- Enforcing uniqueness

- Managing master password metadata (salt, encrypted master password)

- Delegating persistence

#### Design choice

- Uses Map<String, PasswordEntry> instead of List

#### Why Map

- O(1) lookup by account name

- Natural uniqueness constraint

- Cleaner update/remove logic

- Repository does NOT encrypt/decrypt — it stores what it is given.

## 3.4 PersistenceService (Infrastructure Layer)

### Responsibilities

- Save/load data from disk

- Serialize:

    - salt

    - encrypted master password

    - entries map

### Why separate from repository

- Repository stays testable without file I/O

- Persistence can later be replaced (DB, JSON, cloud)


## 4. Why Encryption Happens in PasswordManager (Not Repository)
### Decision
> PasswordManager encrypts → Repository stores

### Why?

- Encryption is a business rule, not storage logic

- Repository should not depend on CryptoService

- Keeps repository reusable and simple

#### This avoids:

- circular dependencies

- hidden security behavior

- hard-to-test storage logic

## 5. Authentication Design
### Master Password Flow

1. User enters password

2. Key is derived using stored salt

3. Entered password is encrypted

4. Encrypted value is compared

### Security Decisions

- PBKDF2WithHmacSHA256

- Random per-user salt

- No plaintext passwords stored

- 3 login attempts limit

## 6. Testing Strategy
### Tools

- JUnit 4.13

- Mockito

What is Tested

| Component          | Test Type              | Why                          |
| ------------------ | ---------------------- | ---------------------------- |
| CryptoService      | Real unit tests        | Validate crypto correctness  |
| PasswordRepository | Unit tests             | Business rules, map behavior |
| PersistenceService | Integration-like tests | File save/load integrity     |
| PasswordManager    | Mock-based tests       | Business flow, orchestration |


### Why Mock

- Avoid file system dependency

- Avoid real crypto during logic tests

- Isolate failures

- Faster tests

Example:

``` java
when(crypto.encrypt(any(), any())).thenReturn("encrypted");
```

## 7. Why Do NOT Mock CryptoService Everywhere

> CryptoService is deterministic and self-contained.

### We:

- test it with real encryption

- mock it only when testing higher layers

### This balances:

- correctness

- performance

- test clarity

## 8. Why Manual Dependency Injection

Manually inject dependencies:

``` java
new PasswordManager(new CryptoService(), new PasswordRepository())
```

### Why?

- No framework overhead

- Educational clarity

- Easy to reason about

- Perfect for console applications

## 9. Future Extension Points

- Because of the current design, easily add:

    - Multiple crypto implementations

    - Database persistence

    - Password strength policies

    - Audit logging

    - GUI / REST API frontend

    - Backup / recovery mechanisms

## 10. Design Principles Used

- SOLID principles

- Dependency Inversion

- Single Responsibility

- Explicit dependencies

- Fail-fast behavior

- Test-first mindset

## 11. Summary

This project intentionally favors:

- **clarity over cleverness**

- **separation over convenience**

- **testability over speed of writing**

The result is a small but professionally structured Java application
that can grow without collapsing under its own complexity.