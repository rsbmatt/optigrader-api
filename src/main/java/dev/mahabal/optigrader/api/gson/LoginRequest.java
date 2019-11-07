package dev.mahabal.optigrader.api.gson;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public final class LoginRequest implements SerializableRequest {

    private String login;
    private String password;
    private String token;

    public boolean validate() {

        return notNullAndNotEmpty(login, password) || notNullAndNotEmpty(token);

    }

}

