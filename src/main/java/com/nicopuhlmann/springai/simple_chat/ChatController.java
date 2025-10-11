package com.nicopuhlmann.springai.simple_chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @PostMapping("/simple_chat")
    public String chat(String message){
        return chatClient.prompt()
                .system("You are a helpful Assistent")
                .user(message)
                .call() // Das ist ein Blocking Call
                .content(); // Gib mir nur den Content (also die Antwort)
    }

    @PostMapping("/stream")
    public Flux<String> stream(String message){
        return chatClient.prompt().system("You are a helpful Assistent").user(message).stream().content();
    }

}
