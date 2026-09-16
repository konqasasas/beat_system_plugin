package dev.konqasasas.beat.application;

public record ParticipantLoginResult(
        Role role,
        String tournamentName,
        String registeredMcid,
        boolean registeredNameMismatch) {
    public enum Role {
        PARTICIPANT,
        ADMIN,
        UNREGISTERED
    }
}
