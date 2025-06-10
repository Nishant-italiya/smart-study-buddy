package com.smartstudybuddy.Repositories;

import com.smartstudybuddy.Models.Subject;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SubjectRepository extends MongoRepository<Subject, String> {
    Optional<Subject> findBySubjectId(String subjectId);
    Optional<Subject> findByName(String name);

    // Find all subjects owned by a professor
    List<Subject> findByProfessorUsername(String professorUsername);

    @Query("{ 'subjectId': { $in: ?0 } }")
    List<Subject> findSubjectsBySubjectIdIn(List<String> subjectIds);
}
