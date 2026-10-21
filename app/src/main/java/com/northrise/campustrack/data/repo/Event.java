package com.northrise.campustrack.data.repo;

public class Event<T> {
    private final T data;
    private boolean handled;

    public Event(T data) {
        this.data = data;
    }

    public T getIfNew() {
        if (handled) {
            return null;
        }
        handled = true;
        return data;
    }
}
