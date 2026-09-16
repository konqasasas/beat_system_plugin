package dev.konqasasas.beat.setup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class NumberRangeFormatter {
    private NumberRangeFormatter() {}

    public static String format(Collection<Integer> numbers) {
        List<Integer> sorted = numbers.stream().distinct().sorted().toList();
        if (sorted.isEmpty()) return "なし";
        List<String> ranges = new ArrayList<>();
        int start = sorted.getFirst();
        int previous = start;
        for (int index = 1; index < sorted.size(); index++) {
            int current = sorted.get(index);
            if (current == previous + 1) {
                previous = current;
                continue;
            }
            ranges.add(range(start, previous));
            start = current;
            previous = current;
        }
        ranges.add(range(start, previous));
        return String.join(", ", ranges);
    }

    public static String missing(Collection<Integer> numbers) {
        if (numbers.isEmpty()) return "なし";
        int maximum = numbers.stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Integer> missing = new ArrayList<>();
        for (int number = 1; number <= maximum; number++) {
            if (!numbers.contains(number)) missing.add(number);
        }
        return format(missing);
    }

    private static String range(int start, int end) {
        return start == end ? "%03d".formatted(start) : "%03d-%03d".formatted(start, end);
    }
}
