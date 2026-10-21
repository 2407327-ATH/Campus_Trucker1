package com.northrise.campustrack.data.repo;

public class AuthResult {
    public enum State { LOADING, SUCCESS, ERROR }

    public final State state;
    public final String message;

    private AuthResult(State state, String message) {
        this.state = state;
        this.message = message;
    }

    public static AuthResult loading() {
        return new AuthResult(State.LOADING, null);
    }

    public static AuthResult ok() {
        return new AuthResult(State.SUCCESS, null);
    }

    public static AuthResult error(String message) {
        return new AuthResult(State.ERROR, message);
    }
}
