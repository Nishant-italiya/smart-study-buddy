package com.smartstudybuddy.Controllers;

import com.smartstudybuddy.DTO.QuizEvaluationRequest;
import com.smartstudybuddy.Services.AIService;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Autowired
    AIService aiService;

    @GetMapping("/summary/{subjectId}/{noteTitle}")
    public ResponseEntity<?> getNoteSummary(
            @PathVariable String subjectId,
            @PathVariable String noteTitle) {
            return aiService.getNoteSummary(subjectId, noteTitle);
    }

    @GetMapping("/generate-preparation-plan")
    public ResponseEntity<?> generatePlan() {
        return aiService.generateEfficientPreparationPlan();
    }

    @GetMapping("/generate-important-questions/{subjectId}")
    public ResponseEntity<?> generateImportantQuestions(@PathVariable String subjectId) {
        return  aiService.generateImportantQuestions(subjectId);
    }

    @PostMapping("/ask")
    public ResponseEntity<?> askAI(@RequestBody String question) {
        return aiService.askGeneralQuestion(question);
    }

    @PostMapping("/generate-quiz")
    public ResponseEntity<?> generateQuiz(@RequestParam String subjectId, @RequestParam String topic) {
        return aiService.generateQuiz(subjectId, topic);
    }

    @PostMapping("/evaluate-quiz")
    public ResponseEntity<?> evaluateQuiz(@RequestBody QuizEvaluationRequest request) {
        return aiService.evaluateQuiz(request);
    }

}
