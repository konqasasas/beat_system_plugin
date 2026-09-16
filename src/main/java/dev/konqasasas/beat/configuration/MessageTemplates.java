package dev.konqasasas.beat.configuration;

import java.util.Map;

final class MessageTemplates {
    private MessageTemplates() {}

    static String render(String template, Map<String, ?> placeholders) {
        String rendered = template;
        for (var entry : placeholders.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return rendered;
    }
}
