package dev.konqasasas.beat.domain.ranking;

import dev.konqasasas.beat.persistence.snapshot.PlayerResultSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public final class OverallRanking {
    private OverallRanking() {}

    public static List<Standing> calculate(List<PlayerResultSnapshot> players) {
        List<Candidate> candidates = players.stream().filter(OverallRanking::eligible).map(player -> {
            int high = player.high().rank();
            int ta = player.timeAttack().rank();
            int endurance = player.endurance().rank();
            int[] sorted = {high, ta, endurance};
            Arrays.sort(sorted);
            long product = Math.multiplyExact(Math.multiplyExact((long) high, ta), endurance);
            return new Candidate(player, product, sorted);
        }).sorted(Comparator.comparingLong(Candidate::product)
                .thenComparingInt(c -> c.sortedRanks()[0])
                .thenComparingInt(c -> c.sortedRanks()[1])
                .thenComparingInt(c -> c.sortedRanks()[2])
                .thenComparing(c -> c.player().uuid())).toList();

        List<Standing> result = new ArrayList<>();
        Candidate previous = null;
        int rank = 0;
        for (int index = 0; index < candidates.size(); index++) {
            Candidate current = candidates.get(index);
            if (previous == null || compareScore(previous, current) != 0) rank = index + 1;
            result.add(new Standing(current.player(), rank, current.product()));
            previous = current;
        }
        return List.copyOf(result);
    }

    private static boolean eligible(PlayerResultSnapshot p) {
        return p.participatedHigh() && p.participatedTa() && p.participatedEndurance()
                && !p.overallExcluded() && !p.disqualified()
                && p.high() != null && p.high().rank() != null
                && p.timeAttack() != null && p.timeAttack().rank() != null
                && p.endurance() != null && p.endurance().rank() != null;
    }

    private static int compareScore(Candidate a, Candidate b) {
        int compared = Long.compare(a.product(), b.product());
        if (compared != 0) return compared;
        for (int i = 0; i < 3; i++) {
            compared = Integer.compare(a.sortedRanks()[i], b.sortedRanks()[i]);
            if (compared != 0) return compared;
        }
        return 0;
    }

    public record Standing(PlayerResultSnapshot player, int rank, long scoreProduct) {}
    private record Candidate(PlayerResultSnapshot player, long product, int[] sortedRanks) {}
}
