# optigrader-api

OptiGrader uses a RESTful web service for it's API. The underlying system is Java and it uses JSON for transferring
payloads.

## Under the Hood

- API connections are done via `https://domain.com:8080`
- Handlers are accessed via one of:
    - `/register`  for  [RegistrationHandler](src/main/java/org/mahabal/optigrader/api/handler/RegistrationHandler.java)
    - `/login`  for [LoginHandler](src/main/java/org/mahabal/optigrader/api/handler/LoginHandler.java)
    - `/test`  for [TestHandler](src/main/java/org/mahabal/optigrader/api/handler/TestHandler.java)
    - `/admin`  for [AdminHandler](src/main/java/org/mahabal/optigrader/api/handler/AdminHandler.java)
- Payloads must be sent as proper JSON objects that can be serialized to their appropriate models
    -  [RegisterRequest](src/main/java/org/mahabal/optigrader/api/gson/RegisterRequest.java)
    -  [LoginRequest](src/main/java/org/mahabal/optigrader/api/gson/LoginRequest.java)
- Data is retrieved from the database via their corresponding data access objects (DAOs)
    - [Sessions](src/main/java/org/mahabal/optigrader/api/dao/SessionDao.java)
      - Also has a `create` method for automatically verifying a User and inserting the session into the table
    - [Submissions](src/main/java/org/mahabal/optigrader/api/dao/SubmissionDao.java)
    - [Tests](src/main/java/org/mahabal/optigrader/api/dao/TestDao.java)
    - [Users](src/main/java/org/mahabal/optigrader/api/dao/UserDao.java)
      - Also has a `login` method for validating a username and password hash
- The SQL queries backing the methods in the data access objects can be found as resources: [here](src/main/resources/org/mahabal/optigrader/api/dao/). 

## Security

- The API only accepts **secure**  requests over HTTPS
  - A private pkcs12 keystore is required (LetsEncrypt works fine)
- All sensitive data such as IP addresses and passwords are hashed and salted before
storage
- All input is sanitized using proven methods to guard against SQLi attacks



## Built With
- [JDBI 3](https://github.com/jdbi/jdbi) - Provides fluent, convenient, idiomatic access to relational data in Java
- [Jetty](https://github.com/eclipse/jetty.project) - Used for creating the servlet
- [HikariCP](https://github.com/brettwooldridge/HikariCP) - Lightweight and fast JDBC connection pool
- [MariaDB](https://mariadb.org) - Open source, better performing drop in replacement for MySQL
- [Guava](https://github.com/google/guava) - Google collections
- [Gson](https://github.com/google/gson) - Google's open source library for easy (de)?serialization of payloads
- [Lombok](https://github.com/rzwitserloot/lombok) - Very spicy additions to Java (via annotation processing

## Authors 

- **Matthew Balwant** - [GitLab](https://mahabal.dev/explore/projects) | [GitHub](https://github.com/rsbmatt)