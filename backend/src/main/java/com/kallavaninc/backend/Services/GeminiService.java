package com.kallavaninc.backend.Services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kallavaninc.backend.Entities.IncidentReport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    @Value("${groq.api.key}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public AiResult analyze(IncidentReport newReport, List<IncidentReport> recentReports) {
        System.out.println("\n🚀 [AI DEBUG] 1. Starting AI Analysis...");
        try {
            StringBuilder recent = new StringBuilder();
            for (IncidentReport r : recentReports) {
                recent.append(String.format("[ID: %d] %s - %s\n", r.getId(), r.getTitle(), r.getLocation()));
            }

            String prompt = String.format(
                    "Analyze this flood report. Title: '%s'. Desc: '%s'. Loc: '%s'. Recent reports: %s. ",
                    newReport.getTitle(), newReport.getDescription(), newReport.getLocation(),
                    recent.toString().isEmpty() ? "None" : recent.toString()
            );

            Map<String, Object> requestBodyMap = Map.of(
                    "model", "llama-3.1-8b-instant",
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are a disaster response AI. You MUST return ONLY a valid JSON object with " +
                                            "exactly these keys: \"summary\" (1 sentence summary), \"urgency\" (LOW, MEDIUM, HIGH, " +
                                            "or CRITICAL), and \"duplicateOfId\" (number ID of a recent report if exact match, otherwise null). " +
                                            "No markdown formatting."

                            ),
                            Map.of(
                                    "role", "user",
                                    "content", prompt
                            )
                    ),
                    "response_format", Map.of("type", "json_object")
            );

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);
            System.out.println("🚀 [AI DEBUG] 2. Payload Built. Sending to Groq...");

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.trim())
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("🚀 [AI DEBUG] 3. Groq responded with HTTP: " + response.statusCode());
            System.out.println("🚀 [AI DEBUG] 4. RAW BODY: " + response.body());

            if (response.statusCode() != 200) {
                throw new RuntimeException("HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String text = root.path("choices").get(0).path("message").path("content").asText()
                    .replace("```json", "").replace("```", "").trim();

            System.out.println("🚀 [AI DEBUG] 5. Extracted JSON text: " + text);

            JsonNode data = objectMapper.readTree(text);

            // Highly forgiving JSON parsing just in case Groq formats it weirdly
            String summary = data.has("summary") ? data.path("summary").asText() : "No summary generated.";
            String urgency = data.has("urgency") ? data.path("urgency").asText() : "MEDIUM";
            Long dupId = null;

            if (data.has("duplicateOfId") && !data.path("duplicateOfId").isNull()) {
                String dupString = data.path("duplicateOfId").asText();
                if (!dupString.equals("null") && !dupString.isEmpty()) {
                    try {
                        dupId = Long.parseLong(dupString);
                    } catch (NumberFormatException e) {
                        System.out.println("🚀 [AI DEBUG] - Duplicate ID was not a valid number, ignoring.");
                    }
                }
            }

            System.out.println("🚀 [AI DEBUG] 6. SUCCESS! Summary: " + summary + " | Urgency: " + urgency);
            return new AiResult(summary, urgency, dupId);

        } catch (Exception e) {
            System.err.println("\n❌ [AI ERROR CAPTURE] ❌");
            System.err.println("Type: " + e.getClass().getName());
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            System.err.println("❌ ====================== ❌\n");
            throw new RuntimeException("AI Failed");
        }
    }

    public record AiResult(String summary, String urgency, Long duplicateOfId) {}
}