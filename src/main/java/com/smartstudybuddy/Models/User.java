package com.smartstudybuddy.Models;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document(collection = "users")
@Data
public class User {
    @Id
    private ObjectId id;

    @Indexed(unique = true)  // Enforces unique username
    private String username;

    private String password;
    private String role;
    private String branch;
    private int semester;

    private List<String> registeredSubjectIds = new ArrayList<>();
}