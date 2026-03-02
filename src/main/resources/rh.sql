-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Mar 02, 2026 at 02:43 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `rh`
--

-- --------------------------------------------------------

--
-- Table structure for table `admin`
--

CREATE TABLE `admin` (
  `id` bigint(20) NOT NULL,
  `assigned_area` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `admin`
--

INSERT INTO `admin` (`id`, `assigned_area`) VALUES
(1, 'SUPER ADMIN'),
(2, 'NORMAL ADMIN');

-- --------------------------------------------------------

--
-- Table structure for table `application_status_history`
--

CREATE TABLE `application_status_history` (
  `id` bigint(20) NOT NULL,
  `application_id` bigint(20) NOT NULL,
  `status` enum('SUBMITTED','IN_REVIEW','SHORTLISTED','REJECTED','INTERVIEW','HIRED','ARCHIVED','UNARCHIVED') NOT NULL,
  `changed_at` datetime DEFAULT current_timestamp(),
  `changed_by` bigint(20) NOT NULL,
  `note` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `application_status_history`
--

INSERT INTO `application_status_history` (`id`, `application_id`, `status`, `changed_at`, `changed_by`, `note`) VALUES
(1, 1, 'SUBMITTED', '2026-03-02 10:00:00', 3, 'Initial submission'),
(2, 1, 'IN_REVIEW', '2026-03-02 11:30:00', 4, 'Strong profile'),
(3, 1, 'INTERVIEW', '2026-03-02 12:00:00', 4, 'Technical talk'),
(4, 2, 'SUBMITTED', '2026-02-16 15:30:00', 3, 'Web portal app'),
(5, 2, 'SHORTLISTED', '2026-02-25 11:00:00', 4, 'Top 5 candidate'),
(6, 2, 'REJECTED', '2026-03-01 16:30:00', 4, 'Lacks TS depth'),
(7, 3, 'IN_REVIEW', '2026-03-02 02:02:17', 4, 'Le recruteur examine cette candidature'),
(8, 1, 'IN_REVIEW', '2026-03-02 02:02:17', 4, 'Le recruteur examine cette candidature'),
(9, 4, 'IN_REVIEW', '2026-03-02 02:02:17', 4, 'Le recruteur examine cette candidature'),
(10, 1, 'INTERVIEW', '2026-03-02 02:06:25', 4, 'Entretien planifié pour le 03/03/2026 02:00'),
(11, 4, 'INTERVIEW', '2026-03-02 02:24:11', 4, 'Entretien planifié pour le 03/03/2026 14:00'),
(12, 3, 'HIRED', '2026-03-02 02:41:25', 1, 'Statut mis à jour par l\'admin');

-- --------------------------------------------------------

--
-- Table structure for table `candidate`
--

CREATE TABLE `candidate` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `education_level` varchar(100) DEFAULT NULL,
  `experience_years` int(11) DEFAULT NULL,
  `cv_path` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidate`
--

INSERT INTO `candidate` (`id`, `user_id`, `location`, `education_level`, `experience_years`, `cv_path`) VALUES
(3, 3, 'ben arous', 'college', 3, '');

-- --------------------------------------------------------

--
-- Table structure for table `candidate_skill`
--

CREATE TABLE `candidate_skill` (
  `id` bigint(20) NOT NULL,
  `candidate_id` bigint(20) NOT NULL,
  `skill_name` varchar(100) NOT NULL,
  `level` enum('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidate_skill`
--

INSERT INTO `candidate_skill` (`id`, `candidate_id`, `skill_name`, `level`) VALUES
(1, 3, 'Java', 'ADVANCED'),
(2, 3, 'Spring Boot', 'INTERMEDIATE'),
(3, 3, 'SQL', 'ADVANCED'),
(4, 3, 'Angular', 'BEGINNER'),
(5, 3, 'Docker', 'INTERMEDIATE');

-- --------------------------------------------------------

--
-- Table structure for table `event_registration`
--

CREATE TABLE `event_registration` (
  `id` bigint(20) NOT NULL,
  `event_id` bigint(20) NOT NULL,
  `candidate_id` bigint(20) NOT NULL,
  `registered_at` datetime DEFAULT current_timestamp(),
  `attendance_status` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `event_registration`
--

INSERT INTO `event_registration` (`id`, `event_id`, `candidate_id`, `registered_at`, `attendance_status`) VALUES
(1, 1, 3, '2026-03-02 13:00:00', 'PENDING'),
(2, 2, 3, '2026-02-10 10:00:00', 'ATTENDED');

-- --------------------------------------------------------

--
-- Table structure for table `event_review`
--

CREATE TABLE `event_review` (
  `id` bigint(20) NOT NULL,
  `event_id` bigint(20) DEFAULT NULL,
  `candidate_id` bigint(20) DEFAULT NULL,
  `rating` int(11) DEFAULT NULL,
  `comment` text DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `event_review`
--

INSERT INTO `event_review` (`id`, `event_id`, `candidate_id`, `rating`, `comment`, `created_at`) VALUES
(1, 2, 3, 5, 'The Java workshop was very helpful for my current apps!', '2026-03-02 01:00:33');

-- --------------------------------------------------------

--
-- Table structure for table `interview`
--

CREATE TABLE `interview` (
  `id` bigint(20) NOT NULL,
  `application_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `scheduled_at` datetime NOT NULL,
  `duration_minutes` int(11) NOT NULL,
  `mode` enum('ONLINE','ON_SITE') NOT NULL,
  `meeting_link` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `status` enum('SCHEDULED','CANCELLED','DONE') DEFAULT 'SCHEDULED',
  `notes` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `reminder_sent` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `interview`
--

INSERT INTO `interview` (`id`, `application_id`, `recruiter_id`, `scheduled_at`, `duration_minutes`, `mode`, `meeting_link`, `location`, `status`, `notes`, `created_at`, `reminder_sent`) VALUES
(1, 1, 4, '2026-03-02 14:30:00', 45, 'ONLINE', 'https://meet.google.com/7d2-20ba-fbf', NULL, 'SCHEDULED', '', '2026-03-02 02:00:33', 0),
(2, 2, 4, '2026-02-28 15:00:00', 30, 'ONLINE', NULL, NULL, 'DONE', NULL, '2026-03-02 02:00:33', 0),
(3, 1, 4, '2026-03-03 22:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-1-3TvnJbADwMJ91358280', NULL, 'SCHEDULED', '', '2026-03-02 02:06:24', 0),
(4, 4, 4, '2026-03-03 13:00:00', 60, 'ONLINE', 'https://meet.jit.si/TalentBridge-Interview-4-K3jFx8j3h9HR3c8b080', NULL, 'SCHEDULED', '', '2026-03-02 02:24:11', 0);

-- --------------------------------------------------------

--
-- Table structure for table `interview_feedback`
--

CREATE TABLE `interview_feedback` (
  `id` bigint(20) NOT NULL,
  `interview_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `overall_score` int(11) DEFAULT NULL,
  `decision` enum('ACCEPTED','REJECTED') NOT NULL,
  `comment` text DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `interview_feedback`
--

INSERT INTO `interview_feedback` (`id`, `interview_id`, `recruiter_id`, `overall_score`, `decision`, `comment`, `created_at`) VALUES
(1, 2, 4, 60, 'ACCEPTED', 'Candidate struggled with advanced TypeScript types.', '2026-03-02 02:00:33');

-- --------------------------------------------------------

--
-- Table structure for table `job_application`
--

CREATE TABLE `job_application` (
  `id` bigint(20) NOT NULL,
  `offer_id` bigint(20) NOT NULL,
  `candidate_id` bigint(20) NOT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `cover_letter` text DEFAULT NULL,
  `cv_path` varchar(255) DEFAULT NULL,
  `applied_at` datetime DEFAULT current_timestamp(),
  `current_status` enum('SUBMITTED','IN_REVIEW','SHORTLISTED','REJECTED','INTERVIEW','HIRED') DEFAULT 'SUBMITTED',
  `is_archived` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_application`
--

INSERT INTO `job_application` (`id`, `offer_id`, `candidate_id`, `phone`, `cover_letter`, `cv_path`, `applied_at`, `current_status`, `is_archived`) VALUES
(1, 1, 3, '58913065', 'I have 3 years of Java experience.', NULL, '2026-03-02 10:00:00', 'INTERVIEW', 0),
(2, 4, 3, '58913065', 'Interested in freelance React work.', NULL, '2026-02-16 15:30:00', 'REJECTED', 0),
(3, 3, 3, '58913065', 'Transitioning into DevOps.', NULL, '2026-01-25 10:00:00', 'HIRED', 0),
(4, 5, 3, '58913065', 'Looking for an internship at Actia.', NULL, '2026-03-02 08:00:00', 'INTERVIEW', 0);

-- --------------------------------------------------------

--
-- Table structure for table `job_offer`
--

CREATE TABLE `job_offer` (
  `id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `contract_type` enum('CDI','CDD','INTERNSHIP','FREELANCE','PART_TIME','FULL_TIME') NOT NULL,
  `created_at` datetime DEFAULT current_timestamp(),
  `deadline` datetime DEFAULT NULL,
  `status` enum('OPEN','CLOSED','FLAGGED') DEFAULT 'OPEN',
  `quality_score` int(11) DEFAULT NULL,
  `ai_suggestions` text DEFAULT NULL,
  `is_flagged` tinyint(1) NOT NULL DEFAULT 0,
  `flagged_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_offer`
--

INSERT INTO `job_offer` (`id`, `recruiter_id`, `title`, `description`, `location`, `latitude`, `longitude`, `contract_type`, `created_at`, `deadline`, `status`, `quality_score`, `ai_suggestions`, `is_flagged`, `flagged_at`) VALUES
(1, 4, 'Senior Java Developer', 'Expert in Spring Boot and Microservices.', 'Tunis', NULL, NULL, 'CDI', '2026-02-01 10:00:00', '2026-03-15 10:00:00', 'OPEN', NULL, NULL, 0, NULL),
(2, 4, 'Data Analyst Junior', 'Visualizing complex datasets with PowerBI.', 'Ariana', NULL, NULL, 'INTERNSHIP', '2026-01-10 09:00:00', '2026-02-28 23:59:59', 'CLOSED', NULL, NULL, 0, NULL),
(3, 4, 'DevOps Engineer', 'Cloud infrastructure and CI/CD pipelines.', 'Ariana', NULL, NULL, 'CDI', '2026-01-20 08:30:00', '2026-02-20 17:00:00', 'CLOSED', NULL, NULL, 0, NULL),
(4, 4, 'Frontend Developer', 'UI/UX implementation with React and TS.', 'Remote', NULL, NULL, 'FREELANCE', '2026-02-15 11:00:00', '2026-03-30 23:59:59', 'OPEN', NULL, NULL, 0, NULL),
(5, 4, 'Embedded Systems Intern', 'Working on automotive ECU firmware.', 'Ghazela', NULL, NULL, 'INTERNSHIP', '2026-03-01 09:00:00', '2026-04-01 12:00:00', 'OPEN', NULL, NULL, 0, NULL),
(6, 4, 'QA Automation Specialist', 'Testing with Selenium and JUnit.', 'Tunis', NULL, NULL, 'CDD', '2026-02-10 14:00:00', '2026-03-10 18:00:00', 'OPEN', NULL, NULL, 0, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `job_offer_warning`
--

CREATE TABLE `job_offer_warning` (
  `id` bigint(20) NOT NULL,
  `job_offer_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `admin_id` bigint(20) NOT NULL,
  `reason` varchar(255) NOT NULL,
  `message` text NOT NULL,
  `status` enum('SENT','SEEN','RESOLVED','DISMISSED') NOT NULL DEFAULT 'SENT',
  `created_at` datetime NOT NULL DEFAULT current_timestamp(),
  `seen_at` datetime DEFAULT NULL,
  `resolved_at` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `job_offer_warning`
--

INSERT INTO `job_offer_warning` (`id`, `job_offer_id`, `recruiter_id`, `admin_id`, `reason`, `message`, `status`, `created_at`, `seen_at`, `resolved_at`) VALUES
(1, 6, 4, 1, 'Vague Description', 'Specify tech stack for QA role.', 'RESOLVED', '2026-02-11 10:00:00', NULL, NULL),
(2, 4, 4, 2, 'Salary Info', 'Add budget for freelance mission.', 'SENT', '2026-03-02 01:50:00', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `offer_skill`
--

CREATE TABLE `offer_skill` (
  `id` bigint(20) NOT NULL,
  `offer_id` bigint(20) NOT NULL,
  `skill_name` varchar(100) NOT NULL,
  `level_required` enum('BEGINNER','INTERMEDIATE','ADVANCED') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `offer_skill`
--

INSERT INTO `offer_skill` (`id`, `offer_id`, `skill_name`, `level_required`) VALUES
(8, 1, 'Java', 'ADVANCED'),
(9, 1, 'Spring Boot', 'INTERMEDIATE'),
(10, 3, 'Docker', 'INTERMEDIATE'),
(11, 3, 'Kubernetes', 'BEGINNER'),
(12, 4, 'React', 'ADVANCED'),
(13, 4, 'TypeScript', 'INTERMEDIATE'),
(14, 5, 'C Language', 'ADVANCED'),
(15, 6, 'Selenium', 'INTERMEDIATE');

-- --------------------------------------------------------

--
-- Table structure for table `recruiter`
--

CREATE TABLE `recruiter` (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) DEFAULT NULL,
  `company_name` varchar(255) NOT NULL,
  `company_location` varchar(255) DEFAULT NULL,
  `company_description` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `recruiter`
--

INSERT INTO `recruiter` (`id`, `user_id`, `company_name`, `company_location`, `company_description`) VALUES
(4, 4, 'actia', 'Ghazela centre, نهج الأنصار, المدينة الفاضلة, معتمدية رواد, ولاية أريانة, 2083, تونس', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `recruitment_event`
--

CREATE TABLE `recruitment_event` (
  `id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text DEFAULT NULL,
  `event_type` varchar(255) DEFAULT NULL,
  `location` varchar(255) DEFAULT NULL,
  `event_date` datetime NOT NULL,
  `capacity` int(11) DEFAULT 0,
  `meet_link` varchar(255) DEFAULT NULL,
  `created_at` datetime DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `recruitment_event`
--

INSERT INTO `recruitment_event` (`id`, `recruiter_id`, `title`, `description`, `event_type`, `location`, `event_date`, `capacity`, `meet_link`, `created_at`) VALUES
(1, 4, 'Actia Tech Day', 'Discover our ECU projects.', 'Open Day', 'Ghazela centre', '2026-04-10 14:00:00', 50, NULL, '2026-03-02 02:00:33'),
(2, 4, 'Java Workshop', 'Hands-on Spring session.', 'Workshop', 'Online', '2026-02-15 18:00:00', 100, NULL, '2026-03-02 02:00:33'),
(3, 4, 'Career Fair 2026', 'Recruitment drive.', 'Fair', 'Palais des Congrès', '2026-05-20 09:00:00', 500, NULL, '2026-03-02 02:00:33');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `email` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT current_timestamp(),
  `forget_code` varchar(10) DEFAULT NULL,
  `forget_code_expires` datetime DEFAULT NULL,
  `face_person_id` varchar(128) DEFAULT NULL,
  `face_enabled` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `email`, `password`, `first_name`, `last_name`, `phone`, `is_active`, `created_at`, `forget_code`, `forget_code_expires`, `face_person_id`, `face_enabled`) VALUES
(1, 'mohamedmkaouem@gmail.com', '$2a$12$wlTJMvbbsiwyoquTSeumfugPrzNGvezC90kl0NDItmeT4cMfsZkUK', 'Amine', 'mkaouem', '50638321', 1, '2026-03-02 01:49:15', NULL, NULL, NULL, 0),
(2, 'zex54lol@gmail.com', '$2a$12$wlTJMvbbsiwyoquTSeumfugPrzNGvezC90kl0NDItmeT4cMfsZkUK', 'mohamed', 'ben moussa', '53757969', 1, '2026-03-02 01:50:38', NULL, NULL, NULL, 0),
(3, 'aziz15abidi@gmail.com', '$2a$12$4yHczgKOqNQnBLNqvnHUIOFDlhL36/ey13Fi/WcGckzvaIPE5bNIq', 'mohamed aziz', 'abidi', '58913065', 1, '2026-03-02 01:53:17', NULL, NULL, '170f1cab-15d9-11f1-b6c8-0242ac120003', 1),
(4, 'ammounazaidi9@gmail.com', '$2a$12$tuWch2NHVu2Tv1U.rkT8luOCnFMrDchyempYoTVRKjde7DJS9qu3q', 'emna', 'zaidi', '53752303', 1, '2026-03-02 01:55:29', NULL, NULL, NULL, 0);

-- --------------------------------------------------------

--
-- Table structure for table `warning_correction`
--

CREATE TABLE `warning_correction` (
  `id` bigint(20) NOT NULL,
  `warning_id` bigint(20) NOT NULL,
  `job_offer_id` bigint(20) NOT NULL,
  `recruiter_id` bigint(20) NOT NULL,
  `correction_note` text DEFAULT NULL,
  `old_title` varchar(255) DEFAULT NULL,
  `new_title` varchar(255) DEFAULT NULL,
  `old_description` text DEFAULT NULL,
  `new_description` text DEFAULT NULL,
  `status` enum('PENDING','APPROVED','REJECTED') DEFAULT 'PENDING',
  `submitted_at` datetime DEFAULT current_timestamp(),
  `reviewed_at` datetime DEFAULT NULL,
  `admin_note` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `warning_correction`
--

INSERT INTO `warning_correction` (`id`, `warning_id`, `job_offer_id`, `recruiter_id`, `correction_note`, `old_title`, `new_title`, `old_description`, `new_description`, `status`, `submitted_at`, `reviewed_at`, `admin_note`) VALUES
(1, 1, 6, 4, 'Added Selenium/JUnit details.', NULL, NULL, NULL, NULL, 'APPROVED', '2026-03-02 02:00:33', NULL, NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `admin`
--
ALTER TABLE `admin`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `application_status_history`
--
ALTER TABLE `application_status_history`
  ADD PRIMARY KEY (`id`),
  ADD KEY `application_id` (`application_id`),
  ADD KEY `changed_by` (`changed_by`);

--
-- Indexes for table `candidate`
--
ALTER TABLE `candidate`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  ADD PRIMARY KEY (`id`),
  ADD KEY `candidate_id` (`candidate_id`);

--
-- Indexes for table `event_registration`
--
ALTER TABLE `event_registration`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `event_id` (`event_id`,`candidate_id`),
  ADD KEY `candidate_id` (`candidate_id`);

--
-- Indexes for table `event_review`
--
ALTER TABLE `event_review`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_event_review_event` (`event_id`),
  ADD KEY `fk_event_review_candidate` (`candidate_id`);

--
-- Indexes for table `interview`
--
ALTER TABLE `interview`
  ADD PRIMARY KEY (`id`),
  ADD KEY `application_id` (`application_id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `interview_id` (`interview_id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `job_application`
--
ALTER TABLE `job_application`
  ADD PRIMARY KEY (`id`),
  ADD KEY `offer_id` (`offer_id`),
  ADD KEY `candidate_id` (`candidate_id`);

--
-- Indexes for table `job_offer`
--
ALTER TABLE `job_offer`
  ADD PRIMARY KEY (`id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_warn_offer` (`job_offer_id`),
  ADD KEY `fk_warn_recruiter` (`recruiter_id`),
  ADD KEY `fk_warn_admin` (`admin_id`);

--
-- Indexes for table `offer_skill`
--
ALTER TABLE `offer_skill`
  ADD PRIMARY KEY (`id`),
  ADD KEY `offer_id` (`offer_id`);

--
-- Indexes for table `recruiter`
--
ALTER TABLE `recruiter`
  ADD PRIMARY KEY (`id`);

--
-- Indexes for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  ADD PRIMARY KEY (`id`),
  ADD KEY `recruiter_id` (`recruiter_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`);

--
-- Indexes for table `warning_correction`
--
ALTER TABLE `warning_correction`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_correction_warning` (`warning_id`),
  ADD KEY `fk_correction_job` (`job_offer_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `application_status_history`
--
ALTER TABLE `application_status_history`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `event_registration`
--
ALTER TABLE `event_registration`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `event_review`
--
ALTER TABLE `event_review`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `interview`
--
ALTER TABLE `interview`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `job_application`
--
ALTER TABLE `job_application`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `job_offer`
--
ALTER TABLE `job_offer`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `offer_skill`
--
ALTER TABLE `offer_skill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT for table `warning_correction`
--
ALTER TABLE `warning_correction`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `admin`
--
ALTER TABLE `admin`
  ADD CONSTRAINT `admin_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `application_status_history`
--
ALTER TABLE `application_status_history`
  ADD CONSTRAINT `application_status_history_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `application_status_history_ibfk_2` FOREIGN KEY (`changed_by`) REFERENCES `users` (`id`);

--
-- Constraints for table `candidate`
--
ALTER TABLE `candidate`
  ADD CONSTRAINT `candidate_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  ADD CONSTRAINT `candidate_skill_ibfk_1` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `event_registration`
--
ALTER TABLE `event_registration`
  ADD CONSTRAINT `event_registration_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `recruitment_event` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `event_registration_ibfk_2` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `event_review`
--
ALTER TABLE `event_review`
  ADD CONSTRAINT `fk_event_review_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_event_review_event` FOREIGN KEY (`event_id`) REFERENCES `recruitment_event` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `interview`
--
ALTER TABLE `interview`
  ADD CONSTRAINT `interview_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `interview_ibfk_2` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  ADD CONSTRAINT `interview_feedback_ibfk_1` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `interview_feedback_ibfk_2` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_application`
--
ALTER TABLE `job_application`
  ADD CONSTRAINT `job_application_ibfk_1` FOREIGN KEY (`offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `job_application_ibfk_2` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_offer`
--
ALTER TABLE `job_offer`
  ADD CONSTRAINT `job_offer_ibfk_1` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  ADD CONSTRAINT `fk_warn_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_warn_offer` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_warn_recruiter` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Constraints for table `offer_skill`
--
ALTER TABLE `offer_skill`
  ADD CONSTRAINT `offer_skill_ibfk_1` FOREIGN KEY (`offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `recruiter`
--
ALTER TABLE `recruiter`
  ADD CONSTRAINT `recruiter_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  ADD CONSTRAINT `recruitment_event_ibfk_1` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `warning_correction`
--
ALTER TABLE `warning_correction`
  ADD CONSTRAINT `fk_correction_job` FOREIGN KEY (`job_offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `fk_correction_warning` FOREIGN KEY (`warning_id`) REFERENCES `job_offer_warning` (`id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
