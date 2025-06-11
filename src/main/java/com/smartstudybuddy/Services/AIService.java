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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public ResponseEntity<?> getNoteSummary(String subjectId, String noteTitle) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();
        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only students can access this feature.", HttpStatus.FORBIDDEN);
        }

        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            return new ResponseEntity<>("You are not registered for this subject.", HttpStatus.FORBIDDEN);
        }

        Optional<Note> noteOpt = notesRepo.findBySubjectIdAndTitle(subjectId, noteTitle);
        if (noteOpt.isEmpty()) {
            return new ResponseEntity<>("Note not found.", HttpStatus.NOT_FOUND);
        }

        String noteContent = "";
        try {
            PDDocument document = PDDocument.load(noteOpt.get().getData());
            PDFTextStripper stripper = new PDFTextStripper();
            noteContent = stripper.getText(document);
            document.close();
        } catch (IOException e) {
            return new ResponseEntity<>("Failed to extract text from PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String promptMessage = "Please summarize the following note:\n\n" + noteContent;

        try {
            String summary = chatClient.call(new Prompt(promptMessage))
                    .getResult()
                    .getOutput()
                    .getContent();
            return new ResponseEntity<>(summary, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("AI call failed: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    public ResponseEntity<?> generateEfficientPreparationPlan() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
            }

            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found."));

            if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can access this feature.");
            }

            List<String> subjectIds = user.getRegisteredSubjectIds();
            if (subjectIds.isEmpty()) {
                return ResponseEntity.ok("No registered subjects found.");
            }

            LocalDate today = LocalDate.now();
            List<Exam> upcomingExams = examRepository.findBySubjectIdInAndExamDateAfter(subjectIds, today);
            if (upcomingExams.isEmpty()) {
                return ResponseEntity.ok("No upcoming exams found for your registered subjects.");
            }

            StringBuilder promptBuilder = new StringBuilder();
            promptBuilder.append("You are an expert study planner. Create an efficient preparation plan for these upcoming exams:\n\n");

            for (Exam exam : upcomingExams) {
                promptBuilder.append("Subject ID: ").append(exam.getSubjectId()).append("\n");
                promptBuilder.append("Exam Date: ").append(exam.getExamDate()).append("\n");
                promptBuilder.append("Exam Type: ").append(exam.getExamType()).append("\n");
                promptBuilder.append("Syllabus: ").append(exam.getSyllabus()).append("\n\n");
            }

            String prompt = promptBuilder.toString();
            String plan = chatClient.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getContent();

            return ResponseEntity.ok(plan);

        } catch (UsernameNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI call failed: " + e.getMessage());
        }
    }


    public ResponseEntity<?> generateImportantQuestions(String subjectId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
            }

            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found."));

            if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can access this feature.");
            }

            if (!user.getRegisteredSubjectIds().contains(subjectId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not registered for this subject.");
            }

            LocalDate today = LocalDate.now();
            Exam exam = examRepository.findBySubjectIdAndExamDateAfter(subjectId, today)
                    .orElseThrow(() -> new IllegalArgumentException("No upcoming exam found for this subject."));

            String syllabus = exam.getSyllabus();
            List<Note> notes = notesRepo.findBySubjectId(subjectId);

            if (notes.isEmpty()) {
                String prompt = "The syllabus for an upcoming exam is:\n" + syllabus +
                        "\n\nGenerate important questions with answers based only on this syllabus.";
                String aiResponse = chatClient.call(new Prompt(prompt))
                        .getResult()
                        .getOutput()
                        .getContent();

                return ResponseEntity.ok(aiResponse != null && !aiResponse.isBlank()
                        ? aiResponse
                        : "AI could not generate questions from the syllabus.");
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
                String fallbackPrompt = "The syllabus for an upcoming exam is:\n" + syllabus +
                        "\n\nGenerate important questions with answers based only on this syllabus.";
                String fallbackResponse = chatClient.call(new Prompt(fallbackPrompt))
                        .getResult()
                        .getOutput()
                        .getContent();

                return ResponseEntity.ok(fallbackResponse != null && !fallbackResponse.isBlank()
                        ? fallbackResponse
                        : "AI could not generate questions from the syllabus.");
            }

            return ResponseEntity.ok(combinedResponses.toString());

        } catch (UsernameNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI call failed: " + e.getMessage());
        }
    }


    public ResponseEntity<?> askGeneralQuestion(String question) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
            }

            String username = auth.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found."));

            // Optionally log who asked what
            String role = user.getRole();  // Can be used for logging if needed

            String aiResponse = chatClient.call(new Prompt(question))
                    .getResult()
                    .getOutput()
                    .getContent();

            if (aiResponse != null && !aiResponse.isBlank()) {
                return ResponseEntity.ok(aiResponse);
            } else {
                return ResponseEntity.ok("AI was not able to answer your question.");
            }

        } catch (UsernameNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI call failed: " + e.getMessage());
        }
    }


    public ResponseEntity<?> generateQuiz(String subjectId, String topic) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
            }

            String username = auth.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found."));

            if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can access this feature.");
            }

            String prompt;

            if (!user.getRegisteredSubjectIds().contains(subjectId)) {
                prompt = "Generate a 10-question multiple-choice quiz for the topic: " + topic +
                        " in the subject. Do not give answers, only generate questions.";
            } else {
                Optional<Note> optionalNote = notesRepo.findBySubjectIdAndTitle(subjectId, topic);

                if (optionalNote.isPresent()) {
                    Note note = optionalNote.get();
                    StringBuilder content = new StringBuilder();

                    try (PDDocument doc = PDDocument.load(note.getData())) {
                        PDFTextStripper stripper = new PDFTextStripper();
                        content.append(stripper.getText(doc));
                    } catch (IOException e) {
                        content.setLength(0); // fallback to empty
                    }

                    if (!content.isEmpty()) {
                        prompt = "Based on the following notes content, generate only questions (no answers). " +
                                "Create a 10-question multiple-choice quiz on the topic: " + topic + "\n\nNotes:\n" + content;
                    } else {
                        prompt = "Generate a 10-question multiple-choice quiz for the topic: " + topic +
                                " in the subject. Do not give answers, only generate questions.";
                    }
                } else {
                    prompt = "Generate a 10-question multiple-choice quiz for the topic: " + topic +
                            " in the subject. Do not give answers, only generate questions.";
                }
            }

            String result = chatClient.call(new Prompt(prompt))
                    .getResult()
                    .getOutput()
                    .getContent();

            return ResponseEntity.ok(result);

        } catch (UsernameNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("AI call failed: " + e.getMessage());
        }
    }


    public ResponseEntity<?> evaluateQuiz(QuizEvaluationRequest request) {
        try {
            List<String> questions = request.getQuestions();
            List<String> answers = request.getAnswers();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated.");
            }

            String username = auth.getName();

            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found."));

            if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only students can evaluate quizzes.");
            }

            if (questions == null || answers == null || questions.size() != answers.size()) {
                return ResponseEntity.badRequest().body("Questions and answers must be non-null and of equal size.");
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

                // Parse score
                Pattern scorePattern = Pattern.compile("Correctness.*?(\\d)", Pattern.CASE_INSENSITIVE);
                Matcher scoreMatcher = scorePattern.matcher(aiResponse);
                if (scoreMatcher.find()) {
                    int score = Integer.parseInt(scoreMatcher.group(1));
                    totalScore += Math.min(score, 1);
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

            return ResponseEntity.ok(evaluationReport.toString());

        } catch (UsernameNotFoundException | AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Quiz evaluation failed: " + e.getMessage());
        }
    }




}