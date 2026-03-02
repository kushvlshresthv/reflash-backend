CREATE TABLE students
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    firstname     VARCHAR(50)  NOT NULL,
    lastname      VARCHAR(50)  NOT NULL,
    password      VARCHAR(100) NOT NULL,
    grade         VARCHAR(10)  NOT NULL,
    section       VARCHAR(10) DEFAULT 'NONE',
    roll          VARCHAR(10)  NOT NULL,
    academic_year VARCHAR(10)  NOT NULL,
    CONSTRAINT unique_class_roll UNIQUE (grade, section, roll, academic_year)
);

CREATE TABLE teachers
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    firstname VARCHAR(50)  NOT NULL,
    lastname  VARCHAR(50)  NOT NULL,
    username  VARCHAR(50)  NOT NULL UNIQUE,
    password  VARCHAR(100) NOT NULL
);

CREATE TABLE courses
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    academic_year VARCHAR(10)  NOT NULL
);

CREATE TABLE course_teacher
(
    course_id  INT AUTO_INCREMENT NOT NULL,
    teacher_id INT NOT NULL,
    PRIMARY KEY (course_id, teacher_id),
    FOREIGN KEY (course_id) REFERENCES courses (id),
    FOREIGN KEY (teacher_id) REFERENCES teachers (id)
);

CREATE TABLE course_student
(
    course_id  INT AUTO_INCREMENT NOT NULL,
    student_id INT NOT NULL,
    PRIMARY KEY (course_id, student_id),
    FOREIGN KEY (course_id) REFERENCES courses (id),
    FOREIGN KEY (student_id) REFERENCES students (id)
);

CREATE TABLE decks
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    name      VARCHAR(255),
    course_id BIGINT,
    FOREIGN KEY (course_id) REFERENCES courses (id)
);

CREATE TABLE notes
(
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    front              TEXT,
    back               TEXT,
    additional_context TEXT
);

CREATE TABLE note_tags
(
    note_id INT          NOT NULL,
    tag     VARCHAR(100) NOT NULL,
    FOREIGN KEY (note_id) REFERENCES notes (id)
);

CREATE TABLE flashcards
(
    id      INT AUTO_INCREMENT PRIMARY KEY,
    note_id BIGINT,
    crt     BIGINT,
    type    VARCHAR(20),
    queue   VARCHAR(20),
    ivl     INT,
    factor  INT,
    reps    INT,
    lapses  INT,
    `left`  INT,
    due     BIGINT,
    deck_id INT,
    FOREIGN KEY (note_id) REFERENCES notes (id),
    FOREIGN KEY (deck_id) REFERENCES decks (id)
);
