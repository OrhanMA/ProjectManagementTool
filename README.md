# PMT - Project Management Tool

PMT est une plateforme collaborative de gestion de projet pour équipes de développement logiciel.

## Stack

- Frontend: Angular 21, Angular Material, Jest, Testing Library, Playwright
- Backend: Java 21, Spring Boot 3.5, Gradle, Spring Security, OpenAPI
- Base de données: MySQL 8.4 LTS avec migrations Flyway
- Emails locaux: Mailpit
- CI/CD: GitHub Actions
- Déploiement local: Docker Compose

## Principes de développement

- Documentation en français, code en anglais.
- Développement feature par feature.
- TDD strict: test rouge, implémentation minimale, refactor, test vert.
- Couverture cible: 80% minimum en instructions et branches.
- Code lisible, maintenable, sans raccourcis de projet scolaire.
- Commits au format Conventional Commits.

## Lancement local pour développer

Démarrer les dépendances:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Backend:

```bash
cd backend
./gradlew bootRun
```

Frontend:

```bash
cd frontend
npm install
npm start
```

URLs:

- Frontend: http://localhost:4200
- API: http://localhost:8080/api/v1
- Swagger: http://localhost:8080/swagger-ui.html
- Mailpit: http://localhost:8025

## Lancement Docker Compose complet

```bash
docker compose up --build
```

Si un service local occupe déjà un port, surchargez uniquement le port concerné:

```bash
PMT_MYSQL_PORT=3307 docker compose up --build
```

## Tests

Backend:

```bash
cd backend
./gradlew check
```

Frontend:

```bash
cd frontend
npm test -- --coverage
```

E2E:

```bash
cd frontend
npx playwright test
```

## Comptes de démonstration

Les données de démo sont créées par Flyway. Le mot de passe cible pour les comptes de démo est `Password123!`.

- `alice.admin@pmt.local`
- `marc.member@pmt.local`
- `olivia.observer@pmt.local`

## API principale

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `POST /api/v1/projects`
- `GET /api/v1/projects`
- `GET /api/v1/projects/{projectId}`
- `POST /api/v1/projects/{projectId}/members`
- `PATCH /api/v1/projects/{projectId}/members/{userId}/role`
- `POST /api/v1/projects/{projectId}/tasks`
- `GET /api/v1/projects/{projectId}/tasks`
- `GET /api/v1/projects/{projectId}/tasks/{taskId}`
- `PATCH /api/v1/projects/{projectId}/tasks/{taskId}`
- `PATCH /api/v1/projects/{projectId}/tasks/{taskId}/assignee`
- `GET /api/v1/projects/{projectId}/tasks/{taskId}/history`

## Variables CI/CD

Secrets GitHub Actions attendus:

- `DOCKERHUB_USERNAME`
- `DOCKERHUB_TOKEN`
- `JWT_SECRET`

Images publiées:

- `${DOCKERHUB_USERNAME}/pmt-frontend`
- `${DOCKERHUB_USERNAME}/pmt-backend`

Le push Docker Hub se fait sur `main` et les tags SemVer `v*.*.*`.
