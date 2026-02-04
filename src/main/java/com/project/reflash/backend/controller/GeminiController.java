package com.project.reflash.backend.controller;

import com.project.reflash.backend.dto.Card;
import com.project.reflash.backend.response.ApiResponse;
import com.project.reflash.backend.response.ResponseMessage;
import com.project.reflash.backend.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class GeminiController {

    private final GeminiService geminiService;

    public GeminiController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    @PostMapping("/generate-flashcards")
    public ResponseEntity<ApiResponse> generateFlashcards() {
        List<Card> flashcards = geminiService.generateFlashcards();
        return ResponseEntity.ok(new ApiResponse(ResponseMessage.SUCCESS, flashcards));
    }
}
