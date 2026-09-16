package dev.konqasasas.beat.roster;

public final class RosterException extends Exception {
    public RosterException(String message) {
        super(message);
    }

    public RosterException(String message, Throwable cause) {
        super(message, cause);
    }
}
