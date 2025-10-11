package com.nicopuhlmann.springaiday1.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    public String chat(String message){
        return chatClient.prompt()
                .system("You are a helpful Assistent")
                .user(message)
                .call() // Das ist ein Blocking Call
                .content(); // Gib mir nur den Content (also die Antwort)
    }

    @GetMapping("/stream")
    public Flux<String> stream(String message){
        return chatClient.prompt().user(message).stream().content();
    }
}
