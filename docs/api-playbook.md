# API Playbook

## 0. Auth

Register:

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "demo",
  "password": "demo123"
}
```

Login:

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "demo",
  "password": "demo123"
}
```

Current user:

```http
GET /api/auth/me
Authorization: Bearer <token>
```

Logout:

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

Notes:

- logged-in users keep profile, report snapshot, and chat memory
- guest mode does not persist memory
- default admin account is controlled by `app.admin.username` and `app.admin.password`

## 1. Import normalized jobs

Import a normalized `jsonl` file into both MySQL and Neo4j.

```http
POST /api/data/import-normalized-jobs?path=E:\path\to\normalized.jsonl
```

Expected result:

- total record count
- valid record count
- MySQL upsert count
- Neo4j upsert count

## 2. Query jobs

After normalized data is imported, the existing job APIs prefer imported MySQL jobs over the local static knowledge files.

```http
GET /api/jobs
GET /api/jobs/{jobId}
GET /api/jobs/search/similar?query=java
```

## 3. Query graph similar jobs

```http
GET /api/graph/similar-jobs?job_title=Java%20Backend%20Engineer&limit=5
```

This returns:

- similar jobs
- shared skill list
- similarity score

## 4. Query graph skill gap

```http
POST /api/graph/skill-gap
Content-Type: application/json

{
  "targetJob": "Java Backend Engineer",
  "studentProfile": {
    "student_id": "demo",
    "basic_info": {
      "name": "Alice",
      "education": "本科",
      "major": "软件工程",
      "school": "ZUST",
      "graduation_year": "2026"
    },
    "skills": ["Java", "Spring Boot", "MySQL"],
    "certificates": [],
    "soft_abilities": {
      "innovation": 70,
      "learning": 80,
      "stress_tolerance": 70,
      "communication": 75,
      "professional_skills": 80,
      "certificates": 50,
      "internship": 60
    },
    "job_preference": {
      "expected_position": "Java Backend Engineer",
      "expected_salary": "15K-25K",
      "expected_city": "杭州"
    }
  }
}
```

This returns:

- matched skills
- missing skills
- skill match score
- summary text

## 5. AI chat enhancement

The AI chat endpoint now supports context enrichment.
If the frontend sends a student profile context with:

- `basicInfo`
- `skills`
- `jobPreference.expectedPosition`

the backend can append graph-based job insight before sending the message to the LLM.

```http
POST /api/ai/chat
```

The current frontend already sends:

- `basicInfo`
- `skills`
- `jobPreference`

so once graph data exists, chat quality should improve automatically.

## 6. Report snapshot recovery

Latest snapshot by student and target job:

```http
GET /api/report/snapshot/latest?student_id=<studentId>&target_job=<targetJob>
```

History for one student:

```http
GET /api/report/snapshot/history/{studentId}
```

Manual save for edited report content:

```http
POST /api/report/snapshot/save
Content-Type: application/json
```

Snapshots are updated automatically when:

- a new report is generated
- a new growth plan is generated

## 7. Admin enterprise management

List imported jobs:

```http
GET /api/admin/jobs
Authorization: Bearer <admin-token>
```

Update one job:

```http
PUT /api/admin/jobs/{sourceId}
Authorization: Bearer <admin-token>
Content-Type: application/json
```
