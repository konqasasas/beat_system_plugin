package dev.konqasasas.beat.application;

import dev.konqasasas.beat.configuration.CompetitionSettings;
import dev.konqasasas.beat.configuration.CompetitionSettingsService;
import dev.konqasasas.beat.roster.RegisteredIdentity;
import dev.konqasasas.beat.roster.RosterService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/** Per-admin confirmations bound to the exact settings and participant roster inspected. */
public final class HighStartPreflightService {
    private final Supplier<CompetitionSettings> settings;
    private final Supplier<Map<UUID, RegisteredIdentity>> participants;
    private final Map<UUID, Confirmation> confirmations = new HashMap<>();

    public HighStartPreflightService(CompetitionSettingsService settings, RosterService rosters) {
        this(settings::current, () -> rosters.current().participants());
    }

    public HighStartPreflightService(Supplier<CompetitionSettings> settings,
            Supplier<Map<UUID, RegisteredIdentity>> participants) {
        this.settings = settings;
        this.participants = participants;
    }

    public void confirmSettings(UUID admin) {
        Confirmation current = confirmations.getOrDefault(admin, Confirmation.empty());
        confirmations.put(admin, new Confirmation(settings.get(), current.rosterFingerprint(), true,
                current.participantsConfirmed()));
    }

    public void confirmParticipants(UUID admin) {
        Confirmation current = confirmations.getOrDefault(admin, Confirmation.empty());
        confirmations.put(admin, new Confirmation(current.settings(), rosterFingerprint(),
                current.settingsConfirmed(), true));
    }

    public Status status(UUID admin) {
        Confirmation value = confirmations.getOrDefault(admin, Confirmation.empty());
        boolean settingsCurrent = value.settingsConfirmed() && settings.get().equals(value.settings());
        boolean rosterCurrent = value.participantsConfirmed() && rosterFingerprint().equals(value.rosterFingerprint());
        return new Status(settingsCurrent, rosterCurrent);
    }

    public void requireConfirmed(UUID admin) {
        Status status = status(admin);
        if (!status.ready()) throw new IllegalStateException("競技設定と参加者の確認が完了していません");
    }

    public void clear() { confirmations.clear(); }

    private String rosterFingerprint() {
        return participants.get().values().stream()
                .sorted(Comparator.comparing(RegisteredIdentity::uuid))
                .map(identity -> identity.uuid() + "=" + identity.mcid())
                .reduce("", (left, right) -> left + "\n" + right);
    }

    public record Status(boolean settingsConfirmed, boolean participantsConfirmed) {
        public boolean ready() { return settingsConfirmed && participantsConfirmed; }
    }

    private record Confirmation(CompetitionSettings settings, String rosterFingerprint,
            boolean settingsConfirmed, boolean participantsConfirmed) {
        private static Confirmation empty() { return new Confirmation(null, null, false, false); }
    }
}
