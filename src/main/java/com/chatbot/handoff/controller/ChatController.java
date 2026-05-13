package com.chatbot.handoff.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chatbot.handoff.model.ChatRequest;
import com.chatbot.handoff.model.ChatResponse;
import com.chatbot.handoff.service.AiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final AiService aiService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        String answer = aiService.generateAnswer(request.getMessage());
        return new ChatResponse(answer);
    }
}
