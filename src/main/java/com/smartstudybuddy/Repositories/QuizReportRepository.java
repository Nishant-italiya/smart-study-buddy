package com.smartstudybuddy.Repositories;

import com.smartstudybuddy.Models.QuizReport;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface QuizReportRepository extends MongoRepository<QuizReport, String> {
    List<QuizReport> findByUsername(String username);

}
