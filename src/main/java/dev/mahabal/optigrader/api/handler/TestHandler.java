package dev.mahabal.optigrader.api.handler;

import com.google.common.util.concurrent.AtomicDouble;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.mahabal.optigrader.api.dao.TestDao;
import dev.mahabal.optigrader.api.model.Session;
import dev.mahabal.optigrader.api.model.Submission;
import dev.mahabal.optigrader.api.model.Test;
import dev.mahabal.optigrader.api.model.User;
import org.jdbi.v3.core.Jdbi;
import dev.mahabal.optigrader.api.dao.SessionDao;
import dev.mahabal.optigrader.api.dao.SubmissionDao;
import dev.mahabal.optigrader.api.dao.UserDao;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class TestHandler extends AbstractHandler {

    private static final Pattern VALID_SOLUTION = Pattern.compile("[A-E]+");

    public TestHandler(final Jdbi dbi) {
        super(dbi, "TestHandler");
    }

    public static double grade(final String answers, final String submission) {
        final double min = Math.min(answers.length(), submission.length());
        double score = 0.0d;
        for (int i = 0; i < min; ++i) {
            if (answers.charAt(i) == submission.charAt(i))
                score++;
        }
        return score / answers.length();
    }

    protected void handleRequest(HttpServletRequest req, HttpServletResponse resp, String ip, JsonElement payload) throws Exception {

        try {
            if (!payload.isJsonObject()) {
                badRequest(resp, "JSON payload must be a valid JSON object.");
                return;
            }

            final JsonObject json = payload.getAsJsonObject();
            if (!json.has("token")) {
                badRequest(resp, "Session token is missing.");
                return;
            }

            final String token = json.get("token").getAsString();
            final Session session = dbi.withExtension(SessionDao.class, dao -> dao.getSessionByToken(token, ip).orElse(null));
            if (session == null) {
                badRequest(resp, "Invalid session.");
                return;
            }

            final User user = dbi.withExtension(UserDao.class, dao -> dao.getUserByNid(session.nid).orElse(null));
            if (user == null) {
                log.warn("Unable to find a matching user for NID: {}", session.getNid());
                badRequest(resp, "Unable to locate user for associated token.");
                return;
            }

            final JsonObject object = new JsonObject();

            final String action = json.has("action") ? json.get("action").getAsString().toLowerCase() : "get";
            switch (action) {

                case "create":
                    if (!user.isTeacher()) {
                        error(resp, HttpServletResponse.SC_FORBIDDEN, "Only teachers can create tests.");
                        return;
                    } else {
                        if (!json.has("testName")) {
                            badRequest(resp, "No test name specified.");
                            return;
                        }
                        if (!json.has("solutions")) {
                            badRequest(resp, "No solutions specified.");
                            return;
                        }
                        final String testName = json.get("testName").getAsString()
                                .replaceAll("[^\\w\\s]+", " ")
                                .replaceAll("\\s+", " ")
                                .trim();
                        final String testSolutions = json.get("solutions").getAsString();
                        if (testName.isEmpty() || testName.isBlank()) {
                            badRequest(resp, "Test name cannot be empty!");
                            return;
                        }
                        if (testSolutions.isEmpty() || testSolutions.isBlank()) {
                            badRequest(resp, "Test solutions cannot be empty!");
                            return;
                        }
                        if (!VALID_SOLUTION.matcher(testSolutions).matches()) {
                            badRequest(resp, "Test solutions can only contain the characters: A,B,C,D,E");
                            return;
                        }
                        StringBuilder code = new StringBuilder();
                        for (int i = 0; i < Test.CODE_LENGTH; ++i) {
                            code.append((char) ('A' + new Random().nextInt((int) 'Z' - (int) 'A')));
                        }

                        final Test test = new Test();
                        test.code = code.toString();
                        test.expiration = Instant.now().plus(1, ChronoUnit.HOURS);
                        test.numberOfQuestions = testSolutions.length();
                        test.testName = testName;
                        test.solutions = testSolutions.toUpperCase();
                        test.testOwner = user.nid;
                        dbi.useExtension(TestDao.class, dao -> dao.addTest(test));
                        log.debug("Created test: {} for {}, code: {}", testName, user.fullName(), code);
                        object.addProperty("code", code.toString());
                        object.addProperty("status", "Test created successfully!");

                    }
                    break;

                case "delete": {
                    if (!user.isTeacher()) {
                        error(resp, HttpServletResponse.SC_FORBIDDEN, "Only teachers can delete tests.");
                        return;
                    }
                    if (!json.has("testCode")) {
                        badRequest(resp, "JSON is missing property 'testCode'");
                        return;
                    }
                    final String testCode = json.get("testCode").getAsString();
                    final Optional<Test> _test = dbi.withExtension(TestDao.class, dao -> dao.getTestByCode(testCode));
                    if (_test.isEmpty()) {
                        badRequest(resp, "Unable to delete test: test does not exist.");
                        return;
                    }
                    final Test test = _test.get();
                    if (user.nid != null && !user.nid.equals(test.testOwner)) {
                        error(resp, HttpServletResponse.SC_FORBIDDEN, "Wow, you can't delete tests that you didn't create...");
                        return;
                    }
                    dbi.useExtension(TestDao.class, dao -> dao.deleteTestByCode(testCode));
                    break;
                }

                case "submit":
                    if (user.isTeacher()) {
                        error(resp, HttpServletResponse.SC_FORBIDDEN, "Teachers are not allowed to submit tests.");
                        return;
                    }
                    if (!json.has("testCode")) {
                        badRequest(resp, "Missing JSON property \"testCode\"");
                        return;
                    }
                    if (!json.has("solutions")) {
                        badRequest(resp, "Missing JSON property \"solutions\"");
                        return;
                    }

                    final String testCode = json.get("testCode").getAsString();
                    final String solutions = json.get("solutions").getAsString();
                    dbi.withExtension(TestDao.class, dao -> dao.getTestByCode(testCode)).ifPresentOrElse(test -> {
                                double grade = grade(test.solutions, solutions);
                                int score = (int) (grade * 100d);
                                final Submission submission = new Submission();
                                submission.nid = user.nid;
                                submission.studentSolutions = solutions;
                                submission.studentScore = score;
                                submission.testCode = test.code;
                                dbi.useExtension(SubmissionDao.class, dao -> dao.addSubmission(submission));
                                log.info("User: '{}' [{}] has submitted solutions for test: '{}'.",
                                        user.fullName(),
                                        user.getNid(),
                                        testCode);
                                object.addProperty("status", "Test received.");
                            },
                            () -> {
                                log.warn("User: '{}' [{}] submitted solutions for invalid test: '{}'.",
                                        user.fullName(), user.getNid(), testCode);
                                object.addProperty("error", "No test found for code: \"" + testCode + "\"");
                            });
                    break;

                default:
                    log.debug("Looking up all tests for: {}", user.fullName());
                    // defaulting to "get"
                    if (user.isTeacher()) {
                        final JsonArray tests = new JsonArray();
                        // if the user is a teacher, iterate though all tests find tests we own
                        List<Test> ownedTests = dbi.withExtension(TestDao.class, dao -> dao.getTestsOwnedByNid(user.nid));
                        log.debug("they are a teacher with: {} tests.", ownedTests.size());
                        for (final Test t : ownedTests) {
                            final JsonObject test = new JsonObject();
                            test.addProperty("name", t.testName);
                            test.addProperty("id", t.code);
                            final AtomicInteger lowest = new AtomicInteger(-1);
                            final AtomicInteger highest = new AtomicInteger(0);
                            final AtomicDouble total = new AtomicDouble(0);
                            final AtomicDouble count = new AtomicDouble(0);
                            final JsonArray students = new JsonArray();
                            dbi.withExtension(SubmissionDao.class, dao -> dao.getSubmissionsForTestCode(t.code)).forEach(submission -> {
                                final User s = dbi.withExtension(UserDao.class, dao -> dao.getUserByNid(submission.nid).orElse(null));
                                if (s == null) return;
                                final JsonObject student = new JsonObject();
                                student.addProperty("name", s.fullName());
                                student.addProperty("id", s.getNid());
                                final int score = submission.getStudentScore();
                                student.addProperty("score", score);
                                student.addProperty("submitted", submission.getStudentSolutions());
                                students.add(student);
                                if (lowest.get() == -1) {
                                    lowest.set(score);
                                } else {
                                    lowest.set(Math.min(lowest.get(), score));
                                }
                                highest.set(Math.max(highest.get(), score));
                                total.addAndGet(score);
                                count.addAndGet(1.0d);
                            });
                            final int average = (int) (total.get() / count.get());
                            test.addProperty("avgScore", average);
                            test.addProperty("highScore", highest.get());
                            test.addProperty("lowScore", lowest.get());
                            test.addProperty("numQuestions", t.numberOfQuestions);
                            test.addProperty("solutions", t.solutions);
                            test.add("inner", students);
                            tests.add(test);
                        }
                        ok(resp, tests);
                        return;
                    } else {
                        log.debug("\t... they are a student!");
                        final JsonObject response = new JsonObject();
                        response.addProperty("name", user.fullName());
                        response.addProperty("id", user.nid);
                        final JsonArray submissions = new JsonArray();
                        final List<Submission> _submissions = dbi.withExtension(SubmissionDao.class, dao -> dao.getSubmissionsForNid(user.getNid()));
                        for (final Submission _submission : _submissions) {
                            final Optional<Test> _test = dbi.withExtension(TestDao.class, dao -> dao.getTestByCode(_submission.testCode));
                            if (_test.isEmpty()) {
                                log.warn("Invalid testCode: {} for user {}", _submission.testCode, user.fullName());
                                continue;
                            }
                            final Test test = _test.get();
                            final JsonObject submission = new JsonObject();
                            submission.addProperty("name", test.getTestName());
                            submission.addProperty("id", test.getCode());
                            submission.addProperty("avgScore", 0);
                            submission.addProperty("highScore", 0);
                            submission.addProperty("lowScore", 0);
                            submission.addProperty("score", _submission.getStudentScore());
                            submission.addProperty("solutions", test.solutions);
                            submission.addProperty("numQuestions", test.numberOfQuestions);
                            submission.addProperty("submitted", _submission.studentSolutions);
                            submissions.add(submission);
                        }
                        response.add("inner", submissions);
                        ok(resp, response);
                        return;
                    }

            }
            log.trace("Responding to: {}, '{}'.", user.fullName(), object);
            ok(resp, object);
        } catch (final Throwable t) {

            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, t.getMessage());

        }

    }


}
