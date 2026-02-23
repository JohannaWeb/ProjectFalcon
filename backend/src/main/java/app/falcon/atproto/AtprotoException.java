package app.falcon.atproto;

import lombok.Getter;

@Getter
public class AtprotoException extends RuntimeException {

    private final int statusCode;
    private final String body;

    public AtprotoException(int statusCode, String body) {
        super("AT Protocol error " + statusCode + ": " + body);
        this.statusCode = statusCode;
        this.body = body;
    }
}
