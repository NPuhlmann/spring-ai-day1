package com.nicopuhlmann.springai.ragtoolsandagent.service;

import com.nicopuhlmann.springai.ragtoolsandagent.VectorStoreConfiguration;
import com.nicopuhlmann.springai.ragtoolsandagent.dto.OrchestratorResponse;
import com.nicopuhlmann.springai.ragtoolsandagent.dto.ReportPlan;
import com.nicopuhlmann.springai.ragtoolsandagent.tools.TavilyTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class OrchestratorService {

    private final ChatClient chatClientWithRAG;
    private final ChatClient chatClientWithoutRAG;
    private final ToolCallbackProvider mcpToolProvider;

    private static final Logger log = LoggerFactory.getLogger(OrchestratorService.class);

    private static final String ORCHESTRATOR_PROMPT = """
            Analysiere die folgende Anfrage und entscheide zunächst, ob ein ausführlicher Deep-Research Report nötig ist.

            ENTSCHEIDUNGSKRITERIEN für needsFullReport:
            - TRUE: Komplexe, vielschichtige Fragen die tiefe Analyse, Vergleiche, oder mehrere Perspektiven benötigen
            - TRUE: Fragen nach ausführlichen Berichten, Analysen, Studien oder Dokumentationen
            - TRUE: Themen die mehrere Kapitel, strukturierte Aufbereitung oder wissenschaftliche Tiefe erfordern
            - FALSE: Einfache Faktenfragen, schnelle Antworten, einzelne Informationen
            - FALSE: "Was ist...", "Wer ist...", "Wann...", "Wie viel..." Fragen
            - FALSE: Aktuelle News oder einzelne Datenpunkte

            Du hast Zugriff auf mehrere Worker:
            - RAG Worker mit Zugriff auf lokale Dokumente über LLM Techniken und wissenschaftliche Paper
            - Tavily Worker für den Zugriff auf das Internet, zum Suchen nach den neuesten Informationen
            - mcp Worker für den Zugriff auf Mitarbeiter Daten

            Erstelle einen Plan mit 2-3 Tasks:
            Zerlege die Frage des Nutzers in einzelne Aufgaben und beschreibe den Typ des Workers und eine speziell
            auf die Fähigkeiten dieses Workers zugeschnittene Aufgabenbeschreibung. Beschränke dich bei der Beschreibung
            der Aufgabe für den Worker auf eine einzige Fragestellung.

            Anfrage: %s
            """;
    private static final String WORKER_PROMPT = """
            Generiere eine Antwort basierend auf folgendem:
            Task: %s
            Style: %s
            Anweisungen: %s
            """;

    private static final String REPORT_PLANNER_PROMPT = """
            Rolle:
            Du bist ein Deep Research Agent, der auf Basis von bereits gesammelten Daten und einer spezifischen User-Anfrage einen detaillierten, mehrkapiteligen Report plant. Dein Ziel ist es, eine logische, informative und nutzerorientierte Struktur zu erstellen, die sowohl Tiefe als auch Relevanz gewährleistet.
            Aufgabe:

            Analysiere die User-Anfrage und die bereits gesammelten Daten, um das zentrale Thema, die Zielgruppe und den Zweck des Reports zu verstehen.
            Identifiziere die Kernfragen, die der Report beantworten soll, und leite daraus eine sinnvolle Kapitelstruktur ab.
            Berücksichtige folgende Aspekte für die Planung:

            Zielgruppe: Wer wird den Report lesen? (z. B. Fachleute, Laien, Entscheidungsträger)
            Zweck: Soll der Report informieren, überzeugen, analysieren oder Handlungsempfehlungen geben?
            Datenlage: Welche Daten, Quellen und Erkenntnisse liegen bereits vor? Wo gibt es Lücken, die im Report adressiert werden müssen?
            Struktur: Der Report soll mindestens 3–5 Kapitel umfassen, die logisch aufeinander aufbauen.

            Schritte zur Erstellung des Report-Plans:

            Einleitung:

            Kurze Zusammenfassung des Themas und der Relevanz.
            Zielsetzung des Reports und zentrale Forschungsfragen.
            Methodik: Wie wurden die Daten gesammelt und analysiert?

            Hauptkapitel:

            Kapitel 1: Hintergrund und Kontext
            (Hintergrundinformationen, Definitionen, historische Entwicklung, aktuelle Relevanz)
            Kapitel 2: Analyse der gesammelten Daten
            (Strukturierte Darstellung der Daten, Visualisierungen, erste Erkenntnisse)
            Kapitel 3: Vertiefende Untersuchung
            (Thematische Schwerpunkte, Vergleich mit bestehenden Studien, kritische Diskussion)
            Kapitel 4: Fallbeispiele oder Anwendungsfälle (falls relevant)
            (Praktische Beispiele, Case Studies, Lessons Learned)
            Kapitel 5: Diskussion und Implikationen
            (Interpretation der Ergebnisse, mögliche Konsequenzen, offene Fragen)

            Fazit und Ausblick:

            Zusammenfassung der wichtigsten Erkenntnisse.
            Handlungsempfehlungen oder weiterführende Forschungsfragen.
            Limitationen der Analyse und Hinweise auf zukünftige Forschung.

            Anhang (optional):

            Glossar, Datenquellen, technische Details, weiterführende Literatur.

            Anforderungen an den Plan:

            Jedes Kapitel soll eine klare These oder Zielsetzung haben.
            Die Struktur soll modular sein, sodass Kapitel bei Bedarf vertieft oder gekürzt werden können.
            Berücksichtige interaktive Elemente (z. B. Fragen an den Nutzer, Verweise auf weitere Ressourcen), falls der Report digital genutzt wird.
            Halte den Plan flexibel, um spätere Anpassungen basierend auf neuen Daten oder Nutzerfeedback zu ermöglichen.

            Originale User Frage: %s

            Informationen: %s

            """;

    private static final String WRITER_WORKER_PROMPT = """
            Schreibe ein detailliertes Kapitel für einen Report basierend auf den folgenden Informationen:

            Kapitel-Titel: %s
            Ziel des Kapitels: %s
            Beschreibung: %s

            Verfügbare Ressourcen/Daten: %s

            Anforderungen:
            - Schreibe einen fließenden, gut strukturierten Text
            - Nutze die verfügbaren Ressourcen und Daten als Basis
            - Halte dich an das Ziel und die Beschreibung des Kapitels
            - Verwende eine professionelle, aber verständliche Sprache
            - Strukturiere das Kapitel mit Unterüberschriften, falls sinnvoll
            """;



    public OrchestratorService(ChatClient.Builder builder, VectorStore vectorStore, TavilyTool tavilyTool, ToolCallbackProvider mcpToolProvider) {
        this.mcpToolProvider = mcpToolProvider;

        // ChatClient mit RAG und Tools für Worker Tasks
        this.chatClientWithRAG = builder
            .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
            .defaultTools(tavilyTool)
            .defaultToolCallbacks(mcpToolProvider.getToolCallbacks())
            .build();

        // ChatClient ohne RAG für Orchestrator, Report Planning und Writing
        this.chatClientWithoutRAG = builder.build();

        log.info("OrchestratorService initialisiert mit 2 ChatClients (mit/ohne RAG)");
    }

    @PostConstruct
    public void validateMcpTools() {
        log.info("╔════════════════════════════════════════════════════════════════════════════════╗");
        log.info("║  MCP TOOL VALIDATION                                                          ║");
        log.info("╚════════════════════════════════════════════════════════════════════════════════╝");

        if (mcpToolProvider == null) {
            log.error("❌ ToolCallbackProvider ist NULL! MCP Server ist NICHT verbunden!");
            return;
        }

        var toolCallbacks = mcpToolProvider.getToolCallbacks();

        if (toolCallbacks == null || toolCallbacks.length == 0) {
            log.warn("⚠️  Keine MCP Tools gefunden! MCP Server könnte nicht erreichbar sein.");
            log.warn("    Überprüfe: http://localhost:8081");
            log.warn("    application.properties: spring.ai.mcp.client.sse.connections.server1.url");
        } else {
            log.info("✅ MCP Tools erfolgreich geladen: {} Tools gefunden", toolCallbacks.length);
            for (int i = 0; i < toolCallbacks.length; i++) {
                var tool = toolCallbacks[i];
                log.info("   {}. Tool: {}", (i + 1), tool.getToolDefinition());
            }
        }

        log.info("════════════════════════════════════════════════════════════════════════════════\n");
    }


    private OrchestratorResponse analyzeWithOrchestrator(String question){
        log.info("=== ORCHESTRATOR ANALYSE START ===");
        log.info("Input Frage: {}", question);

        String orchestratorPrompt = String.format(ORCHESTRATOR_PROMPT, question);
        log.debug("Orchestrator Prompt erstellt: {}", orchestratorPrompt);

        log.info("Rufe ChatClient (OHNE RAG) auf für Orchestrator-Analyse...");
        OrchestratorResponse response = this.chatClientWithoutRAG.prompt().user(orchestratorPrompt).call().entity(OrchestratorResponse.class);

        if (response == null){
            log.error("Orchestrator konnte keine Antwort generieren!");
            throw new RuntimeException("Orchestrator konnte keine Antwort generieren");
        }
        log.info("Orchestrator Response erhalten:");
        log.info("  • Needs Full Report: {}", response.needsFullReport());
        log.info("  • Tasks generiert: {}", response.tasks().size());
        log.info("Geplante Tasks:");
        response.tasks().forEach(task ->
            log.info("  • Task [{}]: {}", task.type(), task.description())
        );
        log.debug("Orchestrator Details: {}", response);
        log.info("=== ORCHESTRATOR ANALYSE ENDE ===\n");

        return response;
    }

    private String executeWorkers(OrchestratorResponse orchestratorResponse, String question){
        log.info("=== WORKER EXECUTION START ===");
        log.info("Führe {} Tasks PARALLEL aus", orchestratorResponse.tasks().size());

        List<CompletableFuture<String>> workerFutures = orchestratorResponse.tasks().stream().map(task ->
            CompletableFuture.supplyAsync(() -> {
                log.info("--- Worker Task {} START ---", task.type());

                String prompt = String.format(WORKER_PROMPT, question, task.type(), task.description());
                log.debug("Worker Prompt: {}", prompt);

                log.info("Rufe ChatClient (MIT RAG & TOOLS) auf für Worker Task '{}'...", task.type());
                String result = this.chatClientWithRAG.prompt().user(prompt).call().content();

                log.info("Worker Task '{}' abgeschlossen. Ergebnis-Länge: {} Zeichen", task.type(), result.length());
                log.debug("Worker Result: {}", result);
                log.info("--- Worker Task {} ENDE ---\n", task.type());

                return result;
            })
        ).toList();

        // Warte auf alle Worker
        CompletableFuture.allOf(workerFutures.toArray(new CompletableFuture[0])).join();

        List<String> workerResults = workerFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        String combinedResults = String.join("\n\n NÄCHSTE QUELLE: \n\n", workerResults);
        log.info("Alle Worker Tasks abgeschlossen. Kombinierte Daten-Länge: {} Zeichen", combinedResults.length());
        log.info("=== WORKER EXECUTION ENDE ===\n");

        return combinedResults;
    }

    private ReportPlan createReportPlan(String data, String question){
        log.info("=== REPORT PLANNING START ===");
        log.info("Input Frage: {}", question);
        log.info("Verfügbare Daten-Länge: {} Zeichen", data.length());

        String prompt = String.format(REPORT_PLANNER_PROMPT, question, data);
        log.debug("Report Planner Prompt erstellt");

        log.info("Rufe ChatClient (OHNE RAG) auf für Report-Planung...");
        ReportPlan plan = this.chatClientWithoutRAG.prompt().user(prompt).call().entity(ReportPlan.class);

        if (plan == null){
            log.error("Report Planner konnte keinen Plan generieren!");
            throw new RuntimeException("Report Planner konnte keinen Plan generieren");
        }

        log.info("Report Plan erhalten: '{}'", plan.reportTitle());
        log.info("Anzahl Kapitel: {}", plan.kapitel().size());
        plan.kapitel().forEach(kapitel -> log.debug("  - Kapitel: {}", kapitel.title()));
        log.info("=== REPORT PLANNING ENDE ===\n");

        return plan;
    }

    private String writerWorker(ReportPlan plan){
        log.info("=== REPORT WRITING START ===");
        log.info("Report Titel: '{}'", plan.reportTitle());
        log.info("Schreibe {} Kapitel PARALLEL", plan.kapitel().size());

        List<CompletableFuture<String>> kapitelFutures = plan.kapitel().stream().map(kapitel ->
            CompletableFuture.supplyAsync(() -> {
                log.info("--- Kapitel Writing START ---");
                log.info("Kapitel: '{}'", kapitel.title());
                log.info("Ziel: {}", kapitel.goal());
                log.info("Ressourcen-Länge: {} Zeichen", kapitel.resource() != null ? kapitel.resource().length() : 0);

                String prompt = String.format(WRITER_WORKER_PROMPT,
                    kapitel.title(),
                    kapitel.goal(),
                    kapitel.description(),
                    kapitel.resource());

                log.debug("Writer Worker Prompt erstellt für Kapitel '{}'", kapitel.title());
                log.info("Rufe ChatClient (OHNE RAG) auf für Kapitel '{}'...", kapitel.title());
                String kapitelInhalt = this.chatClientWithoutRAG.prompt().user(prompt).call().content();

                log.info("Kapitel '{}' abgeschlossen. Länge: {} Zeichen", kapitel.title(), kapitelInhalt.length());
                log.debug("Kapitel Inhalt: {}", kapitelInhalt);
                log.info("--- Kapitel Writing ENDE ---\n");

                return "## " + kapitel.title() + "\n\n" + kapitelInhalt;
            })
        ).toList();

        // Warte auf alle Kapitel
        CompletableFuture.allOf(kapitelFutures.toArray(new CompletableFuture[0])).join();

        List<String> kapitelTexte = kapitelFutures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());

        String report = "# " + plan.reportTitle() + "\n\n" +
                        String.join("\n\n", kapitelTexte);

        log.info("Report fertiggestellt. Gesamtlänge: {} Zeichen", report.length());
        log.info("=== REPORT WRITING ENDE ===\n");

        return report;
    }



    public String answer(String question){
        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║  DEEP RESEARCH WORKFLOW START                                                ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.info("User Frage: {}\n", question);

        // Phase 1: Orchestrator Analyse
        log.info(">>> PHASE 1: Orchestrator Analyse");
        OrchestratorResponse orchestratorResponse = analyzeWithOrchestrator(question);

        log.info("Orchestrator Entscheidung: needsFullReport = {}", orchestratorResponse.needsFullReport());

        // Phase 2: Worker Execution
        log.info(">>> PHASE 2: Worker Execution - Datensammlung");
    String combinedData = executeWorkers(orchestratorResponse, question);

        // GATE: Entscheidung ob Full Report oder Quick Answer
        if (!orchestratorResponse.needsFullReport()) {
            // Quick Answer Mode: Gib kombinierte Worker-Ergebnisse direkt zurück
            log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
            log.info("║  WORKFLOW ABGESCHLOSSEN (Quick Answer Mode)                                  ║");
            log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
            log.info("Antwort-Länge: {} Zeichen\n", combinedData.length());
            return combinedData;
        }

        // Full Report Mode: Fortfahren mit Report Planning und Writing
        log.info(">>> Starte Full Report Mode (Phasen 3 & 4)");

        // Phase 3: Report Planning
        log.info(">>> PHASE 3: Report Planning - Struktur erstellen");
        ReportPlan reportPlan = createReportPlan(combinedData, question);

        // Phase 4: Report Writing
        log.info(">>> PHASE 4: Report Writing - Finaler Report");
        String report = writerWorker(reportPlan);

        log.info("╔═══════════════════════════════════════════════════════════════════════════════╗");
        log.info("║  DEEP RESEARCH WORKFLOW ABGESCHLOSSEN (Full Report Mode)                     ║");
        log.info("╚═══════════════════════════════════════════════════════════════════════════════╝");
        log.info("Finaler Report-Länge: {} Zeichen\n", report.length());

        return report;
    }
}
