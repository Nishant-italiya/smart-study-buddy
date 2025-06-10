package com.smartstudybuddy.Repositories;

import com.smartstudybuddy.Models.Note;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface NotesRepository extends MongoRepository<Note, String> {
    List<Note> findBySubjectId(String subjectId);
    Optional<Note> findBySubjectIdAndTitle(String subjectId, String title);
    void deleteBySubjectIdIn(List<String> subjectIds);
    void deleteBySubjectId(String subjectId);
}