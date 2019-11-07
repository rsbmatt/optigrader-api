package dev.mahabal.optigrader.api.gson;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Random;

/**
 * @author Matthew
 */
@Data
@NoArgsConstructor
public final class RegisterRequest implements SerializableRequest {

    private String nid;
    private String firstName;
    private String lastName;
    private String login;
    private String password;
    private boolean teacher;

    public boolean validate() {
        return notNullAndNotEmpty(firstName, lastName, login, password);
    }

    public void generateNid() {
        this.nid = firstName.substring(0, Math.min(2, firstName.length())).toLowerCase() + new Random().nextInt(999999);
    }

    public String getNid() {
        if (nid == null) {
            this.generateNid();
        }
        return nid;
    }

}
