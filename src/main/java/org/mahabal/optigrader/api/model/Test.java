package org.mahabal.optigrader.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * @author Matthew
 */
@NoArgsConstructor
@Data
public final class Test {

    public static int CODE_LENGTH = 4;

    public String testName;
    public String code;
    public int numberOfQuestions;
    public String solutions;
    public String testOwner;
    public Instant expiration;

}
