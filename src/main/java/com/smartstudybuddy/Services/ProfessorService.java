package com.smartstudybuddy.Services;


import com.smartstudybuddy.Models.Subject;
import com.smartstudybuddy.Models.Note;
import com.smartstudybuddy.Models.Exam;
import com.smartstudybuddy.Models.User;
import com.smartstudybuddy.Repositories.SubjectRepository;
import com.smartstudybuddy.Repositories.NotesRepository;
import com.smartstudybuddy.Repositories.ExamRepository;
import com.smartstudybuddy.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProfessorService {
    @Autowired private SubjectRepository subjectRepo;
    @Autowired private NotesRepository notesRepo;
    @Autowired private ExamRepository examRepo;
    @Autowired private UserRepository userRepository;

    public ResponseEntity<?> addSubject(Subject subject) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }

        User user = optionalUser.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can add subjects.", HttpStatus.FORBIDDEN);
        }

        if (subjectRepo.findBySubjectId(subject.getSubjectId()).isPresent()) {
            return new ResponseEntity<>("Subject with this ID already exists.", HttpStatus.CONFLICT);
        }

        if (subjectRepo.findByName(subject.getName()).isPresent()) {
            return new ResponseEntity<>("Subject with this name already exists.", HttpStatus.CONFLICT);
        }

        // Set professor who is adding the subject
        subject.setProfessorUsername(username);

        Subject savedSubject = subjectRepo.save(subject);
        return new ResponseEntity<>(savedSubject, HttpStatus.CREATED);
    }


    public ResponseEntity<?> uploadNotes(String subjectId, String title, MultipartFile file) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found.", HttpStatus.NOT_FOUND);
        }
        Subject subject = subjectOpt.get();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can add notes.", HttpStatus.FORBIDDEN);
        }

        if (!username.equals(subject.getProfessorUsername())) {
            return new ResponseEntity<>("You do not own this subject.", HttpStatus.FORBIDDEN);
        }

        Optional<Note> existingNote = notesRepo.findBySubjectIdAndTitle(subjectId, title);
        if (existingNote.isPresent()) {
            return new ResponseEntity<>("A note with this title already exists for the subject.", HttpStatus.CONFLICT);
        }

        Note note = new Note();
        note.setSubjectId(subjectId);
        note.setTitle(title);
        note.setFileName(file.getOriginalFilename());
        note.setData(file.getBytes());

        Note savedNote = notesRepo.save(note);
        return new ResponseEntity<>(savedNote, HttpStatus.CREATED);
    }


    public ResponseEntity<?> setExam(Exam exam) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can add exams.", HttpStatus.FORBIDDEN);
        }

        String subjectId = exam.getSubjectId();
        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found.", HttpStatus.NOT_FOUND);
        }
        Subject subject = subjectOpt.get();

        if (!username.equals(subject.getProfessorUsername())) {
            return new ResponseEntity<>("You do not own this subject.", HttpStatus.FORBIDDEN);
        }

        exam.setSubjectId(subjectId);
        Exam savedExam = examRepo.save(exam);
        return new ResponseEntity<>(savedExam, HttpStatus.CREATED);
    }


    public ResponseEntity<?> getSubjects() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can view subjects.", HttpStatus.FORBIDDEN);
        }

        List<Subject> subjects = subjectRepo.findByProfessorUsername(username);
        return new ResponseEntity<>(subjects, HttpStatus.OK);
    }


    public ResponseEntity<?> getNotesBySubjectForProfessor(String subjectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can view notes.", HttpStatus.FORBIDDEN);
        }

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found", HttpStatus.NOT_FOUND);
        }

        Subject subject = subjectOpt.get();
        if (!subject.getProfessorUsername().equals(username)) {
            return new ResponseEntity<>("You are not authorized to access these notes.", HttpStatus.FORBIDDEN);
        }

        List<Note> notes = notesRepo.findBySubjectId(subjectId);
        return new ResponseEntity<>(notes, HttpStatus.OK);
    }


    public ResponseEntity<?> getExamsBySubjectForProfessor(String subjectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can view subjects.", HttpStatus.FORBIDDEN);
        }

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found", HttpStatus.NOT_FOUND);
        }
        Subject subject = subjectOpt.get();

        if (!subject.getProfessorUsername().equals(username)) {
            return new ResponseEntity<>("You are not authorized to access these exams.", HttpStatus.FORBIDDEN);
        }

        List<Exam> exams = examRepo.findBySubjectId(subjectId);
        return new ResponseEntity<>(exams, HttpStatus.OK);
    }


    public ResponseEntity<?> deleteSubjectByProfessor(String subjectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can unregister subject.", HttpStatus.FORBIDDEN);
        }

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(subjectId);
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found.", HttpStatus.NOT_FOUND);
        }

        Subject subject = subjectOpt.get();
        subjectRepo.delete(subject);

        // Delete notes and exams
        notesRepo.deleteBySubjectId(subjectId);
        examRepo.deleteBySubjectId(subjectId);

        // Deregister subject from all users
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            List<String> updatedSubjects = u.getRegisteredSubjectIds()
                    .stream()
                    .filter(id -> !id.equals(subjectId))
                    .collect(Collectors.toList());
            u.setRegisteredSubjectIds(updatedSubjects);
        }
        userRepository.saveAll(allUsers);

        return new ResponseEntity<>("Subject and associated data deleted successfully.", HttpStatus.OK);
    }


    public ResponseEntity<?> deleteNoteById(String noteId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can delete note.", HttpStatus.FORBIDDEN);
        }

        Optional<Note> noteOpt = notesRepo.findById(noteId);
        if (noteOpt.isEmpty()) {
            return new ResponseEntity<>("Note not found.", HttpStatus.NOT_FOUND);
        }

        Note note = noteOpt.get();

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(note.getSubjectId());
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found.", HttpStatus.NOT_FOUND);
        }

        Subject subject = subjectOpt.get();

        if (!subject.getProfessorUsername().equals(username)) {
            return new ResponseEntity<>("You are not authorized to delete this note.", HttpStatus.FORBIDDEN);
        }

        notesRepo.deleteById(noteId);
        return new ResponseEntity<>("Note deleted successfully.", HttpStatus.OK);
    }


    public ResponseEntity<?> deleteExamById(String examId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = authentication.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            return new ResponseEntity<>("Only professors can delete exam.", HttpStatus.FORBIDDEN);
        }

        Optional<Exam> examOpt = examRepo.findById(examId);
        if (examOpt.isEmpty()) {
            return new ResponseEntity<>("Exam not found.", HttpStatus.NOT_FOUND);
        }

        Exam exam = examOpt.get();

        Optional<Subject> subjectOpt = subjectRepo.findBySubjectId(exam.getSubjectId());
        if (subjectOpt.isEmpty()) {
            return new ResponseEntity<>("Subject not found.", HttpStatus.NOT_FOUND);
        }

        Subject subject = subjectOpt.get();

        if (!subject.getProfessorUsername().equals(username)) {
            return new ResponseEntity<>("You are not authorized to delete this exam.", HttpStatus.FORBIDDEN);
        }

        examRepo.deleteById(examId);
        return new ResponseEntity<>("Exam deleted successfully.", HttpStatus.OK);
    }


    public ResponseEntity<?> deleteAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("User not authenticated.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();
        String role = user.getRole();

        if ("PROFESSOR".equalsIgnoreCase(role)) {
            // Step 1: Get all subjects created by professor
            List<Subject> subjects = subjectRepo.findByProfessorUsername(username);
            List<String> subjectIds = subjects.stream()
                    .map(Subject::getSubjectId)
                    .collect(Collectors.toList());

            // Step 2: Delete related notes and exams
            notesRepo.deleteBySubjectIdIn(subjectIds);
            examRepo.deleteBySubjectIdIn(subjectIds);

            // Step 3: Remove subject IDs from all users (students)
            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                List<String> updated = u.getRegisteredSubjectIds().stream()
                        .filter(id -> !subjectIds.contains(id))
                        .collect(Collectors.toList());
                u.setRegisteredSubjectIds(updated);
            }
            userRepository.saveAll(allUsers);

            // Step 4: Delete subjects
            subjectRepo.deleteAll(subjects);
        }

        // Step 5: Delete user (professor or student)
        userRepository.delete(user);

        return new ResponseEntity<>("User and related data deleted successfully.", HttpStatus.OK);
    }



}

