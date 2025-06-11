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

    public ResponseEntity<?> register(User user) {
        // Check if user with same username already exists
        Optional<User> existingUser = userRepository.findByUsername(user.getUsername());
        if (existingUser.isPresent()) {
            return new ResponseEntity<>("Username already exists", HttpStatus.BAD_REQUEST);
        }

        // Validate common fields
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            return new ResponseEntity<>("Username is required", HttpStatus.BAD_REQUEST);
        }
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            return new ResponseEntity<>("Password is required", HttpStatus.BAD_REQUEST);
        }
        if (user.getRole() == null || user.getRole().isEmpty()) {
            return new ResponseEntity<>("Role is required", HttpStatus.BAD_REQUEST);
        }

        // Normalize role
        String role = user.getRole().toUpperCase();
        if (role.equals("STUDENT")) {
            if (user.getBranch() == null || user.getBranch().isEmpty()) {
                return new ResponseEntity<>("Branch is required for students", HttpStatus.BAD_REQUEST);
            }
            if (user.getSemester() <= 0) {
                return new ResponseEntity<>("Semester must be greater than 0 for students", HttpStatus.BAD_REQUEST);
            }
        } else if (role.equals("PROFESSOR")) {
            user.setBranch(null);
            user.setSemester(0);
        } else {
            return new ResponseEntity<>("Invalid role. Must be STUDENT or PROFESSOR", HttpStatus.BAD_REQUEST);
        }

        // Encode password and save
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
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