drop database if exists optigrader;
create database optigrader;
use optigrader;

CREATE TABLE IF NOT EXISTS users
(
  nid VARCHAR(8) NOT NULL,
  login VARCHAR(254) NULL,
  password CHAR(64) NULL,
  firstName VARCHAR(50) NULL,
  lastName VARCHAR(50) NULL,
  user_mode BIT(1) NOT NULL,
  dateCreated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  lastLoggedIn DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (nid)
);

CREATE TABLE IF NOT EXISTS sessions
(
  nid   VARCHAR(8)  NOT NULL,
  ip    VARCHAR(45) NOT NULL,
  token CHAR(64)    NOT NULL,
  dateCreated DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (nid),
  FOREIGN KEY (nid) REFERENCES users (nid)
);

CREATE TABLE IF NOT EXISTS tests
(
  code              CHAR(4)      NOT NULL,
  testName          VARCHAR(50)  NOT NULL,
  numberOfQuestions INT          NOT NULL,
  solutions         VARCHAR(500) NOT NULL,
  testOwner         VARCHAR(8)   NOT NULL,
  expiration        DATETIME     NOT NULL,
  PRIMARY KEY (code),
  FOREIGN KEY (testOwner) REFERENCES users (nid)
);

CREATE TABLE IF NOT EXISTS history
(
  id               INT unsigned NOT NULL AUTO_INCREMENT,
  nid              VARCHAR(8)   NOT NULL,
  testCode         VARCHAR(4)  NOT NULL,
  studentSolutions VARCHAR(500) NOT NULL,
  studentScore     INT NULL,
  submittedImage   BLOB NULL,
  submissionTime   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  FOREIGN KEY (nid) REFERENCES users (nid),
  FOREIGN KEY (testCode) REFERENCES tests (code)
);