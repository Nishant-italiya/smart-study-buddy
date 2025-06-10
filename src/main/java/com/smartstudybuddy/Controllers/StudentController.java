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
    public String registerSubject(@PathVariable String subjectId) {
        return studentService.registerSubject(subjectId);
    }

    @GetMapping("/subjects")
    public List<Subject> getRegisteredSubjects() {
        return studentService.getRegisteredSubjects();
    }

    // Get notes for registered subject
    @GetMapping("/{subjectId}/notes")
    public List<Note> getNotesBySubjectId(@PathVariable String subjectId) {
        return studentService.getNotesBySubject(subjectId);
    }

    // Get exams for registered subject
    @GetMapping("/{subjectId}/exams")
    public List<Exam> getExamsBySubjectId(@PathVariable String subjectId) {
        return studentService.getExamsBySubject(subjectId);
    }

    @GetMapping("/my-reports")
    public List<QuizReport> getMyReports() {
        return studentService.getMyReports();
    }

    @DeleteMapping("/unregister/{subjectId}")
    public ResponseEntity<String> deleteRegisteredSubject(@PathVariable String subjectId) {
        studentService.deleteRegisteredSubject(subjectId);
        return ResponseEntity.ok("Subject unregistered successfully.");
    }

    @DeleteMapping("/delete-profile")
    public ResponseEntity<String> deleteMyProfile() {
        studentService.deleteAuthenticatedUser();
        return ResponseEntity.ok("Profile deleted successfully.");
    }


}
