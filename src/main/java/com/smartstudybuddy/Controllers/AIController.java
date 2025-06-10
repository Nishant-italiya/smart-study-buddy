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
    public ResponseEntity<String> getNoteSummary(
            @PathVariable String subjectId,
            @PathVariable String noteTitle) {
        String summary = aiService.getNoteSummary(subjectId, noteTitle);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/generate-preparation-plan")
    public ResponseEntity<String> generatePlan() {
        String plan = aiService.generateEfficientPreparationPlan();
        return ResponseEntity.ok(plan);
    }

    @GetMapping("/generate-important-questions/{subjectId}")
    public ResponseEntity<String> generateImportantQuestions(@PathVariable String subjectId) {
        String questions = aiService.generateImportantQuestions(subjectId);
        return ResponseEntity.ok(questions);
    }

    @PostMapping("/ask")
    public ResponseEntity<String> askAI(@RequestBody String question) {
        String answer = aiService.askGeneralQuestion(question);
        return ResponseEntity.ok(answer);
    }

    // Step 1: Generate a quiz
    @PostMapping("/generate-quiz")
    public ResponseEntity<String> generateQuiz(@RequestParam String subjectId, @RequestParam String topic) {
        String quiz = aiService.generateQuiz(subjectId, topic);
        return ResponseEntity.ok(quiz);
    }

    @PostMapping("/evaluate-quiz")
    public ResponseEntity<String> evaluateQuiz(@RequestBody QuizEvaluationRequest request) {
        String result = aiService.evaluateQuiz(request);
        return ResponseEntity.ok(result);
    }

}
