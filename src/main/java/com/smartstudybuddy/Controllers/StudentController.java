package com.smartstudybuddy.Controllers;

import com.smartstudybuddy.Models.Exam;
import com.smartstudybuddy.Models.Note;
import com.smartstudybuddy.Models.QuizReport;
import com.smartstudybuddy.Models.Subject;
import com.smartstudybuddy.Services.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/student")
public class StudentController {

    @Autowired
    private StudentService studentService;

    // Register a subject
    @PostMapping("/register/{subjectId}")
    public ResponseEntity<String> registerSubject(@PathVariable String subjectId) {
        return studentService.registerSubject(subjectId);
    }

    @GetMapping("/subjects")
    public ResponseEntity<?> getRegisteredSubjects() {
        return studentService.getRegisteredSubjects();
    }

    // Get notes for registered subject
    @GetMapping("/{subjectId}/notes")
    public ResponseEntity<?> getNotesBySubjectId(@PathVariable String subjectId) {
        return studentService.getNotesBySubject(subjectId);
    }

    // Get exams for registered subject
    @GetMapping("/{subjectId}/exams")
    public ResponseEntity<?> getExamsBySubjectId(@PathVariable String subjectId) {
        return studentService.getExamsBySubject(subjectId);
    }

    @GetMapping("/my-reports")
    public ResponseEntity<?> getMyReports() {
        return studentService.getMyReports();
    }

    @DeleteMapping("/unregister/{subjectId}")
    public ResponseEntity<?> deleteRegisteredSubject(@PathVariable String subjectId) {
        studentService.deleteRegisteredSubject(subjectId);
        return ResponseEntity.ok("Subject unregistered successfully.");
    }

    @DeleteMapping("/delete-profile")
    public ResponseEntity<?> deleteMyProfile() {
        studentService.deleteAuthenticatedUser();
        return ResponseEntity.ok("Profile deleted successfully.");
    }


}
