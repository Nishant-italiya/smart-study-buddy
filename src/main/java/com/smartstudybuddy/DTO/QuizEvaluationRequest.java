package com.smartstudybuddy.DTO;

import java.util.List;

public class QuizEvaluationRequest {
    private List<String> questions;
    private List<String> answers;

    public QuizEvaluationRequest() {}

    public QuizEvaluationRequest(List<String> questions, List<String> answers) {
        this.questions = questions;
        this.answers = answers;
    }

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public List<String> getAnswers() {
        return answers;
    }

    public void setAnswers(List<String> answers) {
        this.answers = answers;
    }
}