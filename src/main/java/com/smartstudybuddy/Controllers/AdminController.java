package com.smartstudybuddy.Controllers;

import com.smartstudybuddy.Models.User;
import com.smartstudybuddy.Services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private AdminService adminService;

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return adminService.getAllUsers();
    }

    @DeleteMapping("/remove")
    public ResponseEntity<?> deleteUserByUsernameAndRole(@RequestParam String username,
                                                         @RequestParam String role) {
        return adminService.deleteUserByUsernameAndRole(username, role);
    }
}

