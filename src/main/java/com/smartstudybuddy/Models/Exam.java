package com.smartstudybuddy.Models;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "exams")
@Data
public class Exam {
    @Id
    private ObjectId id;

    // Reference to the Subject's subjectId this exam belongs to
    private String subjectId;

    private LocalDate examDate;
    private String examType; // Midterm, Endsem, Quiz

    private String syllabus; // New field for exam syllabus
}
