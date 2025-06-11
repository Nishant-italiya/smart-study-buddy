package com.smartstudybuddy.Controllers;

import com.smartstudybuddy.Models.Subject;
import com.smartstudybuddy.Models.Note;
import com.smartstudybuddy.Models.Exam;
import com.smartstudybuddy.Services.ProfessorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/professor")
public class ProfessorDashboardController {

    @Autowired
    private ProfessorService professorService;

    // Add new subject - ownership is assigned inside service by logged-in professor
    @PostMapping("/subjects")
    public ResponseEntity<?> addSubject(@RequestBody Subject subject) {
        return professorService.addSubject(subject);
    }

    // Add notes to a subject
    @PostMapping("/subjects/notes")
    public ResponseEntity<?> uploadNotePdf(@RequestParam("subjectId") String subjectId,
                              @RequestParam("title") String title,
                              @RequestParam("file") MultipartFile file) throws IOException {
        return professorService.uploadNotes(subjectId, title, file);
    }

    // Set exam for a subject
    @PostMapping("/subjects/exams")
    public ResponseEntity<?> setExam(@RequestBody Exam exam) {
        return professorService.setExam(exam);
    }

    // Get all subjects owned by the logged-in professor
    @GetMapping("/subjects")
    public ResponseEntity<?> getSubjects() {
        return professorService.getSubjects();
    }

    @GetMapping("/notes/{subjectId}")
    public ResponseEntity<?> getNotesBySubject(@PathVariable String subjectId) {
        return ResponseEntity.ok(professorService.getNotesBySubjectForProfessor(subjectId));
    }

    @GetMapping("/exams/{subjectId}")
    public ResponseEntity<?> getExamsBySubject(@PathVariable String subjectId) {
        return ResponseEntity.ok(professorService.getExamsBySubjectForProfessor(subjectId));
    }

    @DeleteMapping("/delete-subject/{subjectId}")
    public ResponseEntity<?> deleteSubject(@PathVariable String subjectId) {
        professorService.deleteSubjectByProfessor(subjectId);
        return ResponseEntity.ok("Subject and related data deleted successfully.");
    }

    @DeleteMapping("/delete-note/{noteId}")
    public ResponseEntity<?> deleteNote(@PathVariable String noteId) {
        professorService.deleteNoteById(noteId);
        return ResponseEntity.ok("Note deleted successfully.");
    }

    @DeleteMapping("/delete-exam/{examId}")
    public ResponseEntity<?> deleteExam(@PathVariable String examId) {
        professorService.deleteExamById(examId);
        return ResponseEntity.ok("Exam deleted successfully.");
    }

    @DeleteMapping("/delete-profile")
    public ResponseEntity<?> deleteMyProfile() {
        professorService.deleteAuthenticatedUser();
        return ResponseEntity.ok("Profile deleted successfully.");
    }
}
