package com.nicopuhlmann.springai.simple_chat;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    @Value("classpath:/data/teilnahmebescheinigung.pdf")
    private Resource teilnahmebescheinigung;

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

    // Multimodalität: PDF als Bilder an das Vision-Modell senden
    // Mistral Medium 3.1+ unterstützt Vision (Bilder), daher werden PDF-Seiten gerendert
    @PostMapping("/pdf")
    public String analyzePdf(@RequestParam(value = "message", defaultValue = "Analysiere diese Teilnahmebescheinigung und fasse den Inhalt zusammen. Wer hat teilgenommen, an welcher Schulung, und wann?") String message) throws IOException {
        List<Resource> pageImages = renderPdfToImages(teilnahmebescheinigung);

        return chatClient.prompt()
                .system("Du bist ein hilfreicher Assistent, der Dokumente analysiert.")
                .user(u -> {
                    u.text(message);
                    for (Resource pageImage : pageImages) {
                        u.media(MimeTypeUtils.IMAGE_PNG, pageImage);
                    }
                })
                .call()
                .content();
    }

    private List<Resource> renderPdfToImages(Resource pdfResource) throws IOException {
        List<Resource> images = new ArrayList<>();
        byte[] pdfBytes = pdfResource.getInputStream().readAllBytes();
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = renderer.renderImageWithDPI(page, 150);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(image, "png", baos);
                images.add(new ByteArrayResource(baos.toByteArray()));
            }
        }
        return images;
    }
}
