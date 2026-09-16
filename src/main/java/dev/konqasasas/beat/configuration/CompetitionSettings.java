package dev.konqasasas.beat.configuration;

import java.util.List;

public record CompetitionSettings(
        long startCountdownTicks,
        long highPracticeTicks,
        long highPrepareTicks,
        long highRunningTicks,
        List<Integer> highEliminationTicks,
        long timeAttackRunningTicks,
        List<Integer> timeAttackEliminationTicks,
        List<Integer> timeAttackSurvivorCounts,
        long enduranceRunningTicks,
        List<Integer> enduranceEliminationTicks) {

    public CompetitionSettings {
        highEliminationTicks = List.copyOf(highEliminationTicks);
        timeAttackEliminationTicks = List.copyOf(timeAttackEliminationTicks);
        timeAttackSurvivorCounts = List.copyOf(timeAttackSurvivorCounts);
        enduranceEliminationTicks = List.copyOf(enduranceEliminationTicks);
        validate(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
    }

    public static CompetitionSettings production() {
        return new CompetitionSettings(200, 12_000, 1_200, 36_000,
                List.of(12_000, 18_000, 24_000, 30_000),
                36_000, List.of(24_000, 30_000), List.of(20, 10),
                36_000, List.of(24_000, 30_000));
    }

    public static CompetitionSettings testPreset() {
        return new CompetitionSettings(200, 6_000, 600, 18_000,
                List.of(6_000, 9_000, 12_000, 15_000),
                18_000, List.of(12_000, 15_000), List.of(20, 10),
                18_000, List.of(12_000, 15_000));
    }

    public CompetitionSettings with(String key, long value) {
        if (value < 0 || value > Integer.MAX_VALUE) throw new IllegalArgumentException("値が範囲外です: " + value);
        int intValue = (int) value;
        return switch (key) {
            case "start-countdown" -> copy(value, highPracticeTicks, highPrepareTicks, highRunningTicks,
                    highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                    timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
            case "high-practice" -> copy(startCountdownTicks, value, highPrepareTicks, highRunningTicks,
                    highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                    timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
            case "high-prepare" -> copy(startCountdownTicks, highPracticeTicks, value, highRunningTicks,
                    highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                    timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
            case "high-total" -> copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, value,
                    highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                    timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
            case "ta-total" -> copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                    highEliminationTicks, value, timeAttackEliminationTicks,
                    timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
            case "endurance-total" -> copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                    highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                    timeAttackSurvivorCounts, value, enduranceEliminationTicks);
            case "high-elimination-1", "high-elimination-2", "high-elimination-3", "high-elimination-4" ->
                    copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                            replace(highEliminationTicks, index(key), intValue), timeAttackRunningTicks,
                            timeAttackEliminationTicks, timeAttackSurvivorCounts,
                            enduranceRunningTicks, enduranceEliminationTicks);
            case "ta-elimination-1", "ta-elimination-2" ->
                    copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                            highEliminationTicks, timeAttackRunningTicks,
                            replace(timeAttackEliminationTicks, index(key), intValue),
                            timeAttackSurvivorCounts, enduranceRunningTicks, enduranceEliminationTicks);
            case "endurance-elimination-1", "endurance-elimination-2" ->
                    copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                            highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                            timeAttackSurvivorCounts, enduranceRunningTicks,
                            replace(enduranceEliminationTicks, index(key), intValue));
            case "ta-survivors-1", "ta-survivors-2" ->
                    copy(startCountdownTicks, highPracticeTicks, highPrepareTicks, highRunningTicks,
                            highEliminationTicks, timeAttackRunningTicks, timeAttackEliminationTicks,
                            replace(timeAttackSurvivorCounts, index(key), intValue),
                            enduranceRunningTicks, enduranceEliminationTicks);
            default -> throw new IllegalArgumentException("不明な設定キーです: " + key);
        };
    }

    public long value(String key) {
        return switch (key) {
            case "start-countdown" -> startCountdownTicks;
            case "high-practice" -> highPracticeTicks;
            case "high-prepare" -> highPrepareTicks;
            case "high-total" -> highRunningTicks;
            case "ta-total" -> timeAttackRunningTicks;
            case "endurance-total" -> enduranceRunningTicks;
            case "high-elimination-1", "high-elimination-2", "high-elimination-3", "high-elimination-4" -> highEliminationTicks.get(index(key));
            case "ta-elimination-1", "ta-elimination-2" -> timeAttackEliminationTicks.get(index(key));
            case "endurance-elimination-1", "endurance-elimination-2" -> enduranceEliminationTicks.get(index(key));
            case "ta-survivors-1", "ta-survivors-2" -> timeAttackSurvivorCounts.get(index(key));
            default -> throw new IllegalArgumentException("不明な設定キーです: " + key);
        };
    }

    private CompetitionSettings copy(long countdown, long practice, long prepare, long highTotal,
            List<Integer> highCuts, long taTotal, List<Integer> taCuts, List<Integer> survivors,
            long enduranceTotal, List<Integer> enduranceCuts) {
        return new CompetitionSettings(countdown, practice, prepare, highTotal, highCuts,
                taTotal, taCuts, survivors, enduranceTotal, enduranceCuts);
    }

    private static List<Integer> replace(List<Integer> source, int index, int value) {
        var result = new java.util.ArrayList<>(source);
        result.set(index, value);
        return result;
    }

    private static int index(String key) { return Integer.parseInt(key.substring(key.lastIndexOf('-') + 1)) - 1; }

    private static void validate(long countdown, long practice, long prepare, long highTotal,
            List<Integer> highCuts, long taTotal, List<Integer> taCuts, List<Integer> survivors,
            long enduranceTotal, List<Integer> enduranceCuts) {
        if (countdown < 20 || practice < 1 || prepare < 1) throw new IllegalArgumentException("開始・練習・準備時間は正数にしてください");
        validateCuts("高難易度", highTotal, highCuts, 4);
        validateCuts("TA", taTotal, taCuts, 2);
        validateCuts("耐久", enduranceTotal, enduranceCuts, 2);
        if (survivors.size() != 2 || survivors.get(0) < survivors.get(1) || survivors.get(1) < 0) {
            throw new IllegalArgumentException("TA生存人数は2段階かつ降順の0以上にしてください");
        }
    }

    private static void validateCuts(String name, long total, List<Integer> cuts, int expected) {
        if (total < 1 || cuts.size() != expected) throw new IllegalArgumentException(name + "の時間設定が不正です");
        int previous = 0;
        for (int cut : cuts) {
            if (cut <= previous || cut >= total) throw new IllegalArgumentException(name + "の脱落時刻は昇順かつ終了前にしてください");
            previous = cut;
        }
    }
}
