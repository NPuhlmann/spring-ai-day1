package com.nicopuhlmann.springai.ragtoolsandagent;

import com.nicopuhlmann.springai.ragtoolsandagent.service.OrchestratorService;
import com.nicopuhlmann.springai.ragtoolsandagent.tools.TavilyTool;
import org.slf4j.Logger;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RagChatController {

    private final ChatClient chatClient;
    private final TavilyTool tavilyTool;
    private final OrchestratorService orchestratorService;

    public RagChatController(ChatClient.Builder builder, VectorStore vectorStore, TavilyTool tavilyTool, OrchestratorService orchestratorService) {
        this.tavilyTool = tavilyTool;
        this.orchestratorService = orchestratorService;
        this.chatClient = builder
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore).build())
                .defaultTools(this.tavilyTool)
                .defaultSystem("You are a helpful assistant. When a user asks about current events, news, or real-time information, ALWAYS use the available search tool to get the latest information. Do not ask for permission - just use the tool directly.")
                .build();

    }

    @PostMapping("/rag")
    public ChatClientResponse askInternalData(@RequestParam(value="message", defaultValue="Was ist der Attention Mechanismus?") String question){
        return chatClient.prompt().user(question).call().chatClientResponse();
    }

    @PostMapping("/tavily")
    public ChatClientResponse askExternalData(@RequestParam(value="message", defaultValue="Was ist der Attention Mechanismus?") String question){
        return chatClient.prompt().user(question).call().chatClientResponse();
    }

    @PostMapping("/mcp")
    public ChatClientResponse askMcpServer(@RequestParam(value = "message", defaultValue="welcher meiner Mitarbeiter kann mir bei Server Problemen helfen?") String question, ToolCallbackProvider mcpToolProvider){
        return chatClient.prompt().user(question).toolCallbacks(mcpToolProvider).call().chatClientResponse();
    }

    @PostMapping("/agent/chat")
    public String chatAgentic(@RequestParam(value = "question", defaultValue = "Was ist der Attention Mechanismus, und was haben die Autoren noch veröffentlicht?") String question){
        return orchestratorService.answer(question);
    }

}
