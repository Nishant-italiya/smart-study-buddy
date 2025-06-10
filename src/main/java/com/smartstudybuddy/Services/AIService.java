package com.smartstudybuddy.Services;

import com.smartstudybuddy.DTO.QuizEvaluationRequest;
import com.smartstudybuddy.Models.Exam;
import com.smartstudybuddy.Models.Note;
import com.smartstudybuddy.Models.QuizReport;
import com.smartstudybuddy.Models.User;
import com.smartstudybuddy.Repositories.ExamRepository;
import com.smartstudybuddy.Repositories.NotesRepository;
import com.smartstudybuddy.Repositories.QuizReportRepository;
import com.smartstudybuddy.Repositories.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AIService {
     @Autowired
     private UserRepository userRepository;
     @Autowired
     private NotesRepository notesRepo;
     @Autowired
     private ExamRepository examRepository;
     @Autowired
     private QuizReportRepository quizReportRepository;
     @Autowired
     private ChatClient chatClient;

    public String getNoteSummary(String subjectId, String noteTitle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated");
        }

        String username = auth.getName();

        // Check if student is registered for the subject
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can access this feature.");
        }

        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            throw new AccessDeniedException("You are not registered for this subject.");
        }

        // Fetch the note
        Note note = notesRepo.findBySubjectIdAndTitle(subjectId, noteTitle)
                .orElseThrow(() -> new NoSuchElementException("Note not found"));

        // Convert file data to string
        String noteContent = "";
        try {
            PDDocument document = PDDocument.load(note.getData());
            PDFTextStripper stripper = new PDFTextStripper();
            noteContent = stripper.getText(document);
            document.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract text from PDF", e);
        }

        // Prepare the prompt
        String promptMessage = "Please summarize the following note:\n\n" + noteContent;
        // Send prompt to AI and return the summary
        try {
            String summary = chatClient.call(new Prompt(promptMessage))
                    .getResult()
                    .getOutput()
                    .getContent();
            return summary;
        } catch (Exception e) {
            e.printStackTrace();  // or log error
            throw new RuntimeException("AI call failed: " + e.getMessage());
        }
    }

    public String generateEfficientPreparationPlan() {
        // Get current authenticated student username
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }
        String username = auth.getName();

        // Check if student is registered for the subject
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can access this feature.");
        }


        // Get all registered subject IDs for the student
        List<String> subjectIds = user.getRegisteredSubjectIds();

        if (subjectIds.isEmpty()) {
            return "No registered subjects found.";
        }

        // Get current date
        LocalDate today = LocalDate.now();

        // Find upcoming exams for these subjects
        List<Exam> upcomingExams = examRepository.findBySubjectIdInAndExamDateAfter(subjectIds, today);

        if (upcomingExams.isEmpty()) {
            return "No upcoming exams found for your registered subjects.";
        }

        // Build prompt for AI using exam details & syllabus
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("You are an expert study planner. Create an efficient preparation plan for these upcoming exams:\n\n");

        for (Exam exam : upcomingExams) {
            promptBuilder.append("Subject ID: ").append(exam.getSubjectId()).append("\n");
            promptBuilder.append("Exam Date: ").append(exam.getExamDate()).append("\n");
            promptBuilder.append("Exam Type: ").append(exam.getExamType()).append("\n");
            promptBuilder.append("Syllabus: ").append(exam.getSyllabus()).append("\n\n");
        }

        String prompt = promptBuilder.toString();

        // Call AI service
        return chatClient.call(new Prompt(prompt))
                .getResult()
                .getOutput()
                .getContent();
    }

    public String generateImportantQuestions(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }
        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can access this feature.");
        }

        List<String> registeredSubjectIds = user.getRegisteredSubjectIds();
        if (!registeredSubjectIds.contains(subjectId)) {
            throw new AccessDeniedException("Student is not registered for this subject.");
        }

        LocalDate today = LocalDate.now();
        Exam exam = examRepository.findBySubjectIdAndExamDateAfter(subjectId, today)
                .orElseThrow(() -> new IllegalArgumentException("No upcoming exam found for this subject."));

        String syllabus = exam.getSyllabus();
        List<Note> notes = notesRepo.findBySubjectId(subjectId);

        // If no notes found, fallback to syllabus-based question generation
        if (notes.isEmpty()) {
            String prompt = "The syllabus for an upcoming exam is:\n" + syllabus +
                    "\n\nGenerate important questions with answers based only on this syllabus.";

            String aiResponse = chatClient.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getContent();

            return aiResponse != null && !aiResponse.isBlank()
                    ? aiResponse
                    : "AI could not generate questions from the syllabus.";
        }

        StringBuilder combinedResponses = new StringBuilder();
        int noteCount = 1;

        for (Note note : notes) {
            try (PDDocument document = PDDocument.load(note.getData())) {
                PDFTextStripper stripper = new PDFTextStripper();
                String noteText = stripper.getText(document);

                String prompt = "You are a smart exam assistant.\n" +
                        "Given the following syllabus:\n" + syllabus +
                        "\n\nAnd this note content:\n" + noteText +
                        "\n\nExtract the most important exam questions with answers based on this note and the syllabus. Only include questions relevant to the syllabus topics.";
//                System.out.println(prompt);
                String response = chatClient.call(new Prompt(prompt))
                        .getResult()
                        .getOutput()
                        .getContent();

                if (response != null && !response.isBlank()) {
                    combinedResponses.append("Note ").append(noteCount++).append(":\n")
                            .append(response).append("\n\n");
                }

            } catch (IOException e) {
                // Skip unreadable notes
            }
        }

        if (combinedResponses.isEmpty()) {
            // If all notes failed to extract content, fallback to syllabus
            String prompt = "The syllabus for an upcoming exam is:\n" + syllabus +
                    "\n\nGenerate important questions with answers based only on this syllabus.";

            String aiResponse = chatClient.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getContent();

            return aiResponse != null && !aiResponse.isBlank()
                    ? aiResponse
                    : "AI could not generate questions from the syllabus.";
        }

        return combinedResponses.toString();
    }

    public String askGeneralQuestion(String question) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        // Optionally log who asked what
        String role = user.getRole();  // "STUDENT" or "PROFESSOR"

        // Send question to AI
        String aiResponse = chatClient.call(new Prompt(question))
                .getResult()
                .getOutput()
                .getContent();

        return aiResponse != null && !aiResponse.isBlank()
                ? aiResponse
                : "AI was not able to answer your question.";
    }

    public String generateQuiz(String subjectId, String topic) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can access this feature.");
        }

        String prompt;

        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            prompt = "Generate a 10-question multiple-choice quiz for the topic: " + topic + " in the subject. and dont give me answer only generate questions";
        }
        else{
            Optional<Note> optionalNote = notesRepo.findBySubjectIdAndTitle(subjectId, topic);

            if (optionalNote.isPresent()) {
                Note note = optionalNote.get();
                StringBuilder content = new StringBuilder();

                try (PDDocument doc = PDDocument.load(note.getData())) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    content.append(stripper.getText(doc));
                } catch (IOException e) {
                    // fallback to topic only
                    content.setLength(0);
                }

                if (!content.isEmpty()) {
                    prompt = "Based on the following notes content,generate only question dont give me answer, generate a 10-question multiple-choice quiz on the topic: "
                            + topic + "\n\nNotes:\n" + content;
                } else {
                    prompt = "Generate a 10-question multiple-choice quiz for the topic: " + topic + " in the subject.and dont give me answer only generate questions";
                }
            } else {
                // No note found, use topic only
                prompt = "Generate a 10-question multiple-choice quiz for the topic: " + topic + " in the subject.and dont give me answer only generate questions";
            }

        }
        return chatClient.call(new Prompt(prompt))
                .getResult()
                .getOutput()
                .getContent();
    }

    public String evaluateQuiz(QuizEvaluationRequest request) {
        List<String> questions = request.getQuestions();
        List<String> answers = request.getAnswers();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can evaluate quizzes.");
        }

        if (questions == null || answers == null || questions.size() != answers.size()) {
            throw new IllegalArgumentException("Questions and answers must be non-null and of equal size.");
        }

        int totalScore = 0;
        int maxScore = questions.size();
        StringBuilder evaluationReport = new StringBuilder();
        Set<String> weakTopics = new HashSet<>();

        for (int i = 0; i < questions.size(); i++) {
            String prompt = "Evaluate the student's answer to the following question.\n\n"
                    + "Question: " + questions.get(i) + "\n"
                    + "Student Answer: " + answers.get(i) + "\n\n"
                    + "Respond in this format:\n"
                    + "1. Correctness (0 or 1):\n"
                    + "2. Short feedback:\n"
                    + "3. Topic to improve (if any):";

            String aiResponse = chatClient.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getContent();

            evaluationReport.append("Q").append(i + 1).append(": ").append(questions.get(i)).append("\n")
                    .append(aiResponse).append("\n\n");

            // Parse score (more reliable using regex)
            Pattern scorePattern = Pattern.compile("Correctness.*?(\\d)", Pattern.CASE_INSENSITIVE);
            Matcher scoreMatcher = scorePattern.matcher(aiResponse);
            if (scoreMatcher.find()) {
                int score = Integer.parseInt(scoreMatcher.group(1));
                totalScore += Math.min(score, 1); // Only add 1 or 0
            }

            // Parse weak topic
            Pattern topicPattern = Pattern.compile("Topic to improve \\(if any\\):\\s*(.*)", Pattern.CASE_INSENSITIVE);
            Matcher topicMatcher = topicPattern.matcher(aiResponse);
            if (topicMatcher.find()) {
                String topic = topicMatcher.group(1).trim();
                if (!topic.equalsIgnoreCase("none") && !topic.isEmpty()) {
                    weakTopics.add(topic);
                }
            }
        }

        evaluationReport.append("Final Score: ").append(totalScore).append(" out of ").append(maxScore).append("\n");
        if (!weakTopics.isEmpty()) {
            evaluationReport.append("Suggested Topics to Review: ").append(String.join(", ", weakTopics));
        } else {
            evaluationReport.append("Great job! No weak topics identified.");
        }
        QuizReport report = new QuizReport();
        report.setUsername(username);
        report.setQuestions(questions);
        report.setAnswers(answers);
        report.setReport(evaluationReport.toString());
        report.setScore(totalScore);
        report.setMaxScore(maxScore);
        quizReportRepository.save(report);
        return evaluationReport.toString();
    }



}