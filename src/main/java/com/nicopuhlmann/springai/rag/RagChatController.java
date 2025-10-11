package com.nicopuhlmann.springai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RagChatController {

    private final ChatClient chatClient;
    private final TavilyTool tavilyTool;

    public RagChatController(ChatClient.Builder builder, VectorStore vectorStore, TavilyTool tavilyTool) {
        this.tavilyTool = tavilyTool;
        this.chatClient = builder
                .defaultAdvisors(new QuestionAnswerAdvisor(vectorStore))
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

}
