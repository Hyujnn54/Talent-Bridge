# 🌉 Talent Bridge — Recruitment Management Platform

> **Talent Bridge** is a full-stack recruitment management application built as part of a university project (PI — Projet Intégré) at ESPRIT. It covers the complete hiring lifecycle: job offers, candidate applications, technical interviews, feedback, and events — all in one unified platform.

---

## 📋 Table of Contents

- [Overview](#overview)
- [Target Users](#target-users)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Modules & Features](#modules--features)
- [APIs & Integrations](#apis--integrations)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [Database](#database)
- [Web Phase](#web-phase)
- [Team](#team)

---

## Overview

Talent Bridge is a desktop application (JavaFX) that digitizes and streamlines the full recruitment workflow for companies. Recruiters can post job offers, review applications, schedule interviews, manage events, and track analytics — while candidates can browse offers, apply, track their application status, join events, and view their interview results.

The application enforces **role-based access control**: each view and action is tailored to the connected user's role (Candidate, Recruiter, or Admin).

---

## Target Users

| Role | Description |
|------|-------------|
| **Candidate** | Job seekers who browse offers, submit applications with CV and cover letter, track application status, attend events, and view their interview schedule and results |
| **Recruiter** | HR professionals who post job offers, review incoming applications, schedule interviews, evaluate candidates through feedback, and organize recruitment events |
| **Admin** | Platform administrators who manage users, monitor all applications and statistics, and oversee platform-wide data |

---

## Tech Stack

### Desktop Application (Phase 1 — Java)

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| UI Framework | JavaFX 17 (FXML + CSS) |
| Build Tool | Apache Maven |
| Database | MySQL 8 |
| DB Driver | MySQL Connector/J 8.0.33 |
| PDF Processing | Apache PDFBox 2.0.30 |
| Email | Jakarta Mail 2.0 + Brevo SMTP API |
| SMS | SMSMobileAPI (HTTP REST) |
| AI / NLP | Grok AI (xAI API) — cover letter generation, job description moderation |
| AI Matching | Custom fuzzy matching + Ollama local ranking |
| Face Recognition | Luxand FaceSDK |
| Maps | Nominatim (OpenStreetMap) geocoding API |
| Calendar | Built-in JavaFX custom calendar component |
| Meeting Links | Jit.si auto-generated meeting URLs |
| Password Hashing | jBCrypt 0.4 |
| JSON | org.json + Jackson Databind |
| Webcam | sarxos webcam-capture |
| Skill Suggestions | ESCO Skills API (European Skills/Competences taxonomy) |

### Web Application (Phase 2 — Symfony)

> The second phase of the project is a web application built with **Symfony (PHP)** that mirrors and extends the desktop application features in a browser-based interface. It shares the same MySQL database.

| Layer | Technology |
|-------|-----------|
| Framework | Symfony 6+ (PHP 8) |
| Templating | Twig |
| ORM | Doctrine |
| Frontend | HTML5, CSS3, JavaScript, Bootstrap |
| Database | MySQL 8 (shared with Java phase) |

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

## Modules & Features

### 🔐 Authentication & User Management
- Secure login and registration (password hashed with BCrypt)
- Role-based access: Candidate / Recruiter / Admin
- Password reset via email OTP
- Face recognition login (Luxand FaceSDK)
- Profile management with skills, experience, and education
- ESCO Skills API integration for standardized skill suggestions

### 📄 Job Offers
- Recruiters can create, edit, and delete job offers
- Candidates browse and search offers (by title, location, contract type)
- AI-powered **content moderation** (Grok AI) flags inappropriate job descriptions
- AI-powered **warning corrections** suggest improvements to offer text
- **Analytics dashboard**: views, applications count, conversion rates
- **Fuzzy search** for typo-tolerant searching
- Map integration (Nominatim) to visualize job locations

### 📨 Applications
- Candidates apply with phone number, cover letter, and optional PDF CV
- AI-powered **cover letter generation** (Grok AI) based on candidate profile and CV
- AI **ranking/scoring** of candidates per offer (Ollama local model)
- Real-time validation: phone format (Tunisia/France), cover letter length
- Recruiters review all applications for their offers
- Accept → triggers automatic interview creation flow
- Reject → removes the application
- Application **status history** tracking
- Email confirmation sent to candidate on application submission (Brevo API)
- Admin view for all platform applications + statistics

### 📅 Interviews
- Recruiters create interviews **from accepted applications** only
- Two modes: **Online** (auto-generates Jit.si meeting link) / **On-site** (location field)
- Full CRUD: create, update, delete interviews
- **Interview Feedback**: recruiter scores the candidate (score + comments + decision: Accept/Reject)
- Candidates see their upcoming interviews and results
- Search & filter: by date (calendar picker), status, mode
- Interactive **calendar view** with colored indicators for interview days
- **Automatic email reminders** 24h before each interview (Brevo SMTP)
- **Automatic SMS reminders** 24h before each interview (SMSMobileAPI)
- Background scheduler checks every 5 minutes for upcoming interviews

### 🎪 Events
- Recruiters post recruitment events (job fairs, workshops, etc.)
- Candidates browse and register for events
- Event reviews and attendance tracking
- Past events archive

### 👤 Admin
- User management dashboard
- Application statistics and analytics
- Platform-wide oversight

---

## APIs & Integrations

| Integration | Purpose | Notes |
|-------------|---------|-------|
| **Brevo** (formerly Sendinblue) | Transactional emails (reminders, confirmations) | Configure in `email.properties` |
| **SMSMobileAPI** | SMS interview reminders | Configure in `sms.properties` |
| **Grok AI** (xAI) | Cover letter generation, job offer moderation, AI suggestions | API key in `config.properties` |
| **Ollama** | Local AI ranking of candidates | Runs locally, no key needed |
| **Nominatim** (OpenStreetMap) | Job location geocoding and map display | Free, no key needed |
| **Jit.si** | Auto-generate meeting links for online interviews | Free, no key needed |
| **ESCO Skills API** | European standardized skill suggestions | Free, no key needed |
| **Luxand FaceSDK** | Face recognition for login | License required |

---

## Getting Started

### Prerequisites

- Java 17 (Microsoft Build recommended)
- Maven 3.8+
- MySQL 8.0+
- IntelliJ IDEA (recommended)

### Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Hyujnn54/PI_java_web.git
   cd PI_java_web
   ```

2. **Import the database**
   ```bash
   mysql -u root -p < src/main/resources/rh.sql
   ```

3. **Configure the database connection**
   Edit `src/main/resources/config.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/rh
   db.user=root
   db.password=your_password
   ```

4. **Configure email** (copy the template)
   ```bash
   cp src/main/resources/email.properties.template src/main/resources/email.properties
   ```
   Fill in your Brevo API key and SMTP credentials.

5. **Configure SMS** (optional)
   ```bash
   cp src/main/resources/sms.properties.template src/main/resources/sms.properties
   ```
   Fill in your SMSMobileAPI key and endpoint.

6. **Build and run**
   ```bash
   mvn clean compile
   mvn javafx:run
   ```
   Or run `org.example.Launcher` directly from IntelliJ IDEA.

---

## Configuration

### Sensitive files (gitignored)

These files contain secrets and are **not** committed to the repository. Templates are provided:

| File | Template | Purpose |
|------|----------|---------|
| `email.properties` | `email.properties.template` | Brevo SMTP credentials |
| `sms.properties` | `sms.properties.template` | SMSMobileAPI credentials |

### User Context (no login for now)

Currently the application starts directly in the main shell without a login page (login integration is prepared but bypassed for the desktop phase demo). The default user context is set in `Utils/UserContext.java`. To switch between Recruiter and Candidate views, use the role toggle button in the top bar.

---

## Database

The full schema is available at `src/main/resources/rh.sql`.

Key tables:

| Table | Description |
|-------|-------------|
| `users` | All users (candidate, recruiter, admin) with role discriminator |
| `job_offer` | Job offers posted by recruiters |
| `job_application` | Candidate applications linking user to job offer |
| `interview` | Scheduled interviews linked to applications |
| `interview_feedback` | Recruiter feedback and scores per interview |
| `recruitment_event` | Recruitment events |
| `event_registration` | Candidate registrations to events |

---

## Web Phase

> 🚧 **Phase 2 — In Progress**

The web application (Symfony/PHP) is being developed in parallel by the same team. It uses the **same MySQL database** (`rh`) as the desktop application, ensuring data consistency between both phases.

The web phase will include:
- Full browser-based access to all features
- Responsive design for mobile and desktop browsers
- RESTful API endpoints
- Extended admin dashboard with charts and KPIs
- Public-facing job board for anonymous browsing

---

## Team

This project is developed by a team of students at **ESPRIT** (École Supérieure Privée d'Ingénierie et de Technologies), Tunisia.

Each team member is responsible for a specific module:

| Module | Responsible |
|--------|-------------|
| Authentication & User Management | User team |
| Job Offers & Matching | Job Offers team |
| Applications & AI | Applications team |
| Interviews & Reminders | Interview team |
| Events | Events team |

---

## Notes for Team Members

- **Do not commit** `email.properties` or `sms.properties` — they are gitignored for security.
- Each module has its own subfolder under `Controllers/`, `Services/`, `Models/`, and `views/` — keep your code in your own folder.
- The `MainShellController.java` wires all modules together via navigation buttons — update it when adding new views.
- Use `Utils.UserContext` to read the current user's role and ID in your controllers.
- Use `Utils.MyDatabase.getInstance().getConnection()` for all database access — it handles auto-reconnect.
- Follow the existing FXML + CSS patterns in `styles.css` for UI consistency.

---

*Talent Bridge — Connecting talent with opportunity.*

