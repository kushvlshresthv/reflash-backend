package com.project.reflash.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.project.reflash.backend.dto.AIFlashCardGenerationRequestDto;
import com.project.reflash.backend.dto.Card;
import com.project.reflash.backend.exception.GeminiApiRateLimitExceededException;
import com.project.reflash.backend.exception.GeminiResponseParseFailureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private static final String MODEL_NAME = "gemini-2.5-flash";
    private static final int MAX_RETRIES = 3;
    // Sample text about 10-11 lines for generating flashcards
    private static final String SAMPLE_TEXT = """
            The human brain is an incredibly complex organ that serves as the command center for the human nervous system.
            It weighs about 3 pounds and contains approximately 86 billion neurons.
            These neurons communicate through electrical and chemical signals, forming intricate networks.
            The brain is divided into several regions, each responsible for different functions.
            The cerebrum is the largest part and handles thinking, learning, and emotions.
            The cerebellum coordinates movement and balance.
            The brainstem controls vital functions like breathing and heart rate.
            Memory is stored across different parts of the brain, not in one single location.
            Short-term memory is processed in the prefrontal cortex, while long-term memories involve the hippocampus.
            The brain uses about 20% of the body's total energy despite being only 2% of body weight.
            Neuroplasticity allows the brain to reorganize itself by forming new neural connections throughout life.
            """;
    private final ObjectMapper objectMapper;
    @Value("${gemini.api.key}")
    private String apiKey;

    public GeminiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Card> generateFlashcards(AIFlashCardGenerationRequestDto requestDetails) {
        //TODO: validate the requestDetails object
        String finalPrompt = buildPrompt(requestDetails.getText(), requestDetails.getPrompt(), requestDetails.getCount());

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try (Client client = Client.builder().apiKey(apiKey).build()) {
                log.info("Attempting to generate flashcards (attempt {}/{})", attempt, MAX_RETRIES);

                GenerateContentResponse response = client.models.generateContent(
                        MODEL_NAME,
                        finalPrompt,
                        null
                );

                String responseText = response.text();
                log.info("Successfully received response from Gemini API");
                return parseResponse(responseText);
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                if (e instanceof GeminiResponseParseFailureException) {
                    //retry if the response could not be parsed properly
                    continue;
                }
                if (errorMessage != null && errorMessage.contains("429")) {
                    log.warn("Rate limit hit on attempt {}. Waiting before retry...", attempt);
                } else {
                    // Non-rate-limit error, don't retry
                    throw new RuntimeException("Failed to generate flashcards: " + e.getMessage(), e);
                }
            }
        }

        // All retries exhausted
        log.error("All retry attempts exhausted");
        throw new GeminiApiRateLimitExceededException(
                "Gemini API rate limit exceeded. Please try again later or check your API quota at https://ai.google.dev/gemini-api/docs/rate-limits"
        );
    }

    private String buildPrompt(String text, String prompt, int count) {
        if(prompt != null && !prompt.isBlank()) {
            return (prompt + text);
        }
        return """
                Based on the following text, generate flashcards in JSON format.
                Each flashcard should have:
                - front: A question or key term
                
                - back: The answer or definition. Keep the length of the back face variable. Some should be long, some should be one or two sentences. Dont make everything the same length. 
                
                - additionalContext: Extra helpful information based on the text itself, keep this information descriptive (add information from your side as well, but the information must be correct)
                
                Return ONLY a valid JSON array, no markdown, no explanation. Example format:
                [{"front": "question", "back": "answer", "additionalContext": "extra info"}]
                
                The total number of cards to create is: """ + count + """
                
                NOTE: the flashcard generate should be from the text only. Don't include information that are outside the text.
                
                If the count is too much compared to the content, you can also generate lesser cards, but don't generate cards that are outside the text please.
                
                
                Text to create flashcards from:
                """ + text;
    }

    private List<Card> parseResponse(String responseText) {
        try {
            // Clean up the response - remove markdown code blocks if present
            String text = responseText.replace("```json", "").replace("```", "").trim();

            return objectMapper.readValue(text, new TypeReference<>() {
            });
        } catch (Exception e) {
            throw new GeminiResponseParseFailureException("Failed to parse Gemini response: " + e.getMessage());
        }
    }
}
