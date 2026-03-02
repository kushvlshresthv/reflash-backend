CREATE TABLE students
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    firstname VARCHAR(50)  NOT NULL,
    lastname  VARCHAR(50)  NOT NULL,
    password  VARCHAR(100) NOT NULL
);

CREATE TABLE enrollments
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    student_id   INT         NOT NULL,
    grade        VARCHAR(10) NOT NULL,
    section      VARCHAR(10) DEFAULT 'NONE',
    roll         VARCHAR(10) NOT NULL,
    academic_year VARCHAR(10) NOT NULL,

    CONSTRAINT fk_enrollment_student
        FOREIGN KEY (student_id)
            REFERENCES students (id)
            ON DELETE CASCADE
            ON UPDATE CASCADE,

    CONSTRAINT unique_class_roll UNIQUE (grade, section, roll, academic_year)
);

CREATE TABLE teachers
(
    id        INT AUTO_INCREMENT PRIMARY KEY,
    firstname VARCHAR(50)  NOT NULL,
    lastname  VARCHAR(50)  NOT NULL,
    username  VARCHAR(50)  NOT NULL,
    password  VARCHAR(100) NOT NULL
);
