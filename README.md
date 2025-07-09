# 🎓 Smart Study Buddy 

A backend, AI-powered web platform built with **Spring Boot** that empowers students with personalized revision plans, note summarization, important question prediction, quizzes, and more — while allowing professors to manage subjects, notes, and exams.

🔗 GitHub Repository: [https://github.com/Nishant-italiya/smart-study-buddy](https://github.com/Nishant-italiya/smart-study-buddy)

---

##  Features

### 👨‍🎓 For Students
-  Register/Login with JWT authentication
-  Register & Unregister for subjects
-  Access notes and exam details
-  Get AI-generated preparation plans & important questions
-  Generate quizzes and evaluate answers
-  Track your AI-generated report history
-  Delete your account anytime

### 👨‍🏫 For Professors
-  Add subjects with ownership tied to your login
-  Upload PDF notes for each subject
-  Set exam dates for subjects
-  View or delete your uploaded notes, exams, or subjects
-  Delete your professor profile

---

## ⚙ Tech Stack

### 💻 Backend
- **Spring Boot**
- **Spring Security + JWT**
- **MongoDB Atlas**
- **Spring AI + OpenRouter API** (DeepSeek Model)

---

## 🧩 AI Integration

All AI features use [OpenRouter](https://openrouter.ai/) via `Spring AI` with the model:

- [`deepseek/deepseek-r1-distill-llama-70b:free`](https://openrouter.ai/models/deepseek/deepseek-r1-distill-llama-70b)

Used for:
-  Summarizing notes
-  Generating revision plans
-  Predicting important questions
-  Quiz generation & evaluation
-  General AI Q&A

---

## 📁 Project Structure
src/
- ├── controller/     # All REST APIs (User, Student, Professor, AI)
- ├── model/          # MongoDB document models
- ├── repository/     # MongoDB Repositories
- ├── service/        # Business logic and AI processing
- ├── config/         # Security config and JWT setup
- └── dto/            # Data transfer objects (requests/responses)

- 
---

## 🔐 Authentication & Authorization

- JWT-based Authentication
- Role-based access: `STUDENT` or `PROFESSOR`
- Each token carries role and email
- Frontend must send JWT in request headers

---

## 🛠 Setup & Run Locally

### 1️⃣ Prerequisites
- Java 17+
- Maven
- MongoDB Atlas (or local MongoDB)
- OpenRouter API Key

### 2️⃣ Clone the Repo

bash
git clone https://github.com/Nishant-italiya/smart-study-buddy.git
cd smart-study-buddy


### 3️⃣ Configure `application.properties` ###

Update the file at: `src/main/resources/application.properties`

```properties
# === Server Configuration ===
server.port=8081
spring.application.name=smartstudybuddy

# === MongoDB Atlas ===
spring.data.mongodb.uri=your_mongodb_uri
spring.data.mongodb.database=smartstudybuddy

# === OpenRouter AI ===
spring.ai.openai.api-key=your_openrouter_api_key
spring.ai.openai.base-url=https://openrouter.ai/api
spring.ai.openai.chat.options.model=deepseek/deepseek-r1-distill-llama-70b:free

# === JWT Secret ===
jwt.secret=your_jwt_secret

# === Spring AI Specific ===
spring.main.web-application-type=servlet
spring.cloud.function.scan.enabled=false
```

### 4️⃣ Run the Application

bash
./mvnw spring-boot:run

## 🛁 API Endpoints

### 🔐 Authentication

| Method | Endpoint           | Description       | Access |
|--------|--------------------|-------------------|--------|
| POST   | `/users/register`  | Register a user   | Public |
| POST   | `/users/login`     | Login & get token | Public |

---

### 👨‍🏫 Professor APIs

| Method | Endpoint                                | Description                 |
|--------|-----------------------------------------|-----------------------------|
| POST   | `/professor/subjects`                   | Add subject                 |
| POST   | `/professor/subjects/notes`             | Upload notes (PDF)         |
| POST   | `/professor/subjects/exams`             | Set exam                   |
| GET    | `/professor/subjects`                   | View owned subjects        |
| GET    | `/professor/notes/{subjectId}`          | View notes by subject      |
| GET    | `/professor/exams/{subjectId}`          | View exams by subject      |
| DELETE | `/professor/delete-subject/{subjectId}` | Delete subject             |
| DELETE | `/professor/delete-note/{noteId}`       | Delete note                |
| DELETE | `/professor/delete-exam/{examId}`       | Delete exam                |
| DELETE | `/professor/delete-profile`             | Delete profile             |

---

### 👨‍🎓 Student APIs

| Method | Endpoint                             | Description               |
|--------|--------------------------------------|---------------------------|
| POST   | `/student/register/{subjectId}`      | Register for subject      |
| GET    | `/student/subjects`                  | View registered subjects  |
| GET    | `/student/{subjectId}/notes`         | View subject notes        |
| GET    | `/student/{subjectId}/exams`         | View subject exams        |
| GET    | `/student/my-reports`                | View AI-generated reports |
| DELETE | `/student/unregister/{subjectId}`    | Unregister from subject   |
| DELETE | `/student/delete-profile`            | Delete profile            |

---

### 🤖 AI Services

| Method | Endpoint                                             | Description                        |
|--------|------------------------------------------------------|------------------------------------|
| GET    | `/ai/summary/{subjectId}/{noteTitle}`                | Summarize specific note            |
| GET    | `/ai/generate-preparation-plan`                      | Generate revision plan             |
| GET    | `/ai/generate-important-questions/{subjectId}`       | Predict important questions        |
| POST   | `/ai/generate-quiz?subjectId=...&topic=...`          | Generate quiz                      |
| POST   | `/ai/evaluate-quiz`                                  | Evaluate submitted answers         |
| POST   | `/ai/ask`                                            | Ask general academic questions     |

### 📁 Headers Required (For Protected Endpoints)

- Authorization: Bearer <your_jwt_token>
- Content-Type: application/json





