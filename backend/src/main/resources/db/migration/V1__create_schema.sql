CREATE TABLE users (
    id CHAR(36) PRIMARY KEY,
    username VARCHAR(80) NOT NULL,
    email VARCHAR(180) NOT NULL,
    password_hash VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE refresh_tokens (
    id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE projects (
    id CHAR(36) PRIMARY KEY,
    name VARCHAR(140) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    start_date DATE NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE project_memberships (
    id CHAR(36) PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    role VARCHAR(30) NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_project_memberships_project_user UNIQUE (project_id, user_id),
    CONSTRAINT fk_project_memberships_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_project_memberships_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE tasks (
    id CHAR(36) PRIMARY KEY,
    project_id CHAR(36) NOT NULL,
    assignee_id CHAR(36) NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    due_date DATE NOT NULL,
    end_date DATE NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_tasks_project FOREIGN KEY (project_id) REFERENCES projects(id),
    CONSTRAINT fk_tasks_assignee FOREIGN KEY (assignee_id) REFERENCES users(id)
);

CREATE TABLE task_history_entries (
    id CHAR(36) PRIMARY KEY,
    task_id CHAR(36) NOT NULL,
    actor_id CHAR(36) NOT NULL,
    field_name VARCHAR(80) NOT NULL,
    old_value VARCHAR(2000) NULL,
    new_value VARCHAR(2000) NULL,
    changed_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_task_history_entries_task FOREIGN KEY (task_id) REFERENCES tasks(id),
    CONSTRAINT fk_task_history_entries_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_memberships_user ON project_memberships(user_id);
CREATE INDEX idx_tasks_project_status ON tasks(project_id, status);
CREATE INDEX idx_task_history_task ON task_history_entries(task_id);
