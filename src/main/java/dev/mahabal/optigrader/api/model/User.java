package dev.mahabal.optigrader.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Matthew
 */
@NoArgsConstructor
@Data
public final class User {

    public String nid;
    public int user_mode;
    public String firstName;
    public String lastName;
    public String login;
    public String password;        // not actually plain-text password, just a hash

    public boolean isTeacher() {
        return user_mode == 1;
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

}
