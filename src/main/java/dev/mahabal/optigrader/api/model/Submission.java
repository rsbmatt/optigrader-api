package dev.mahabal.optigrader.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author Matthew
 */
@NoArgsConstructor
@Data
public final class Submission {

    public int id;
    public String nid;
    public String testCode;
    public String studentSolutions;
    public int studentScore;
    public String submittedImage;
    public Instant submissionTime;

}
