# 🌉 Talent Bridge — Recruitment Management Platform

![License](https://img.shields.io/badge/license-MIT-blue)
![Java Version](https://img.shields.io/badge/java-17-orange)
![Status](https://img.shields.io/badge/status-ongoing-yellow)

> **Talent Bridge** is a full-stack recruitment management application built as part of a university project (PI — Projet Intégré) at ESPRIT. It covers the complete hiring lifecycle: job offers, candidate applications, technical interviews, feedback, and recruitment events — all in one unified platform.

Talent Bridge consists of a **JavaFX desktop application** and an upcoming **Symfony web application**, providing HR teams with tools to manage recruitment workflows and candidates with a smooth, centralized system. The platform features role-based access control, AI-assisted candidate screening, scheduling, notifications, and analytics.

---

## 📋 Table of Contents

* [Overview](#overview)
* [Target Users](#target-users)
* [Tech Stack](#tech-stack)
* [Project Structure](#project-structure)
* [Modules & Features](#modules--features)
* [APIs & Integrations](#apis--integrations)
* [Getting Started](#getting-started)
* [Configuration](#configuration)
* [Database](#database)
* [Web Phase](#web-phase)
* [Team](#team)

---

## Overview

Talent Bridge is a desktop application (JavaFX) that digitizes and streamlines the full recruitment workflow for companies. Recruiters can post job offers, review applications, schedule interviews, manage events, and track analytics — while candidates can browse offers, apply, monitor their application status, attend events, and view interview results.

The application enforces **role-based access control**: each view and action is tailored to the connected user's role (Candidate, Recruiter, or Admin).

---

## Target Users

| Role          | Description                                                                                                                          |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------ |
| **Candidate** | Job seekers who browse offers, submit applications, track status, attend events, and view interview results                          |
| **Recruiter** | HR professionals who post job offers, review applications, schedule interviews, evaluate candidates, and organize recruitment events |
| **Admin**     | Platform administrators who manage users, monitor all applications and statistics, and oversee platform-wide data                    |

---

## Tech Stack

### Desktop Application (Phase 1 — Java)

| Layer             | Technology                                                              |
| ----------------- | ----------------------------------------------------------------------- |
| Language          | Java 17                                                                 |
| UI Framework      | JavaFX 17 (FXML + CSS)                                                  |
| Build Tool        | Apache Maven                                                            |
| Database          | MySQL 8                                                                 |
| DB Driver         | MySQL Connector/J 8.0.33                                                |
| PDF Processing    | Apache PDFBox 2.0.30                                                    |
| Email             | Jakarta Mail 2.0 + Brevo SMTP API                                       |
| SMS               | SMSMobileAPI (HTTP REST)                                                |
| AI / NLP          | Grok AI (xAI API) — cover letter generation, job description moderation |
| AI Matching       | Custom fuzzy matching + Ollama local ranking                            |
| Face Recognition  | Luxand FaceSDK                                                          |
| Maps              | Nominatim (OpenStreetMap) geocoding API                                 |
| Calendar          | Built-in JavaFX custom calendar component                               |
| Meeting Links     | Jit.si auto-generated meeting URLs                                      |
| Password Hashing  | jBCrypt 0.4                                                             |
| JSON              | org.json + Jackson Databind                                             |
| Webcam            | sarxos webcam-capture                                                   |
| Skill Suggestions | ESCO Skills API (European Skills/Competences taxonomy)                  |

### Web Application (Phase 2 — Symfony)

The second phase is a web application built with **Symfony (PHP)** that mirrors and extends the desktop application features in a browser-based interface. It shares the same MySQL database.

| Layer      | Technology                         |
| ---------- | ---------------------------------- |
| Framework  | Symfony 6+ (PHP 8)                 |
| Templating | Twig                               |
| ORM        | Doctrine                           |
| Frontend   | HTML5, CSS3, JavaScript, Bootstrap |
| Database   | MySQL 8 (shared with Java phase)   |

---

## Project Structure

```
src/main/java/
├── Controllers/
│   ├── MainShellController.java          # Main navigation shell
│   ├── application/                      # Application management controllers
│   ├── events/                           # Events controllers
│   ├── interview/                        # Interview & feedback controllers
│   ├── joboffers/                        # Job offers, matching, analytics controllers
│   └── user/                             # Auth, profile, dashboard controllers
│
├── Models/
│   ├── application/                      # JobApplication, CandidateProfile, StatusHistory
│   ├── events/                           # RecruitmentEvent, EventRegistration, EventReview
│   ├── interview/                        # Interview, InterviewFeedback
│   ├── joboffers/                        # JobOffer, MatchingResult, OfferSkill, etc.
│   └── user/                             # User, Candidate, Recruiter, Admin, Skills
│
├── Services/
│   ├── application/                      # Application CRUD, statistics, AI cover letter
│   ├── events/                           # Event management, registration, reviews
│   ├── interview/                        # Interview CRUD, feedback, reminders, meeting links
│   ├── joboffers/                        # Offers, matching, moderation, analytics, recommendations
│   └── user/                             # Auth, profile, email, SMS, face recognition
│
├── Utils/
│   ├── MyDatabase.java                   # Singleton DB connection with auto-reconnect
│   ├── UserContext.java                  # Global session context (logged-in user)
│   ├── ValidationUtils.java              # Phone, email, date, cover letter validators
│   └── ...                               # Other utility classes
│
└── org/example/
    ├── Launcher.java                     # JavaFX entry point (bypasses module restrictions)
    └── MainFX.java                       # Application bootstrap

src/main/resources/
├── MainShell.fxml                        # Main application shell
├── styles.css                            # Global stylesheet
├── email.properties                      # Email config (gitignored — see template)
├── sms.properties                        # SMS config (gitignored — see template)
├── rh.sql                                # Full database schema
└── views/
    ├── application/                      # Application UI (FXML files)
    ├── events/                           # Events UI
    ├── interview/                        # Interview management UI
    ├── joboffers/                        # Job offers UI + analytics dashboard
    └── user/                             # Login, signup, profile, dashboards
```

---

Screenshots:
example:
<img width="1919" height="992" alt="image" src="https://github.com/user-attachments/assets/27363542-e688-4653-9a94-b18ce3a30ffb" />
<img width="1919" height="995" alt="image" src="https://github.com/user-attachments/assets/5ab30ff6-408b-42c3-a0fe-d7d9a562e697" />
<img width="1092" height="662" alt="image" src="https://github.com/user-attachments/assets/2ebf734a-7314-4aab-9396-d8572c1a466b" />




*Talent Bridge — Connecting talent with opportunity.*
