package Services.application;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

/**
 * Service for generating / translating cover letters using Groq API.
 * Used by the Applications module.
 */
public class GrokAIService {

    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";

    // Groq-hosted models, tried in order
    private static final String[] MODELS = {
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
    };

    public static String generateCoverLetter(String candidateName, String email, String phone,
                                             String jobTitle, String companyName, String experience,
                                             String education, List<String> skills, String cvContent) {
        String prompt = buildPrompt(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);

        for (String model : MODELS) {
            try {
                System.out.println("Trying Groq model: " + model);
                String result = callGroq(model, prompt);
                if (result != null && !result.isBlank()) {
                    System.out.println("Cover letter generated with Groq model: " + model);
                    return cleanContent(result);
                }
            } catch (Exception e) {
                System.err.println("Groq model " + model + " failed: " + e.getMessage());
            }
        }

        System.out.println("All Groq models failed – falling back to local generation.");
        return LocalCoverLetterService.generateCoverLetter(candidateName, email, phone, jobTitle,
                companyName, experience, education, skills, cvContent);
    }

    private static String buildPrompt(String candidateName, String email, String phone,
                                      String jobTitle, String companyName, String experience,
                                      String education, List<String> skills, String cvContent) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a professional cover letter writer. Write a compelling, professional cover letter (200-400 words) for the following candidate applying to a job. Output ONLY the cover letter text, nothing else.\n\n");

        prompt.append("CANDIDATE:\n");
        prompt.append("Name: ").append(candidateName).append("\n");
        prompt.append("Email: ").append(email).append("\n");
        prompt.append("Phone: ").append(phone).append("\n");
        prompt.append("Education: ").append(education).append("\n");
        prompt.append("Experience: ").append(experience).append("\n");

        if (skills != null && !skills.isEmpty()) {
            prompt.append("Skills: ").append(String.join(", ", skills)).append("\n");
        }

        if (cvContent != null && cvContent.length() > 50) {
            String cv = cvContent.length() > 2000 ? cvContent.substring(0, 2000) + "..." : cvContent;
            prompt.append("CV Summary: ").append(cv).append("\n");
        }

        prompt.append("\nJOB:\n");
        prompt.append("Position: ").append(jobTitle).append("\n");
        prompt.append("Company: ").append(companyName).append("\n");

        return prompt.toString();
    }

    /**
     * Generates an attractive recruitment event description using the Groq API.
     */
    public static String generateEventDescription(String title, String type, String location, String capacity) {
        String prompt = "Tu es un assistant RH expert en marketing de recrutement. "
                + "Rédige une description très attrayante et professionnelle de 3 ou 4 phrases pour un événement de recrutement. "
                + "L'événement s'appelle '" + title + "', c'est un événement de type '" + type + "'. "
                + "Il se déroulera ici : '" + location + "'. Capacité maximum : " + capacity + " personnes.\n"
                + "Sois enthousiaste, utilise un ton engageant, et encourage les candidats à s'inscrire. "
                + "Ne mets pas de salutations (comme 'Bonjour'), donne directement la description.";

        for (String model : MODELS) {
            try {
                String response = callGroq(model, prompt);
                if (response != null && !response.trim().isEmpty()) {
                    return cleanContent(response);
                }
            } catch (Exception e) {
                System.err.println("Groq AI API failed for event description with model " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    /**
     * Analyzes a recruitment event for a specific candidate profile,
     * returning personalized pros and cons based on their background.
     */
    public static String analyzeEventForCandidate(
            String eventTitle, String eventType, String eventDescription,
            String eventLocation, String eventDate,
            String candidateName, String educationLevel,
            Integer experienceYears, java.util.List<String> skills) {

        StringBuilder profileSb = new StringBuilder();
        profileSb.append("Profil du candidat :\n");
        profileSb.append("- Nom : ").append(candidateName != null ? candidateName : "Non renseigné").append("\n");
        profileSb.append("- Niveau d'études : ").append(educationLevel != null ? educationLevel : "Non renseigné").append("\n");
        profileSb.append("- Années d'expérience : ").append(experienceYears != null ? experienceYears : "Non renseigné").append("\n");
        if (skills != null && !skills.isEmpty()) {
            profileSb.append("- Compétences : ").append(String.join(", ", skills)).append("\n");
        }

        String prompt = "Tu es un conseiller en orientation professionnelle expert. "
                + "Analyse cet événement de recrutement pour ce candidat spécifique et donne une analyse PERSONNALISÉE.\n\n"
                + "Événement : " + eventTitle + "\n"
                + "Type : " + eventType + "\n"
                + "Description : " + (eventDescription != null ? eventDescription : "Non disponible") + "\n"
                + "Lieu : " + (eventLocation != null ? eventLocation : "Non disponible") + "\n"
                + "Date : " + (eventDate != null ? eventDate : "Non disponible") + "\n\n"
                + profileSb
                + "\nRéponds en français avec ce format EXACT (utilise ces emojis et titres) :\n"
                + "✅ POUR (3 arguments positifs sur pourquoi cet événement correspond bien à ce candidat)\n"
                + "• [avantage 1]\n"
                + "• [avantage 2]\n"
                + "• [avantage 3]\n\n"
                + "❌ CONTRE (2 points à considérer ou inconvénients)\n"
                + "• [inconvénient 1]\n"
                + "• [inconvénient 2]\n\n"
                + "💡 VERDICT (une courte phrase de conclusion)\n"
                + "[verdict]\n\n"
                + "Sois très précis et personnalisé, référence les compétences et l'expérience du candidat dans ta réponse.";

        for (String model : MODELS) {
            try {
                String response = callGroq(model, prompt);
                if (response != null && !response.trim().isEmpty()) {
                    return cleanContent(response);
                }
            } catch (Exception e) {
                System.err.println("Groq AI analysis failed with model " + model + ": " + e.getMessage());
            }
        }
        return null;
    }

    private static String callGroq(String model, String prompt) throws Exception {
        URL url = new URL(GROQ_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + GROQ_API_KEY);
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", "You are a professional assistant.");

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        JSONArray messages = new JSONArray();
        messages.put(systemMsg);
        messages.put(userMsg);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);
        body.put("stream", false);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            StringBuilder err = new StringBuilder();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) err.append(line);
            }
            System.err.println("Groq HTTP " + code + " for model " + model + ": " + err);
            return null;
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        JSONObject json = new JSONObject(response.toString());
        JSONArray choices = json.optJSONArray("choices");
        if (choices != null && choices.length() > 0) {
            JSONObject message = choices.getJSONObject(0).optJSONObject("message");
            if (message != null) {
                return message.optString("content", "");
            }
        }
        return null;
    }

    private static String cleanContent(String content) {
        if (content == null) return null;
        content = content.replaceAll("```[a-zA-Z]*\\n?", "").replaceAll("```", "").trim();
        return content.isEmpty() ? null : content;
    }

    // ── Lecto AI Translation (primary) + Groq fallback ─────────────────
    private static final String LECTO_API_KEY = "BQ9TKYZ-6KS4SDN-NB7AW6A-553W0JD";
    private static final String LECTO_API_URL = "https://api.lecto.ai/v1/translate/text";

    public static String translateCoverLetter(String coverLetter, String targetLanguage) {
        if (coverLetter == null || coverLetter.trim().isEmpty()) return coverLetter;

        String targetLangCode = switch (targetLanguage.toLowerCase()) {
            case "french"  -> "fr";
            case "arabic"  -> "ar";
            case "english" -> "en";
            case "spanish" -> "es";
            case "german"  -> "de";
            default        -> targetLanguage.toLowerCase().substring(0, Math.min(2, targetLanguage.length()));
        };

        // Detect source language from the cover letter text
        String sourceLangCode = detectLanguage(coverLetter);
        System.out.println("Detected source language: " + sourceLangCode + " → target: " + targetLangCode);

        // Don't translate if source and target are the same
        if (sourceLangCode.equals(targetLangCode)) {
            System.out.println("Source and target language are the same (" + sourceLangCode + "), skipping translation.");
            return coverLetter;
        }

        // 1) Try Lecto API first
        try {
            String result = callLectoAPI(coverLetter, sourceLangCode, targetLangCode);
            if (result != null && !result.isBlank()) {
                System.out.println("Translation via Lecto API successful (target: " + targetLangCode + ")");
                return result;
            }
        } catch (Exception e) {
            System.err.println("Lecto API translation failed: " + e.getMessage());
        }

        // 2) Fallback to Groq
        System.out.println("Lecto failed — falling back to Groq for translation");
        try {
            String result = translateWithGroq(coverLetter, targetLanguage);
            if (result != null && !result.isBlank()) {
                System.out.println("Translation via Groq fallback successful");
                return result;
            }
        } catch (Exception e) {
            System.err.println("Groq translation fallback also failed: " + e.getMessage());
        }

        System.err.println("All translation methods failed — returning original text.");
        return coverLetter;
    }

    /**
     * Detects the language of the given text using Groq AI.
     * Falls back to local keyword-based detection if Groq fails.
     */
    private static String detectLanguage(String text) {
        if (text == null || text.isBlank()) return "en";

        // 1) Try Groq AI detection
        try {
            String detected = detectLanguageWithGroq(text);
            if (detected != null && !detected.isBlank()) {
                System.out.println("Groq detected language: " + detected);
                return detected;
            }
        } catch (Exception e) {
            System.err.println("Groq language detection failed: " + e.getMessage());
        }

        // 2) Fallback to local keyword-based detection
        System.out.println("Falling back to local language detection");
        return detectLanguageLocal(text);
    }

    /**
     * Uses Groq AI to detect the language of the text.
     * Returns a 2-letter ISO language code (en, fr, ar, es, de, etc.)
     */
    private static String detectLanguageWithGroq(String text) {
        String sample = text.substring(0, Math.min(text.length(), 500));

        String prompt = "Detect the language of the following text. "
                + "Reply with ONLY the 2-letter ISO 639-1 language code (e.g. en, fr, ar, es, de). "
                + "Nothing else, just the 2-letter code.\n\n"
                + sample;

        for (String model : MODELS) {
            try {
                String result = callGroq(model, prompt);
                if (result != null && !result.isBlank()) {
                    // Clean the response: extract just the 2-letter code
                    String code = result.trim().toLowerCase().replaceAll("[^a-z]", "");
                    if (code.length() >= 2) {
                        code = code.substring(0, 2);
                        // Validate it's a known language code
                        if (isValidLangCode(code)) {
                            return code;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Groq detect language model " + model + " failed: " + e.getMessage());
            }
        }
        return null;
    }

    private static boolean isValidLangCode(String code) {
        return code.equals("en") || code.equals("fr") || code.equals("ar")
                || code.equals("es") || code.equals("de") || code.equals("it")
                || code.equals("pt") || code.equals("nl") || code.equals("ru")
                || code.equals("zh") || code.equals("ja") || code.equals("ko")
                || code.equals("tr") || code.equals("pl") || code.equals("sv");
    }

    /**
     * Local keyword-based language detection fallback.
     */
    private static String detectLanguageLocal(String text) {
        String sample = text.substring(0, Math.min(text.length(), 500));

        // Arabic: check for Arabic Unicode characters
        int arabicChars = 0;
        for (char c : sample.toCharArray()) {
            if (Character.UnicodeBlock.of(c) == Character.UnicodeBlock.ARABIC) {
                arabicChars++;
            }
        }
        if (arabicChars > sample.length() * 0.15) return "ar";

        String lower = sample.toLowerCase();

        // French indicators
        String[] frenchWords = {"je", "nous", "vous", "les", "des", "une", "est", "pour", "dans", "avec",
                "mon", "mes", "cette", "cher", "chère", "madame", "monsieur", "candidature", "poste",
                "entreprise", "compétences", "expérience", "cordialement", "veuillez"};
        int frenchHits = 0;
        for (String w : frenchWords) {
            if (lower.contains(" " + w + " ") || lower.startsWith(w + " ") || lower.contains(" " + w + ",")) {
                frenchHits++;
            }
        }
        if (frenchHits >= 4) return "fr";

        // Spanish indicators
        String[] spanishWords = {"para", "con", "una", "los", "las", "por", "como", "estimado", "empresa",
                "puesto", "experiencia", "habilidades", "atentamente"};
        int spanishHits = 0;
        for (String w : spanishWords) {
            if (lower.contains(" " + w + " ") || lower.startsWith(w + " ")) {
                spanishHits++;
            }
        }
        if (spanishHits >= 4) return "es";

        // German indicators
        String[] germanWords = {"ich", "und", "die", "der", "den", "für", "mit", "sehr", "geehrte",
                "bewerbung", "stelle", "unternehmen", "erfahrung", "freundlichen"};
        int germanHits = 0;
        for (String w : germanWords) {
            if (lower.contains(" " + w + " ") || lower.startsWith(w + " ")) {
                germanHits++;
            }
        }
        if (germanHits >= 4) return "de";

        // Default to English
        return "en";
    }

    private static String callLectoAPI(String text, String sourceLangCode, String targetLangCode) throws Exception {
        URL url = new URL(LECTO_API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("X-API-Key", LECTO_API_KEY);
        conn.setRequestProperty("Accept", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);

        JSONObject body = new JSONObject();
        JSONArray texts = new JSONArray();
        texts.put(text);
        body.put("texts", texts);
        body.put("to", new JSONArray().put(targetLangCode));
        body.put("from", sourceLangCode);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        if (code != 200) {
            StringBuilder err = new StringBuilder();
            java.io.InputStream errorStream = conn.getErrorStream();
            if (errorStream != null) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) err.append(line);
                }
            }
            throw new Exception("Lecto API HTTP " + code + ": " + err);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) response.append(line);
        }

        System.out.println("Lecto API response: " + response.substring(0, Math.min(300, response.length())));

        // Lecto response: { "translations": [ { "translated": ["..."] } ] }
        JSONObject json = new JSONObject(response.toString());
        JSONArray translations = json.optJSONArray("translations");
        if (translations != null && translations.length() > 0) {
            JSONObject first = translations.getJSONObject(0);
            JSONArray translated = first.optJSONArray("translated");
            if (translated != null && translated.length() > 0) {
                return translated.getString(0);
            }
        }
        return null;
    }

    /**
     * Fallback translation using Groq LLM when Lecto API is unavailable.
     */
    private static String translateWithGroq(String coverLetter, String targetLanguage) {
        String prompt = "Translate the following cover letter to " + targetLanguage + ".\n"
                + "Output ONLY the translated text, nothing else. No preamble, no explanation.\n\n"
                + coverLetter;

        for (String model : MODELS) {
            try {
                String result = callGroq(model, prompt);
                if (result != null && !result.isBlank()) {
                    System.out.println("Groq translation successful (" + model + ")");
                    return cleanContent(result);
                }
            } catch (Exception e) {
                System.err.println("Groq translation model " + model + " failed: " + e.getMessage());
            }
        }
        return null;
    }
}
