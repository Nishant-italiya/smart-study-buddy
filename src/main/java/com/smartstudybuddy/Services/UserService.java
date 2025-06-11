package com.smartstudybuddy.Services;

import com.smartstudybuddy.Models.User;
import com.smartstudybuddy.Repositories.UserRepository;
import com.smartstudybuddy.Utilis.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private static final PasswordEncoder passwordEncoder=new BCryptPasswordEncoder();

    public User register(User user) {
        // Check if user with same username already exists (username is unique)
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Validate common fields
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            throw new RuntimeException("Username is required");
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            throw new RuntimeException("Password is required");
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            throw new RuntimeException("Role is required");
        }

        // Normalize role to "ROLE_STUDENT" / "ROLE_PROFESSOR"
        String role = user.getRole().toUpperCase();
        if (role.equals("STUDENT")) {
            if (user.getBranch() == null || user.getBranch().isEmpty()) {
                throw new RuntimeException("Branch is required for students");
            }
            if (user.getSemester() <= 0) {
                throw new RuntimeException("Semester must be greater than 0 for students");
            }
        } else if (role.equals("PROFESSOR")) {
            user.setBranch(null);
            user.setSemester(0);
        } else {
            throw new RuntimeException("Invalid role. Must be STUDENT or PROFESSOR");
        }

        // Encode password and save
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }


    public ResponseEntity<String> login(User user) {
        try {
            Optional<User> optionalUser = userRepository.findByUsername(user.getUsername());

            if (optionalUser.isEmpty()) {
                return new ResponseEntity<>("User not found", HttpStatus.BAD_REQUEST);
            }

            User foundUser = optionalUser.get();

            if (!passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
                return new ResponseEntity<>("Invalid password", HttpStatus.BAD_REQUEST);
            }

            String requestedRole = user.getRole().toUpperCase();
            String actualRole = foundUser.getRole().toUpperCase();

            if (!requestedRole.equals(actualRole)) {
                return new ResponseEntity<>("Invalid role", HttpStatus.FORBIDDEN);
            }

            String jwt = jwtUtil.generateToken(foundUser.getUsername());
            return new ResponseEntity<>(jwt, HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Login failed", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}