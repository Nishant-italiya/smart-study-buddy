package com.smartstudybuddy.Models;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "notes")
@Data
public class Note {
    @Id
    private ObjectId id;

    // Reference to the Subject's subjectId this note belongs to
    private String subjectId;
    private String title;
    private String fileName;
    private byte[] data;
}
