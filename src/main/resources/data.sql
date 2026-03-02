-- =========================
-- STUDENTS
-- =========================

INSERT INTO students (id, firstname, lastname, password) VALUES
(1, 'John', 'Doe', '{noop}password'),
(2, 'Jane', 'Smith', '{noop}password'),
(3, 'Aarav', 'Sharma', '{noop}password'),
(4, 'Maya', 'Thapa', '{noop}password');


-- =========================
-- ENROLLMENTS (2026)
-- =========================

INSERT INTO enrollments (student_id, grade, section, roll, academic_year) VALUES
(1, '10', 'A', '1', '2026'),
(2, '10', 'A', '2', '2026'),
(3, '10', 'B', '1', '2026'),
(4, '9',  'A', '5', '2026');


-- =========================
-- ENROLLMENTS (2027 - Promotion Example)
-- =========================

INSERT INTO enrollments (student_id, grade, section, roll, academic_year) VALUES
(1, '11', 'A', '3', '2027'),
(2, '11', 'B', '1', '2027');


INSERT INTO teachers (firstname, lastname, username, password) VALUES
('First', 'Teacher', 'username', '{noop}password');
