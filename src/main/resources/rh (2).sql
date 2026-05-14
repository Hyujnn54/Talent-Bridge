-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 14, 2026 at 12:31 AM
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
(11, 'General Management');

-- --------------------------------------------------------

--
-- Table structure for table `application_status_history`
--

CREATE TABLE `application_status_history` (
  `id` bigint(20) NOT NULL,
  `application_id` bigint(20) NOT NULL,
  `status` enum('SUBMITTED','IN_REVIEW','SHORTLISTED','REJECTED','INTERVIEW','HIRED','ARCHIVED','UNARCHIVED') NOT NULL,
  `changed_at` datetime DEFAULT current_timestamp(),
  `changed_by_id` bigint(20) NOT NULL,
  `note` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `application_status_history`
--

INSERT INTO `application_status_history` (`id`, `application_id`, `status`, `changed_at`, `changed_by_id`, `note`) VALUES
(143, 57, 'SUBMITTED', '2026-05-07 02:23:00', 12, 'aziz abidi submitted a new application.'),
(144, 57, 'IN_REVIEW', '2026-05-07 02:46:38', 13, 'Interview scheduled; application moved to In Review.'),
(145, 58, 'SUBMITTED', '2026-05-07 14:56:57', 12, 'aziz abidi submitted a new application.'),
(146, 58, 'IN_REVIEW', '2026-05-07 14:58:04', 13, 'Interview scheduled; application moved to In Review.'),
(147, 59, 'SUBMITTED', '2026-05-09 13:11:12', 12, 'aziz abidi submitted a new application.');

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
  `cv_path` varchar(255) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `candidate`
--

INSERT INTO `candidate` (`id`, `user_id`, `location`, `education_level`, `experience_years`, `cv_path`, `latitude`, `longitude`) VALUES
(12, NULL, 'Ariana', 'Uni', 6, NULL, NULL, NULL),
(14, NULL, 'tunisie', 'test', 2, '69e89ab5140e6.pdf', NULL, NULL),
(15, NULL, 'Sfax', 'Bac', 6, '69e89af12c324.pdf', NULL, NULL),
(16, NULL, 'tunis', 'sdfqsdfq', 2, NULL, NULL, NULL),
(101, NULL, 'Tunis', 'Engineer', 5, NULL, NULL, NULL),
(102, NULL, 'Tunis', 'Bachelor', 2, NULL, NULL, NULL),
(103, NULL, 'Tunis', 'Bac', 1, NULL, NULL, NULL),
(104, NULL, 'tunis', 'bachlor', 2, NULL, NULL, NULL),
(105, NULL, 'tunis', 'Uni', 3, NULL, NULL, NULL),
(106, NULL, 'sdsqdq', 'Bac', 2, NULL, NULL, NULL);

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
(29, 12, 'java', 'INTERMEDIATE'),
(30, 12, 'php', 'ADVANCED'),
(31, 12, 'js', 'ADVANCED'),
(32, 12, 'gaming', 'ADVANCED'),
(33, 101, 'php', 'ADVANCED'),
(34, 101, 'javascript', 'INTERMEDIATE'),
(35, 101, 'sql', 'INTERMEDIATE'),
(36, 102, 'php', 'INTERMEDIATE'),
(37, 102, 'javascript', 'BEGINNER'),
(38, 103, 'html', 'BEGINNER'),
(39, 103, 'css', 'BEGINNER'),
(40, 12, 'php', 'ADVANCED'),
(41, 12, 'SQL', 'ADVANCED'),
(42, 12, 'git', 'ADVANCED'),
(43, 105, 'AWS/Azure/GCP', 'ADVANCED'),
(44, 12, 'Docker & Kubernetes', 'ADVANCED'),
(45, 12, 'Linux System Administration', 'ADVANCED');

-- --------------------------------------------------------

--
-- Table structure for table `doctrine_migration_versions`
--

CREATE TABLE `doctrine_migration_versions` (
  `version` varchar(191) NOT NULL,
  `executed_at` datetime DEFAULT NULL,
  `execution_time` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `doctrine_migration_versions`
--

INSERT INTO `doctrine_migration_versions` (`version`, `executed_at`, `execution_time`) VALUES
('DoctrineMigrations\\Version20260507021600', '2026-05-07 08:17:55', 48),
('DoctrineMigrations\\Version20260507030500', '2026-05-07 08:54:23', 33),
('DoctrineMigrations\\Version20260507031500', '2026-05-07 08:56:12', 39),
('DoctrineMigrations\\Version20260507032500', '2026-05-07 08:57:51', 36),
('DoctrineMigrations\\Version20260507033500', '2026-05-07 09:00:37', 40),
('DoctrineMigrations\\Version20260507034500', '2026-05-07 09:01:41', 32),
('DoctrineMigrations\\Version20260507035500', '2026-05-07 09:02:52', 41),
('DoctrineMigrations\\Version20260507040500', '2026-05-07 09:04:00', 34),
('DoctrineMigrations\\Version20260507041500', '2026-05-07 09:06:42', 43),
('DoctrineMigrations\\Version20260507042500', '2026-05-07 09:14:36', 36);

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
(2, 11, 104, '2026-04-28 17:26:13', 'confirmed'),
(3, 11, 12, '2026-04-28 17:35:03', 'rejected'),
(4, 12, 12, '2026-04-28 17:50:23', 'registered'),
(5, 13, 12, '2026-05-07 14:57:14', 'confirmed');

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
(1, 57, 13, '2026-05-18 01:46:00', 60, 'ONLINE', 'https://meet.jit.si/talentbridge-interview-app57-3977b65353d8', '', 'SCHEDULED', 'u need to be there in time or u\'re gonna be rejected', '2026-05-07 02:46:38', 0),
(2, 58, 13, '2026-05-22 13:57:00', 60, 'ONLINE', 'https://meet.jit.si/talentbridge-interview-app58-d073151496c1', '', 'SCHEDULED', 'u should be right on time', '2026-05-07 14:58:04', 0);

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
(57, 1778113346067503, 12, '+21658999875', 'I am writing to express my interest in the Devops internship position in Tunis. As a highly motivated and experienced professional with 6 years of experience, I am confident that my skills and expertise make me an ideal candidate for this role. With advanced proficiency in Docker and Kubernetes, I possess a deep understanding of containerization and orchestration, which I believe are essential skills for a Devops position.\n\nIn addition to my technical expertise, I am well-versed in Linux System Administration, git, and SQL, which I believe are crucial for managing and deploying applications. My programming skills in Java, JavaScript, and PHP also enable me to collaborate effectively with development teams. As a gamer, I have developed strong problem-solving skills, which I believe are transferable to a Devops role.\n\nI am excited about the opportunity to apply my skills and knowledge in a real-world setting and contribute to the success of your team. I am a quick learner, and I thrive in environments where I can learn and grow. I am confident that my passion for technology, combined with my experience and skills, make me a strong candidate for this internship. I look forward to the opportunity to discuss my application and how I can contribute to your team.', 'Aziz-Abidi-CV-69fbdb643b3bf.pdf', '2026-05-07 02:23:00', 'IN_REVIEW', 0),
(58, 1778158525770643, 12, '+21658999875', 'I am writing to express my interest in the Web designer internship position in Sousse. As a highly motivated individual with a strong educational background from Uni and 6 years of experience, I am confident in my ability to contribute to your team. My advanced skills in Docker & Kubernetes, git, js, Linux System Administration, and SQL make me a strong candidate for this role. Additionally, my intermediate knowledge of java and advanced knowledge of php will enable me to effectively design and develop web applications.\n\nI am particularly drawn to this internship because it aligns with my passion for gaming and web development. I am excited about the opportunity to apply my skills and knowledge in a real-world setting and gain valuable experience in the field. As a resident of Ariana, I am willing to relocate to Sousse for the internship and am available to start immediately. I can be contacted at azizgamercr7@gmail.com or 58999875. I am confident that my skills and experience make me an ideal candidate for this position, and I look forward to the opportunity to discuss my application with you further.', 'Aziz-Abidi-CV-69fc8c1971bcc.pdf', '2026-05-07 14:56:57', 'IN_REVIEW', 0),
(59, 1778324960055289, 12, '+21658999875', 'Dear Hiring Manager,\nI am writing to express my interest in the cybersecurity analysts position at your esteemed organization in Tunis. As a highly motivated and experienced professional with 6 years of experience, I am confident that my skills and expertise align with the requirements of the job.\nWith advanced proficiency in Linux System Administration, Docker, and Kubernetes, I possess a strong foundation in system security and management. My expertise in programming languages such as Java, PHP, and JavaScript, as well as my knowledge of SQL, will enable me to effectively analyze and mitigate potential security threats. Additionally, my advanced skills in git and gaming will allow me to think creatively and stay ahead of emerging threats.\nI am particularly drawn to this role because of the opportunity to work in a dynamic and challenging environment. As a resident of Ariana, I am willing to relocate to Tunis for this position. I am excited about the prospect of joining your team and contributing my skills and experience to help protect your organization\'s systems and data. I look forward to the opportunity to discuss my application and how I can contribute to your team\'s success. Please feel free to contact me at azizgamercr7@gmail.com or 58999875.', 'Aziz-Abidi-CV-69ff165002fe3.pdf', '2026-05-09 13:11:12', 'SUBMITTED', 0);

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
(1778113346067503, 13, 'Devops', 'We are seeking a highly skilled Devops engineer to join our team, responsible for ensuring the smooth operation of our systems and infrastructure. The ideal candidate will have experience in cloud computing, containerization, and automation. The Devops engineer will work closely with our development team to design, implement, and maintain our infrastructure. This is an excellent opportunity for a motivated and experienced professional to join our dynamic team. The successful candidate will have a strong understanding of Devops principles and practices. Collaboration and communication skills are essential for this role.', 'Tunis', 33.8439408, 9.400138, 'INTERNSHIP', '2026-05-07 02:22:25', '2026-05-30 00:00:00', 'OPEN', 100, '', 0, '2026-05-07 02:22:25'),
(1778158525770643, 13, 'Web designer', 'We are seeking a highly skilled Web Designer to join our team. The successful candidate will be responsible for creating visually appealing and user-friendly websites. The ideal candidate will have a strong portfolio and excellent design skills. The Web Designer will work closely with our development team to ensure seamless integration of design and functionality. The candidate will also be responsible for staying up-to-date with the latest design trends and technologies. If you are a motivated and creative individual with a passion for web design, we encourage you to apply.', 'Sousse', 35.8288284, 10.6405254, 'INTERNSHIP', '2026-05-07 14:55:25', '2026-05-29 13:55:00', 'OPEN', 100, '', 0, '2026-05-07 14:55:25'),
(1778324960055289, 13, 'cybersecurity analysts', 'We are seeking a highly skilled cybersecurity analyst to join our team. The successful candidate will be responsible for monitoring and analyzing security event logs, identifying potential threats, and implementing measures to prevent security breaches. The ideal candidate will have a strong understanding of network security, threat analysis, and incident response. The cybersecurity analyst will work closely with the IT team to ensure the security and integrity of our systems and data. The role requires strong problem-solving skills, attention to detail, and excellent communication skills.', 'Tunis', 33.8439408, 9.400138, 'CDD', '2026-05-09 13:09:18', '2026-05-30 12:08:00', 'OPEN', 100, '', 0, '2026-05-09 13:09:18');

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
(1778113346069400, 1778113346067503, 'AWS', 'INTERMEDIATE'),
(1778113346069447, 1778113346067503, 'Docker', 'INTERMEDIATE'),
(1778113346070248, 1778113346067503, 'Kubernetes', 'INTERMEDIATE'),
(1778113346070725, 1778113346067503, 'Jenkins', 'INTERMEDIATE'),
(1778158525771433, 1778158525770643, 'CSS', 'BEGINNER'),
(1778158525771920, 1778158525770643, 'HTML', 'BEGINNER'),
(1778158525771938, 1778158525770643, 'JavaScript', 'BEGINNER'),
(1778158525772578, 1778158525770643, 'UI/UX design', 'BEGINNER'),
(1778324960056148, 1778324960055289, 'Network Security', 'INTERMEDIATE'),
(1778324960056207, 1778324960055289, 'Threat Analysis', 'INTERMEDIATE'),
(1778324960056678, 1778324960055289, 'Incident Response', 'ADVANCED'),
(1778324960056899, 1778324960055289, 'Firewall Configuration', 'INTERMEDIATE');

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
(13, NULL, 'Google', 'نهج بنان, الياسمينات, معتمدية المدينة الجديدة, Ben Arous, 2014, Tunisia', NULL);

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
(11, 13, 'devops hiring', 'Join us for an exclusive DevOps Hiring Day on April 30th to showcase your technical expertise and fast-track your career with our engineering team. You will have the unique opportunity to interview on-site, meet our lead developers, and potentially secure a job offer before the day ends. Space is limited to 50 participants, so reserve your spot now to build the future of our infrastructure together in Manouba.', 'Hiring Day', 'Mergueb, Choubene, حميم, معتمدية المرناقية, Manouba, Tunisia', '2026-04-30 13:31:00', 2, '', '2026-04-27 14:32:10'),
(12, 13, 'web designer', 'Join us for an exclusive Hiring Day dedicated to web designers on April 30, 2026, at 4:40 PM in the heart of Tunis, Bab Souika. This is your chance to meet hiring managers face to face, showcase your creative portfolio, and potentially walk away with a job offer on the spot. With only 50 spots available, don\'t wait — secure your place now and take the next step in your design career.', 'Hiring Day', 'حمام الرميمي, معتمدية باب سويقة, Tunis, 1005, Tunisia', '2026-04-30 16:40:00', 50, '', '2026-04-28 17:40:47'),
(13, 13, 'Php workshop', 'Elevate your backend development skills at our exclusive PHP workshop hosted at the scenic Lido Hotel & Spa Nabeul on May 23rd. Join our engineering team for an afternoon of hands-on coding, architectural insights, and networking in a professional yet relaxed atmosphere. Secure one of the 50 available spots today to sharpen your technical expertise and explore future career opportunities with us.', 'Workshop', 'Lido hôtel & spa nabeul, Rue de la Sculpture des Pierres, الفهري, معتمدية دار شعبان الفهري, Nabeul, 8075, Tunisia', '2026-05-23 13:55:00', 50, '', '2026-05-07 14:56:14');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` bigint(20) NOT NULL,
  `email` varchar(255) NOT NULL,
  `roles` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
  `password` varchar(255) NOT NULL,
  `first_name` varchar(100) DEFAULT NULL,
  `last_name` varchar(100) DEFAULT NULL,
  `phone` varchar(30) DEFAULT NULL,
  `is_active` tinyint(1) DEFAULT 1,
  `created_at` datetime DEFAULT current_timestamp(),
  `forget_code` varchar(10) DEFAULT NULL,
  `forget_code_expires` datetime DEFAULT NULL,
  `face_person_id` varchar(128) DEFAULT NULL,
  `face_enabled` tinyint(1) NOT NULL DEFAULT 0,
  `discr` varchar(255) NOT NULL,
  `google_authenticator_secret` varchar(255) DEFAULT NULL,
  `google_authenticator_enabled` tinyint(1) NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `email`, `roles`, `password`, `first_name`, `last_name`, `phone`, `is_active`, `created_at`, `forget_code`, `forget_code_expires`, `face_person_id`, `face_enabled`, `discr`, `google_authenticator_secret`, `google_authenticator_enabled`) VALUES
(11, 'admin@gmail.com', '[\"ROLE_ADMIN\"]', '$2y$13$CkoFplROsEf3/C62Cty17O5sHbPI1dE66rLjLhVZ/k1TsPJYf0/3W', 'admin', 'admin', '12345678', 1, '2026-04-14 19:09:08', NULL, NULL, NULL, 0, 'admin', NULL, 0),
(12, 'azizgamercr7@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$FAvaxi7ZvyEjHqNUlVP/lO3oHUEXHQHxMIs5wzt/eKLlVlhwoP/f.', 'aziz', 'abidi', '58999875', 1, '2026-04-14 19:10:35', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(13, 'recruiter@gmail.com', '[\"ROLE_RECRUITER\"]', '$2y$13$aQb681RV8f2CUrdj9l7PsukA9angwYHo9NSaR7E.LHNwmJ/8gzSxW', 'recruiter', 'user', '50555675', 1, '2026-04-14 19:11:31', NULL, NULL, NULL, 0, 'recruiter', NULL, 0),
(14, 'rayen@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$Rev/U0Apo8KTMe3tF1N0X.aEYLiHMPBq.BMBQA5eNqJ8A7zc./cNm', 'rayen', 'ben amor', '57732998', 1, '2026-04-22 11:53:56', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(15, 'emna@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$DZyjGQ0.8omxN70CQeWrzuO5LTyUaPB1z9upvRNjm2DtFTjiGxFji', 'emna', 'zaidi', '67899876', 1, '2026-04-22 11:54:56', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(16, 'test@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$r7GETTv96LFRqNRdtGIlEOVxWsmlo0AfTl017qGhtMQtpe9klD4A6', 'test', 'testtt', '12345678', 1, '2026-04-22 12:01:49', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(101, 'high@candidate.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$r7GETTv96LFRqNRdtGIlEOVxWsmlo0AfTl017qGhtMQtpe9klD4A6', 'High', 'Match', '11111111', 1, '2026-04-22 11:02:05', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(102, 'medium@candidate.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$r7GETTv96LFRqNRdtGIlEOVxWsmlo0AfTl017qGhtMQtpe9klD4A6', 'Medium', 'Match', '22222222', 1, '2026-04-22 11:02:05', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(103, 'low@candidate.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$r7GETTv96LFRqNRdtGIlEOVxWsmlo0AfTl017qGhtMQtpe9klD4A6', 'Low', 'Match', '33333333', 1, '2026-04-22 11:02:05', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(104, 'test2@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$eMxr3RYp9k3JJkZ/wSxxcOoOSHYsQNmhxnCrDkiZqPM0F64R7B8K6', 'test', '2', '12345678', 1, '2026-04-22 15:18:10', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(105, 'test3@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$mUKCFBCgJBOn4yVdigQHkeE7q3kSF86ymV8jEIlGYpzq1HZHFzy6W', 'test', '3', '50999888', 1, '2026-04-22 15:19:22', NULL, NULL, NULL, 0, 'candidate', NULL, 0),
(106, 'test4@gmail.com', '[\"ROLE_CANDIDATE\"]', '$2y$13$n7ZO6D9bAs9KCLYPF2RLPua9qXqMfy0SUIpJMQbHDdkJId5krjnuW', 'test', '4', '58913065', 1, '2026-04-22 15:22:12', NULL, NULL, NULL, 0, 'candidate', NULL, 0);

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
  ADD KEY `changed_by_id` (`changed_by_id`);

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
-- Indexes for table `doctrine_migration_versions`
--
ALTER TABLE `doctrine_migration_versions`
  ADD PRIMARY KEY (`version`);

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
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=148;

--
-- AUTO_INCREMENT for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=46;

--
-- AUTO_INCREMENT for table `event_registration`
--
ALTER TABLE `event_registration`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `event_review`
--
ALTER TABLE `event_review`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `interview`
--
ALTER TABLE `interview`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8;

--
-- AUTO_INCREMENT for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `job_application`
--
ALTER TABLE `job_application`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=60;

--
-- AUTO_INCREMENT for table `job_offer`
--
ALTER TABLE `job_offer`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2000000000000002;

--
-- AUTO_INCREMENT for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1777389031679568;

--
-- AUTO_INCREMENT for table `offer_skill`
--
ALTER TABLE `offer_skill`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1778324960056900;

--
-- AUTO_INCREMENT for table `recruitment_event`
--
ALTER TABLE `recruitment_event`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=107;

--
-- AUTO_INCREMENT for table `warning_correction`
--
ALTER TABLE `warning_correction`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

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
  ADD CONSTRAINT `FK_APPLICATION_STATUS_HISTORY_CHANGED_BY_ID` FOREIGN KEY (`changed_by_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `application_status_history_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`);

--
-- Constraints for table `candidate`
--
ALTER TABLE `candidate`
  ADD CONSTRAINT `candidate_ibfk_1` FOREIGN KEY (`id`) REFERENCES `users` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `candidate_skill`
--
ALTER TABLE `candidate_skill`
  ADD CONSTRAINT `candidate_skill_ibfk_1` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`);

--
-- Constraints for table `event_registration`
--
ALTER TABLE `event_registration`
  ADD CONSTRAINT `event_registration_ibfk_1` FOREIGN KEY (`event_id`) REFERENCES `recruitment_event` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `event_registration_ibfk_2` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`);

--
-- Constraints for table `event_review`
--
ALTER TABLE `event_review`
  ADD CONSTRAINT `fk_event_review_candidate` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`),
  ADD CONSTRAINT `fk_event_review_event` FOREIGN KEY (`event_id`) REFERENCES `recruitment_event` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `interview`
--
ALTER TABLE `interview`
  ADD CONSTRAINT `interview_ibfk_1` FOREIGN KEY (`application_id`) REFERENCES `job_application` (`id`),
  ADD CONSTRAINT `interview_ibfk_2` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`);

--
-- Constraints for table `interview_feedback`
--
ALTER TABLE `interview_feedback`
  ADD CONSTRAINT `interview_feedback_ibfk_1` FOREIGN KEY (`interview_id`) REFERENCES `interview` (`id`),
  ADD CONSTRAINT `interview_feedback_ibfk_2` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_application`
--
ALTER TABLE `job_application`
  ADD CONSTRAINT `job_application_ibfk_1` FOREIGN KEY (`offer_id`) REFERENCES `job_offer` (`id`) ON DELETE CASCADE,
  ADD CONSTRAINT `job_application_ibfk_2` FOREIGN KEY (`candidate_id`) REFERENCES `candidate` (`id`);

--
-- Constraints for table `job_offer`
--
ALTER TABLE `job_offer`
  ADD CONSTRAINT `job_offer_ibfk_1` FOREIGN KEY (`recruiter_id`) REFERENCES `recruiter` (`id`) ON DELETE CASCADE;

--
-- Constraints for table `job_offer_warning`
--
ALTER TABLE `job_offer_warning`
  ADD CONSTRAINT `fk_warn_admin` FOREIGN KEY (`admin_id`) REFERENCES `admin` (`id`),
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
