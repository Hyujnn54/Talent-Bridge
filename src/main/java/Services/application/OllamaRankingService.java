package Services.application;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OllamaRankingService {

    // Use the same Groq cloud API that works for cover letter generation
    private static final String GROQ_API_KEY = "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";
    private static final String GROQ_URL     = "https://api.groq.com/openai/v1/chat/completions";

    private static final String[] MODELS = {
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768"
    };

    public record RankResult(int score, String rationale) {}

    public static RankResult rankApplication(
        String jobTitle,
        String jobDescription,
        List<String> offerSkills,
        String candidateName,
        String candidateExperience,
        String candidateEducation,
        List<String> candidateSkills,
        String coverLetter,
        String cvContent
    ) {
        String prompt = buildPrompt(jobTitle, jobDescription, offerSkills, candidateName,
            candidateExperience, candidateEducation, candidateSkills, coverLetter, cvContent);

        for (String model : MODELS) {
            try {
                System.out.println("[Ranking] Trying Groq model: " + model);
                String response = callGroq(model, prompt);
                if (response != null && !response.isEmpty()) {
                    RankResult parsed = parseResponse(response);
                    if (parsed != null) {
                        System.out.println("[Ranking] Success with model: " + model + " | Score: " + parsed.score());
                        return parsed;
                    }
                }
            } catch (Exception e) {
                System.err.println("[Ranking] Groq model " + model + " failed: " + e.getMessage());
            }
        }

        System.out.println("[Ranking] All Groq models failed — using heuristic fallback.");
        return heuristicRank(jobDescription, offerSkills, candidateSkills, coverLetter);
    }

    private static String buildPrompt(
        String jobTitle,
        String jobDescription,
        List<String> offerSkills,
        String candidateName,
        String candidateExperience,
        String candidateEducation,
        List<String> candidateSkills,
        String coverLetter,
        String cvContent
    ) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an HR assistant ranking candidates for a job.");
        prompt.append(" Return JSON only: {\"score\": 0-100, \"rationale\": \"short reason (1-2 sentences)\"}.\n\n");

        prompt.append("Job Title: ").append(jobTitle).append("\n");
        if (jobDescription != null && !jobDescription.isEmpty()) {
            prompt.append("Job Description: ").append(jobDescription).append("\n");
        }
        if (offerSkills != null && !offerSkills.isEmpty()) {
            prompt.append("Required Skills: ").append(String.join(", ", offerSkills)).append("\n");
        }

        prompt.append("\nCandidate: ").append(candidateName).append("\n");
        if (candidateEducation != null && !candidateEducation.isEmpty()) {
            prompt.append("Education: ").append(candidateEducation).append("\n");
        }
        if (candidateExperience != null && !candidateExperience.isEmpty()) {
            prompt.append("Experience: ").append(candidateExperience).append("\n");
        }
        if (candidateSkills != null && !candidateSkills.isEmpty()) {
            prompt.append("Skills: ").append(String.join(", ", candidateSkills)).append("\n");
        }
        if (coverLetter != null && !coverLetter.isEmpty()) {
            prompt.append("Cover Letter Summary: ").append(limit(coverLetter, 800)).append("\n");
        }
        if (cvContent != null && !cvContent.isEmpty()) {
            prompt.append("CV Excerpt: ").append(limit(cvContent, 1200)).append("\n");
        }

        return prompt.toString();
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
        systemMsg.put("content", "You score candidates for job fit. Return JSON only.");

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);

        JSONArray messages = new JSONArray();
        messages.put(systemMsg);
        messages.put(userMsg);

        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", 0.2);
        body.put("max_tokens", 256);
        body.put("stream", false);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorBody = new StringBuilder();
            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = errorReader.readLine()) != null) errorBody.append(line);
            }
            System.err.println("[Ranking] Groq HTTP " + responseCode + " for model " + model + ": " + errorBody);
            return null;
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }

        // Parse OpenAI-compatible response: choices[0].message.content
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

    private static RankResult parseResponse(String content) {
        try {
            if (content == null || content.isBlank()) return null;

            content = content.trim();

            // Strip markdown code fences if present
            content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();

            // Try direct JSON parse
            try {
                JSONObject parsed = new JSONObject(content);
                int score = clampScore(parsed.optInt("score", 0));
                String rationale = parsed.optString("rationale", "No rationale provided.");
                return new RankResult(score, trimRationale(rationale));
            } catch (Exception ignored) {}

            // Try to extract JSON object from mixed text
            Pattern jsonPattern = Pattern.compile("\\{[^}]*\"score\"[^}]*}");
            Matcher jsonMatcher = jsonPattern.matcher(content);
            if (jsonMatcher.find()) {
                try {
                    JSONObject parsed = new JSONObject(jsonMatcher.group());
                    int score = clampScore(parsed.optInt("score", 0));
                    String rationale = parsed.optString("rationale", "No rationale provided.");
                    return new RankResult(score, trimRationale(rationale));
                } catch (Exception ignored) {}
            }

            // Regex fallback
            int score = clampScore(extractScore(content));
            String rationale = extractRationale(content);
            return new RankResult(score, trimRationale(rationale));

        } catch (Exception e) {
            System.err.println("[Ranking] Failed to parse response: " + e.getMessage());
            return null;
        }
    }

    private static int extractScore(String content) {
        Pattern p = Pattern.compile("(?i)score\\s*[:=]\\s*(\\d{1,3})");
        Matcher m = p.matcher(content);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }

    private static String extractRationale(String content) {
        Pattern p = Pattern.compile("(?i)rationale\\s*[:=]\\s*(.*)");
        Matcher m = p.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }
        return content;
    }

    private static RankResult heuristicRank(String jobDescription, List<String> offerSkills,
                                           List<String> candidateSkills, String coverLetter) {
        int score = 40;
        List<String> matched = new ArrayList<>();

        String description = jobDescription != null ? jobDescription.toLowerCase(Locale.ROOT) : "";
        String cover = coverLetter != null ? coverLetter.toLowerCase(Locale.ROOT) : "";

        if (offerSkills != null && candidateSkills != null) {
            for (String req : offerSkills) {
                for (String cand : candidateSkills) {
                    if (req.equalsIgnoreCase(cand) || cand.toLowerCase(Locale.ROOT).contains(req.toLowerCase(Locale.ROOT))) {
                        matched.add(req);
                        score += 10;
                        break;
                    }
                }
            }
        }

        if (candidateSkills != null) {
            for (String skill : candidateSkills) {
                String s = skill.toLowerCase(Locale.ROOT);
                if (!description.isEmpty() && description.contains(s)) {
                    score += 5;
                } else if (!cover.isEmpty() && cover.contains(s)) {
                    score += 3;
                }
            }
        }

        score = clampScore(score);
        String rationale = matched.isEmpty()
            ? "Baseline fit based on profile; limited direct skill matches found."
            : "Matched skills: " + String.join(", ", matched) + ".";

        return new RankResult(score, rationale);
    }

    private static int clampScore(int score) {
        if (score < 0) return 0;
        if (score > 100) return 100;
        return score;
    }

    private static String trimRationale(String rationale) {
        if (rationale == null || rationale.isEmpty()) {
            return "No rationale provided.";
        }
        return rationale.length() > 240 ? rationale.substring(0, 240) + "..." : rationale;
    }

    private static String limit(String text, int max) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) + "..." : trimmed;
    }
}

