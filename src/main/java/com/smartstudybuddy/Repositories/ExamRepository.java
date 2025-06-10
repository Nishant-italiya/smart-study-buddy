package com.smartstudybuddy.Repositories;

import com.smartstudybuddy.Models.Exam;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExamRepository extends MongoRepository<Exam, String> {
    List<Exam> findBySubjectId(String subjectId);
    List<Exam> findBySubjectIdInAndExamDateAfter(List<String> subjectIds, LocalDate date);
    Optional<Exam> findBySubjectIdAndExamDateAfter(String subjectId, LocalDate date);
    void deleteBySubjectIdIn(List<String> subjectIds);
    void deleteBySubjectId(String subjectId);
}