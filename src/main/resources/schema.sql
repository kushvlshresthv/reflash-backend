CREATE TABLE app_users
(
    uid       INT AUTO_INCREMENT PRIMARY KEY,
    firstname VARCHAR(50),
    lastname  VARCHAR(50),
    username  VARCHAR(50) UNIQUE,
    email     VARCHAR(100),
    password  VARCHAR(100) NOT NULL,
    timezone  VARCHAR(64)  NOT NULL,
    level     INT          NOT NULL,
    xp        INT          NOT NULL
);
