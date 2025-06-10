package com.smartstudybuddy.Services;

import com.smartstudybuddy.Models.User;
import com.smartstudybuddy.Repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

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


    public User login(User user) {
        Optional<User> optionalUser = userRepository.findByUsername(user.getUsername());

        if (optionalUser.isEmpty()) {
            throw new RuntimeException("User not found");
        }

        User foundUser = optionalUser.get();
        // Check password
        if (!passwordEncoder.matches(user.getPassword(), foundUser.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Check role
        if (!foundUser.getRole().equals(user.getRole())) {
            throw new RuntimeException("Invalid role");
        }

        return foundUser;
    }
}