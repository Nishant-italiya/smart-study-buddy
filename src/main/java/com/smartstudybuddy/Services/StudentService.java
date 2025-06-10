package com.smartstudybuddy.Services;

import com.smartstudybuddy.Models.*;
import com.smartstudybuddy.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private SubjectRepository subjectRepo;

    @Autowired
    private NotesRepository notesRepo;

    @Autowired
    private ExamRepository examRepo;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private QuizReportRepository quizReportRepository;

    // Student registers for a subject
    public String registerSubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Login required to register for a subject.");
        }

        String username = auth.getName();

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            throw new IllegalArgumentException("Subject with this ID does not exist.");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can register for subjects.");
        }

        if (user.getRegisteredSubjectIds() == null) {
            user.setRegisteredSubjectIds(new ArrayList<>());
        }

        if (user.getRegisteredSubjectIds().contains(subjectId)) {
            return "You have already registered for this subject.";
        }

        user.getRegisteredSubjectIds().add(subjectId);
        userRepo.save(user);

        return "Subject registered successfully.";
    }

    public List<Subject> getRegisteredSubjects() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Login required to view subjects.");
        }

        String username = auth.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (user.getRegisteredSubjectIds().size()==0) {
            throw new AccessDeniedException("You are not registered any subject.");
        }

        return subjectRepo.findSubjectsBySubjectIdIn(user.getRegisteredSubjectIds());
    }

    // Get notes for a registered subject
    public List<Note> getNotesBySubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Login required to view notes.");
        }

        String username = auth.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            throw new AccessDeniedException("You are not registered for this subject.");
        }

        return notesRepo.findBySubjectId(subjectId);
    }

    // Get exams for a registered subject
    public List<Exam> getExamsBySubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Login required to view exams.");
        }

        String username = auth.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));

        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            throw new AccessDeniedException("You are not registered for this subject.");
        }

        return examRepo.findBySubjectId(subjectId);
    }

    public List<QuizReport> getMyReports() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("Login required to view exams.");
        }

        String username = auth.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new AccessDeniedException("User not found."));
        return quizReportRepository.findByUsername(username);
    }

    public void deleteRegisteredSubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = auth.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only students can unregister subjects.");
        }


        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            throw new AccessDeniedException("You are not registered for this subject.");
        }

        user.getRegisteredSubjectIds().remove(subjectId);
        userRepo.save(user);
    }

    public void deleteAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = auth.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

        // If user is a student, also delete student-related data
        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            User student = userRepo.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Student not found."));

            // Clear registered subjects
            student.setRegisteredSubjectIds(new ArrayList<>());
        }
        // Finally delete user
        userRepo.delete(user);
    }

}
