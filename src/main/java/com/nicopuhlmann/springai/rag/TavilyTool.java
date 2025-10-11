package com.nicopuhlmann.springai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TavilyTool {
    private static final Logger log = LoggerFactory.getLogger(TavilyTool.class);

    private static final String BASE_URL = "https://api.tavily.com";
    private final RestClient restClient;
    private final String tavilyApiKey;

    // --- DTOs für die Tavily API-Antwort ---
    // Ein Record, der ein einzelnes Suchergebnis darstellt
    public record TavilyResult(String title, String url, String content, double score) {}

    // Ein Record, der die gesamte Antwort von Tavily darstellt
    public record TavilyResponse(String query, List<TavilyResult> results) {}


    public TavilyTool(RestClient.Builder builder){
        // 1. API-Schlüssel in einer Variable speichern
        this.tavilyApiKey = System.getenv("TAVILY_API_KEY");
        if (this.tavilyApiKey == null || this.tavilyApiKey.isEmpty()) {
            throw new IllegalStateException("Umgebungsvariable TAVILY_API_KEY ist nicht gesetzt.");
        }

        // 2. Den RestClient ohne den Authorization-Header bauen
        this.restClient = builder
                .baseUrl(BASE_URL)
                .defaultHeader("Content-Type", "application/json") // Wichtig für POST-Requests mit JSON
                .build();
    }

    @Tool(description = "Search the web for current, real-time information. Use this for questions about current events, news, recent developments, or any information that might have changed recently. ALWAYS call this function when asked about current events or real-time information.")
    public String searchTavily(String query){
        log.info("Führe Tavily-Suche für die Anfrage durch: '{}'", query);

        Map<String, Object> requestBody = Map.of(
                "api_key", this.tavilyApiKey,
                "query", query,
                "max_results", 5
        );

        // 1. JSON-Antwort direkt in unser TavilyResponse-Objekt umwandeln
        TavilyResponse response = restClient.post()
                .uri("/search")
                .body(requestBody)
                .retrieve()
                .body(TavilyResponse.class);

        if (response == null || response.results() == null) {
            return "Keine Ergebnisse gefunden.";
        }

        // 2. Die Ergebnisse zu einem formatierten String für das KI-Modell zusammenfassen
        return response.results().stream()
                .map(result -> "Titel: " + result.title() + "\nInhalt: " + result.content())
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}