package com.nicopuhlmann.mcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class EmployeeService {

    private final WebClient webClient;

    public EmployeeService(
            @Value("${employee.service.api.key}") String apiKey,
            @Value("${employee.service.url}") String baseUrl
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-API-KEY", apiKey)
                .build();
    }

    @Tool(description = "Ruft Mitarbeiterdaten für eine bestimmte Mitarbeiter-ID ab. Verwende dieses Tool um Informationen über einen spezifischen Mitarbeiter aus dem HR-System zu bekommen.")
    public Map<String, Object> getEmployeeData(String employeeId) {
        try {
            return webClient.get()
                    .uri("/employees/{id}", employeeId)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            return Map.of("error", "Employee not found", "employeeId", employeeId);
        }
    }

    @Tool(description = "Ruft alle Mitarbeiterdaten ab. Verwende dieses Tool um eine Liste aller Mitarbeiter aus dem HR-System zu bekommen.")
    public List<Map<String, Object>> getAllEmployees() {
        try {
            return webClient.get()
                    .uri("/employees")
                    .retrieve()
                    .bodyToMono(List.class)
                    .block();
        } catch (Exception e) {
            return List.of(Map.of("error", "Could not retrieve employees"));
        }
    }
}
