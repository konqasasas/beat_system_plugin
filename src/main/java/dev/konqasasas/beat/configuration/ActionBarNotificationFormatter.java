package dev.konqasasas.beat.configuration;

import java.util.Objects;
import java.util.regex.Pattern;

final class ActionBarNotificationFormatter {
    private static final String RESET = "\u00a7r";
    private static final String YELLOW = "\u00a7e";
    private static final String GREEN = "\u00a7a";
    private static final String AQUA = "\u00a7b";
    private static final String GRAY = "\u00a77";
    private static final Pattern YELLOW_LABELS = Pattern.compile("(?i)(?<![A-Za-z])(Time|Point|Progress)(?![A-Za-z])");
    private static final Pattern GREEN_LABELS = Pattern.compile("(?i)(?<![A-Za-z])(PB(?:更新!)?|Course|Zone)(?![A-Za-z])");
    private static final Pattern AQUA_LABELS = Pattern.compile("(?i)(?<![A-Za-z])(Split|Spot)(?![A-Za-z])");
    private static final Pattern RANK = Pattern.compile("#(?:\\d+|--)");

    private ActionBarNotificationFormatter() {
    }

    static String colorize(String message) {
        Objects.requireNonNull(message, "message");
        if (message.indexOf('\u00a7') >= 0 || containsAlternateColorCode(message)) {
            return message;
        }

        String colored = colorize(message, YELLOW_LABELS, YELLOW);
        colored = colorize(colored, GREEN_LABELS, GREEN);
        colored = colorize(colored, AQUA_LABELS, AQUA);
        colored = colorize(colored, RANK, AQUA);
        return colored.replace(" ｜ ", GRAY + " ｜ " + RESET);
    }

    private static String colorize(String message, Pattern pattern, String color) {
        return pattern.matcher(message).replaceAll(result -> color + result.group() + RESET);
    }

    private static boolean containsAlternateColorCode(String message) {
        for (int index = 0; index + 1 < message.length(); index++) {
            if (message.charAt(index) == '&'
                    && "0123456789abcdefklmnorx".indexOf(Character.toLowerCase(message.charAt(index + 1))) >= 0) {
                return true;
            }
        }
        return false;
    }
}
