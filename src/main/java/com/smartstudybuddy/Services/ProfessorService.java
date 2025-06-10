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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

    public Subject addSubject(Subject subject) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can add subjects.");
        }

        if (subjectRepo.findBySubjectId(subject.getSubjectId()).isPresent()) {
            throw new IllegalArgumentException("Subject with this ID already exists.");
        }

        if (subjectRepo.findByName(subject.getName()).isPresent()) {
            throw new IllegalArgumentException("Subject with this name already exists.");
        }

        // Set ownership
        subject.setProfessorUsername(username);

        return subjectRepo.save(subject);
    }

    public Note uploadNotes(String subjectId, String title, MultipartFile file) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }
        String username = authentication.getName();
        // Verify ownership of the subject before allowing note upload
        Subject subject = subjectRepo.findBySubjectId(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can add subjects.");
        }
        if (!username.equals(subject.getProfessorUsername())) {
            throw new AccessDeniedException("You do not own this subject.");
        }

        Optional<Note> existingNote = notesRepo.findBySubjectIdAndTitle(subjectId, title);
        if (existingNote.isPresent()) {
            throw new IllegalArgumentException("A note with this title already exists for the subject.");
        }

        Note note = new Note();
        note.setSubjectId(subjectId);
        note.setTitle(title);
        note.setFileName(file.getOriginalFilename());
        note.setData(file.getBytes());
        return notesRepo.save(note);
    }

    public Exam setExam(Exam exam) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can add subjects.");
        }

        String subjectId= exam.getSubjectId();
        Subject subject = subjectRepo.findBySubjectId(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        if (!username.equals(subject.getProfessorUsername())) {
            throw new AccessDeniedException("You do not own this subject.");
        }

        exam.setSubjectId(subjectId);
        return examRepo.save(exam);
    }

    public List<Subject> getSubjects() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can view subjects.");
        }

        return subjectRepo.findByProfessorUsername(username);
    }

    public List<Note> getNotesBySubjectForProfessor(String subjectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can view notes.");
        }

        Subject subject = subjectRepo.findBySubjectId(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (!subject.getProfessorUsername().equals(username)) {
            throw new AccessDeniedException("You are not authorized to access these notes.");
        }

        return notesRepo.findBySubjectId(subjectId);
    }

    public List<Exam> getExamsBySubjectForProfessor(String subjectId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can view subjects.");
        }

        Subject subject = subjectRepo.findBySubjectId(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found"));

        if (!subject.getProfessorUsername().equals(username)) {
            throw new AccessDeniedException("You are not authorized to access these exams.");
        }

        return examRepo.findBySubjectId(subjectId);
    }

    public void deleteSubjectByProfessor(String subjectId) {
        // Step 1: Delete the subject
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can unregister subject.");
        }

        Subject subject = subjectRepo.findBySubjectId(subjectId)
                .orElseThrow(() -> new RuntimeException("Subject not found."));
        subjectRepo.delete(subject);

        // Step 2: Delete notes and exams for this subject
        notesRepo.deleteBySubjectId(subjectId);
        examRepo.deleteBySubjectId(subjectId);

        // Step 3: Deregister this subject from all students
        List<User> allUsers = userRepository.findAll();
        for (User u : allUsers) {
            List<String> updatedSubjects = u.getRegisteredSubjectIds()
                    .stream()
                    .filter(id -> !id.equals(subjectId))
                    .collect(Collectors.toList());
            u.setRegisteredSubjectIds(updatedSubjects);
        }
        userRepository.saveAll(allUsers);
    }

    public void deleteNoteById(String noteId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can delete note.");
        }
        Note note = notesRepo.findById(noteId)
                .orElseThrow(() -> new RuntimeException("Note not found."));

        // Fetch subject related to the note
        Subject subject = subjectRepo.findBySubjectId(note.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found."));


        if (!subject.getProfessorUsername().equals(username)) {
            throw new AccessDeniedException("You are not authorized to delete this note.");
        }

        notesRepo.deleteById(noteId);
    }

    public void deleteExamById(String examId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!"PROFESSOR".equalsIgnoreCase(user.getRole())) {
            throw new AccessDeniedException("Only professors can delete exam.");
        }
        Exam exam = examRepo.findById(examId)
                .orElseThrow(() -> new RuntimeException("Exam not found."));

        // Fetch subject related to the exam
        Subject subject = subjectRepo.findBySubjectId(exam.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found."));


        if (!subject.getProfessorUsername().equals(username)) {
            throw new AccessDeniedException("You are not authorized to delete this exam.");
        }

        examRepo.deleteById(examId);
    }

    public void deleteAuthenticatedUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User not authenticated.");
        }

        String username = auth.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));

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

            // Step 5: Delete professor's user account
            userRepository.delete(user);
        }
    }


}

