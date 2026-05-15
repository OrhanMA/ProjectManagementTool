INSERT INTO users (id, username, email, password_hash, created_at) VALUES
('11111111-1111-1111-1111-111111111111', 'alice.admin', 'alice.admin@pmt.local', '$2y$10$DLcPtVqwCG0Z9gOPS9gvlO8eaHy98Hn32Na8upGNNIIYyVWgPxrSa', CURRENT_TIMESTAMP(6)),
('22222222-2222-2222-2222-222222222222', 'marc.member', 'marc.member@pmt.local', '$2y$10$DLcPtVqwCG0Z9gOPS9gvlO8eaHy98Hn32Na8upGNNIIYyVWgPxrSa', CURRENT_TIMESTAMP(6)),
('33333333-3333-3333-3333-333333333333', 'olivia.observer', 'olivia.observer@pmt.local', '$2y$10$DLcPtVqwCG0Z9gOPS9gvlO8eaHy98Hn32Na8upGNNIIYyVWgPxrSa', CURRENT_TIMESTAMP(6));

INSERT INTO projects (id, name, description, start_date, created_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Refonte portail client', 'Projet de demonstration PMT avec un workflow complet.', CURRENT_DATE, CURRENT_TIMESTAMP(6));

INSERT INTO project_memberships (id, project_id, user_id, role, joined_at) VALUES
('aaaa0001-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 'ADMINISTRATOR', CURRENT_TIMESTAMP(6)),
('aaaa0002-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-2222-2222-2222-222222222222', 'MEMBER', CURRENT_TIMESTAMP(6)),
('aaaa0003-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '33333333-3333-3333-3333-333333333333', 'OBSERVER', CURRENT_TIMESTAMP(6));

INSERT INTO tasks (id, project_id, assignee_id, name, description, due_date, end_date, priority, status, created_at, updated_at) VALUES
('bbbb0001-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '22222222-2222-2222-2222-222222222222', 'Concevoir le modele de donnees', 'Produire le schema relationnel et les migrations Flyway.', DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY), NULL, 'HIGH', 'DOING', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
('bbbb0002-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', NULL, 'Preparer le dashboard', 'Construire le tableau Kanban par statut.', DATE_ADD(CURRENT_DATE, INTERVAL 14 DAY), NULL, 'MEDIUM', 'BACKLOG', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

INSERT INTO task_history_entries (id, task_id, actor_id, field_name, old_value, new_value, changed_at) VALUES
('cccc0001-cccc-cccc-cccc-cccccccccccc', 'bbbb0001-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '11111111-1111-1111-1111-111111111111', 'assignee', NULL, 'marc.member@pmt.local', CURRENT_TIMESTAMP(6));
