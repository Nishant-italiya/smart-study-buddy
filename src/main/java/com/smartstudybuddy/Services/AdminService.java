package com.smartstudybuddy.Services;

import com.smartstudybuddy.Models.Exam;
import com.smartstudybuddy.Models.Note;
import com.smartstudybuddy.Models.Subject;
import com.smartstudybuddy.Models.User;
import com.smartstudybuddy.Repositories.ExamRepository;
import com.smartstudybuddy.Repositories.NotesRepository;
import com.smartstudybuddy.Repositories.SubjectRepository;
import com.smartstudybuddy.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private NotesRepository noteRepository;

    @Autowired
    private ExamRepository examRepository;

    public ResponseEntity<?> getAllUsers() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to view users.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();
        Optional<User> optionalAdmin = userRepository.findByUsername(username);
        if (optionalAdmin.isEmpty() || !"ADMIN".equalsIgnoreCase(optionalAdmin.get().getRole())) {
            return new ResponseEntity<>("Access denied. Admin role required.", HttpStatus.FORBIDDEN);
        }

        List<User> users = userRepository.findAll();
        List<String> userSummaries = users.stream()
                .map(user -> "Username: " + user.getUsername() + ", Role: " + user.getRole())
                .toList();
        return ResponseEntity.ok(userSummaries);
    }

    public ResponseEntity<?> deleteUserByUsernameAndRole(String targetUsername, String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return new ResponseEntity<>("Login required to view users.", HttpStatus.UNAUTHORIZED);
        }

        String username = auth.getName();
        Optional<User> optionalAdmin = userRepository.findByUsername(username);
        if (optionalAdmin.isEmpty() || !"ADMIN".equalsIgnoreCase(optionalAdmin.get().getRole())) {
            return new ResponseEntity<>("Access denied. Admin role required.", HttpStatus.FORBIDDEN);
        }

        Optional<User> userOpt = userRepository.findByUsername(targetUsername);
        if (userOpt.isEmpty()) {
            return new ResponseEntity<>("User not found.", HttpStatus.NOT_FOUND);
        }

        User user = userOpt.get();

        if ("PROFESSOR".equalsIgnoreCase(role)) {
            List<Subject> subjects = subjectRepository.findByProfessorUsername(targetUsername);
            List<String> subjectIds = subjects.stream().map(Subject::getSubjectId).collect(Collectors.toList());

            noteRepository.deleteBySubjectIdIn(subjectIds);
            examRepository.deleteBySubjectIdIn(subjectIds);

            List<User> allUsers = userRepository.findAll();
            for (User u : allUsers) {
                if (u.getRegisteredSubjectIds() != null) {
                    List<String> updated = u.getRegisteredSubjectIds().stream()
                            .filter(id -> !subjectIds.contains(id))
                            .collect(Collectors.toList());
                    u.setRegisteredSubjectIds(updated);
                }
            }
            userRepository.saveAll(allUsers);
            subjectRepository.deleteAll(subjects);
        } else if ("STUDENT".equalsIgnoreCase(role)) {
            user.setRegisteredSubjectIds(new ArrayList<>());
        }

        userRepository.delete(user);
        return new ResponseEntity<>("User and all related data deleted successfully.", HttpStatus.OK);
    }
}

