package dev.mahabal.optigrader.api.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@Data
public final class Session {

    public String nid;
    public String ip;
    public String token;
    public Instant dateCreated;

}
