package com.northrise.campustrack.data.repo;

public class ActionResult {
    public final boolean ok;
    public final String message;
    public final long id;

    private ActionResult(boolean ok, String message, long id) {
        this.ok = ok;
        this.message = message;
        this.id = id;
    }

    public static ActionResult ok(long id, String message) {
        return new ActionResult(true, message, id);
    }

    public static ActionResult fail(String message) {
        return new ActionResult(false, message, 0);
    }
}
