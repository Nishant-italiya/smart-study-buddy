package com.smartstudybuddy.Services;

import com.smartstudybuddy.Models.*;
import com.smartstudybuddy.Repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    public ResponseEntity<String> registerSubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to register for a subject.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject with this ID does not exist.", HttpStatus.NOT_FOUND);
        }

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.UNAUTHORIZED);
        }

        User user = optionalUser.get();

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only students can register for subjects.", HttpStatus.FORBIDDEN);
        }

        if (user.getRegisteredSubjectIds() == null) {
            user.setRegisteredSubjectIds(new ArrayList<>());
        }

        if (user.getRegisteredSubjectIds().contains(subjectId)) {
            return new ResponseEntity<>("You have already registered for this subject.", HttpStatus.CONFLICT);
        }

        user.getRegisteredSubjectIds().add(subjectId);
        userRepo.save(user);

        return new ResponseEntity<>("Subject registered successfully.", HttpStatus.OK);
    }


    public ResponseEntity<?> getRegisteredSubjects() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to view subjects.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.UNAUTHORIZED);
        }

        User user = optionalUser.get();

        if (user.getRegisteredSubjectIds() == null || user.getRegisteredSubjectIds().isEmpty()) {
            return new ResponseEntity<>("You are not registered in any subject.", HttpStatus.NOT_FOUND);
        }

        List<Subject> subjects = subjectRepo.findSubjectsBySubjectIdIn(user.getRegisteredSubjectIds());
        return new ResponseEntity<>(subjects, HttpStatus.OK);
    }


    // Get notes for a registered subject
    public ResponseEntity<?> getNotesBySubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to view notes.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.UNAUTHORIZED);
        }

        User user = optionalUser.get();

        if (user.getRegisteredSubjectIds() == null || !user.getRegisteredSubjectIds().contains(subjectId)) {
            return new ResponseEntity<>("You are not registered for this subject.", HttpStatus.FORBIDDEN);
        }

        List<Note> notes = notesRepo.findBySubjectId(subjectId);
        return new ResponseEntity<>(notes, HttpStatus.OK);
    }


    // Get exams for a registered subject
    public ResponseEntity<?> getExamsBySubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to view exams.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.UNAUTHORIZED);
        }

        User user = optionalUser.get();

        if (user.getRegisteredSubjectIds() == null || !user.getRegisteredSubjectIds().contains(subjectId)) {
            return new ResponseEntity<>("You are not registered for this subject.", HttpStatus.FORBIDDEN);
        }

        List<Exam> exams = examRepo.findBySubjectId(subjectId);
        return new ResponseEntity<>(exams, HttpStatus.OK);
    }


    public ResponseEntity<?> getMyReports() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to view reports.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.UNAUTHORIZED);
        }

        List<QuizReport> reports = quizReportRepository.findByUsername(username);
        return new ResponseEntity<>(reports, HttpStatus.OK);
    }


    public ResponseEntity<?> deleteRegisteredSubject(String subjectId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.UNAUTHORIZED);
        }

        User user = optionalUser.get();

        if (!"STUDENT".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only students can unregister subjects.", HttpStatus.FORBIDDEN);
        }

        if (!user.getRegisteredSubjectIds().contains(subjectId)) {
            return new ResponseEntity<>("You are not registered for this subject.", HttpStatus.BAD_REQUEST);
        }

        user.getRegisteredSubjectIds().remove(subjectId);
        userRepo.save(user);

        return new ResponseEntity<>("Subject unregistered successfully.", HttpStatus.OK);
    }


    public ResponseEntity<?> deleteAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> optionalUser = userRepo.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }

        User user = optionalUser.get();

        if ("STUDENT".equalsIgnoreCase(user.getRole())) {
            user.setRegisteredSubjectIds(new ArrayList<>()); // Clear registered subjects
        }

        userRepo.delete(user);

        return new ResponseEntity<>("User deleted successfully.", HttpStatus.OK);
    }


}
