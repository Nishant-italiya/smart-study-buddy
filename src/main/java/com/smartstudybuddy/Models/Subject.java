package com.smartstudybuddy.Models;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "subjects")
@Data
@CompoundIndexes({
        @CompoundIndex(name = "unique_courseId_name", def = "{'subjectId': 1, 'name': 1}", unique = true)
})
public class Subject {
    @Id
    private ObjectId id;
    private String subjectId;
    private String name;

    // New field to store professor username who created this subject
    private String professorUsername;

    // getters and setters (or use Lombok @Data to generate)
}
