-- =============================================
-- Students
-- =============================================
INSERT INTO students (id, firstname, lastname, password, grade, section, roll, academic_year) VALUES
(1, 'John', 'Doe', '{noop}password', '10', 'A', '1','2026'),
(2, 'Jane', 'Smith', '{noop}password', '10', 'A', '2', '2026'),
(3, 'Aarav', 'Sharma', '{noop}password', '10', 'B', '1', '2026'),
(4, 'Maya', 'Thapa', '{noop}password', '10', 'A', '6', '2026');

-- =============================================
-- Teachers
-- =============================================
INSERT INTO teachers (firstname, lastname, username, password) VALUES
('First', 'Teacher', 'username', '{noop}password');

-- =============================================
-- Courses
-- =============================================
INSERT INTO courses (id, name, grade, academic_year) VALUES
(1, 'Mathematics', '10',  '2026'),
(2, 'Science', '10', '2026');

-- =============================================
-- Course ↔ Teacher
-- =============================================
INSERT INTO course_teacher (course_id, teacher_id) VALUES
(2, 1);

-- =============================================
-- Course ↔ Student
-- =============================================
INSERT INTO course_student (course_id, student_id) VALUES
(1, 1),
(1, 2),
(2, 1),
(2, 3);

-- =============================================
-- Decks  (Snowflake-style IDs)
-- =============================================
INSERT INTO decks (id, name, course_id) VALUES
(1, 'Algebra Basics', 1),
(2, 'Chemical Reactions', 2);

-- =============================================
-- Notes  (Snowflake-style IDs)
-- =============================================
INSERT INTO notes (id, front, back, additional_context) VALUES
(1, 'What is the quadratic formula?', 'x = (-b ± √(b²-4ac)) / 2a', 'Used to solve ax²+bx+c=0'),
(2, 'What is the slope-intercept form?', 'y = mx + b', 'm is slope, b is y-intercept'),
(3, 'What is an exothermic reaction?', 'A reaction that releases heat to the surroundings.', 'Example: combustion'),
(4, 'What is the chemical formula for water?', 'H₂O', 'Two hydrogen atoms and one oxygen atom');

-- =============================================
-- Note tags
-- =============================================
INSERT INTO note_tags (note_id, tag) VALUES
(1, 'algebra'),
(1, 'formula'),
(2, 'algebra'),
(2, 'linear'),
(3, 'thermochemistry'),
(4, 'basic');

-- =============================================
-- FlashCards  (Snowflake-style IDs)
-- =============================================
INSERT INTO flashcards (id, note_id, crt, type, queue, ivl, factor, reps, lapses, `left`, due, deck_id) VALUES
(1, 1, 1740873600, 'NEW', 'NEW', 0, 0, 0, 0, 0, 200000000001, 1),
(2, 2, 1740873600, 'NEW', 'NEW', 0, 0, 0, 0, 0, 200000000002, 1),
(3, 3, 1740873600, 'NEW', 'NEW', 0, 0, 0, 0, 0, 200000000003, 2),
(4, 4, 1740873600, 'NEW', 'NEW', 0, 0, 0, 0, 0, 200000000004, 2);
