# Bug Tracking System (Spring Boot + MySQL + JWT)

Full-stack backend for a Bug Tracking System with JWT authentication and Role-Based Access Control (RBAC) for **Admin**, **Developer**, and **Tester** roles.

## Tech Stack
- Java 17, Spring Boot 3.3.2
- Spring Security + JWT (jjwt)
- Spring Data JPA + MySQL
- Maven
- Lombok

## Features
- User registration & JWT-based login
- Role-based access control (ADMIN / DEVELOPER / TESTER)
- Bug creation (Tester/Admin), assignment (Admin -> Developer)
- Multi-status workflow: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED (with REOPENED)
- Audit log for every bug action (created, assigned, status changed, deleted)
- RESTful APIs

## Setup

### 1. Create MySQL database
The app auto-creates the DB (`bug_tracking_db`) if it doesn't exist (see `createDatabaseIfNotExist=true` in the JDBC URL). Just make sure MySQL is running and update credentials in:

`src/main/resources/application.properties`
```
spring.datasource.username=root
spring.datasource.password=root
```

### 2. Build & Run
```bash
mvn clean install
mvn spring-boot:run
```
App runs on `http://localhost:8080`

## API Endpoints

### Auth (public)
| Method | Endpoint | Body |
|---|---|---|
| POST | `/api/auth/register` | `{ "username","email","password","fullName","role": "ADMIN|DEVELOPER|TESTER" }` |
| POST | `/api/auth/login` | `{ "username","password" }` -> returns JWT |

Use the returned JWT as: `Authorization: Bearer <token>` on all `/api/bugs/**` and `/api/users/**` calls.

### Bugs (JWT required)
| Method | Endpoint | Role | Description |
|---|---|---|---|
| POST | `/api/bugs` | TESTER, ADMIN | Report a new bug |
| GET | `/api/bugs` | any | List all bugs |
| GET | `/api/bugs/{id}` | any | Get bug details |
| GET | `/api/bugs/assigned-to-me` | DEVELOPER | Bugs assigned to me |
| GET | `/api/bugs/reported-by-me` | TESTER | Bugs I reported |
| GET | `/api/bugs/status/{status}` | any | Filter by OPEN/IN_PROGRESS/RESOLVED/REOPENED/CLOSED |
| PUT | `/api/bugs/{id}/assign` | ADMIN | `{ "developerId": 3 }` |
| PUT | `/api/bugs/{id}/status` | role-checked per transition | `{ "status": "RESOLVED" }` |
| DELETE | `/api/bugs/{id}` | ADMIN | Delete a bug |

### Users
| Method | Endpoint | Role |
|---|---|---|
| GET | `/api/users` | ADMIN |
| GET | `/api/users/developers` | any (used for assignment dropdown) |

## Status Workflow Rules
- **OPEN -> IN_PROGRESS**: Developer (assigned) or Admin
- **IN_PROGRESS -> RESOLVED**: Developer (assigned) or Admin
- **RESOLVED -> CLOSED**: Tester (reporter) or Admin
- **-> REOPENED**: Tester (reporter) or Admin
- **-> OPEN** (reset): Admin only

## Sample Flow
1. Register an ADMIN, a TESTER, and a DEVELOPER via `/api/auth/register`.
2. Login as TESTER, POST `/api/bugs` to report a bug.
3. Login as ADMIN, PUT `/api/bugs/{id}/assign` with the developer's id.
4. Login as DEVELOPER, PUT `/api/bugs/{id}/status` -> `IN_PROGRESS`, then `RESOLVED`.
5. Login as TESTER, PUT `/api/bugs/{id}/status` -> `CLOSED` (or `REOPENED` if not fixed).

## Project Structure
```
src/main/java/com/bugtracker/
 ├── entity/        # User, Bug, AuditLog, enums (Role, BugStatus, BugPriority)
 ├── repository/    # Spring Data JPA repositories
 ├── security/      # JWT util, filter, SecurityConfig, UserDetails
 ├── dto/           # Request/response DTOs
 ├── service/       # AuthService, BugService (business logic + audit log)
 ├── controller/    # AuthController, BugController, UserController
 └── exception/     # Global exception handling
```
