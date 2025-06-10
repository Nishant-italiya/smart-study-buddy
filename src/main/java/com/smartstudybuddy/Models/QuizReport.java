package com.smartstudybuddy.Models;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "quizReports")
@Data
public class QuizReport {
    @Id
    private ObjectId id;
    private String username;
    private List<String> questions;
    private List<String> answers;
    private String report;
    private int score;
    private int maxScore;
}
