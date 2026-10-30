# Calculator DevOps (SPE Mini-Project)

A simple Java calculator REST API (plain Java, no Spring Boot) with a full CI/CD pipeline:
GitHub → Jenkins → Docker Hub → Docker Compose.

## API

| Operation | Endpoint |
|-----------|----------|
| Add       | `GET /api/v1/add?a=2&b=3` |
| Subtract  | `GET /api/v1/subtract?a=2&b=3` |
| Multiply  | `GET /api/v1/multiply?a=2&b=3` |
| Divide    | `GET /api/v1/divide?a=6&b=3` (b=0 → HTTP 400) |
| Health    | `GET /actuator/health` → `{"status":"UP"}` |

Example response:

```json
{"operation":"add","a":2,"b":3,"result":5}
```

Errors (missing/invalid parameter, division by zero) return HTTP 400:

```json
{"status":400,"error":"Bad Request","message":"Division by zero is not allowed"}
```

## Build, test and run (local)

Requires Java 17+ and Maven.

```bash
mvn clean package        # build + run tests → target/calculator.jar
mvn test                 # tests only, reports in target/surefire-reports/
java -jar target/calculator.jar   # runs on port 8080 (override with PORT env var)
curl "http://localhost:8080/api/v1/add?a=2&b=3"
```

## Docker

```bash
docker build -t calculator-devops .
docker run -p 8080:8080 calculator-devops
```

## Docker Compose (app + Nginx reverse proxy)

```bash
docker compose up -d
curl "http://localhost:8081/api/v1/multiply?a=4&b=5"
docker compose down
```

- `calculator` – the API, with a health check on `/actuator/health`
- `nginx` – reverse proxy on port 8081, starts only after the API is healthy

## Jenkins pipeline

Stages: **Checkout → Build → Test → Docker Build → Push → Deploy**, with an email on success/failure.

Jenkins runs in WSL (Ubuntu) with Java 21, Maven and Docker Desktop's WSL integration
(`sudo usermod -aG docker jenkins` so Jenkins can use Docker).
It polls GitHub every 2 minutes (Poll SCM), since a local Jenkins can't receive GitHub webhooks.

Setup:
1. Install plugins: Git, Pipeline, JUnit, Mailer.
2. Add a *Username with password* credential with ID `dockerhub-creds` (Docker Hub username + access token).
3. Configure SMTP in *Manage Jenkins → System → E-mail Notification*.
4. In `Jenkinsfile`, set `DOCKER_IMAGE` to `<your-dockerhub-username>/calculator-devops`.
5. Create a *Pipeline* job → *Pipeline script from SCM* → this repo, branch `*/main`.
