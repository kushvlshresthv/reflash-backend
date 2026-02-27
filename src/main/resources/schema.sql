CREATE TABLE app_users
(
    uid       INT AUTO_INCREMENT PRIMARY KEY,
    firstname VARCHAR(50) NOT NULL,
    lastname  VARCHAR(50) NOT NULL,
    grade  VARCHAR(50) NOT NULL,
    section VARCHAR(50),
    roll VARCHAR(50) NOT NULL,
    password  VARCHAR(100) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    CONSTRAINT unique_class_roll UNIQUE(grade, section, roll)
);
