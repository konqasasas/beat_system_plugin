package dev.konqasasas.beat.domain.tick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.konqasasas.beat.domain.time.TickTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompetitionTickPipelineTest {
    @Test
    void sameTickAlwaysProcessesRecordsBeforeRankingEliminationAndFinish() {
        RecordingHandler handler = new RecordingHandler();
        CompetitionTickPipeline<String> pipeline = new CompetitionTickPipeline<>(handler);
        long finishTick = TickTime.ticks(30, 0);

        pipeline.processTick(finishTick, List.of("split", "goal"));

        assertEquals(List.of(
                "events@36000:[split, goal]",
                "ranking@36000",
                "elimination@36000",
                "finish@36000"), handler.calls);
        assertEquals(finishTick, pipeline.lastProcessedTick());
    }

    @Test
    void schedulerCallOrderCannotProcessTheSameTickTwiceOrMoveBackwards() {
        CompetitionTickPipeline<String> pipeline = new CompetitionTickPipeline<>(
                new RecordingHandler());
        pipeline.processTick(100, List.of());

        assertThrows(IllegalStateException.class, () -> pipeline.processTick(100, List.of("goal")));
        assertThrows(IllegalStateException.class, () -> pipeline.processTick(99, List.of("goal")));
    }

    @Test
    void handlersCannotMutateTheSubmittedEventBatch() {
        TickPhaseHandler<String> handler = new TickPhaseHandler<>() {
            @Override
            public void processEvents(long tick, List<String> events) {
                assertThrows(UnsupportedOperationException.class, () -> events.add("late-event"));
            }

            @Override
            public void updateRankings(long tick) {
            }

            @Override
            public void evaluateEliminations(long tick) {
            }

            @Override
            public void evaluateFinish(long tick) {
            }
        };

        new CompetitionTickPipeline<>(handler).processTick(0, List.of("spot"));
    }

    private static final class RecordingHandler implements TickPhaseHandler<String> {
        private final List<String> calls = new ArrayList<>();

        @Override
        public void processEvents(long tick, List<String> events) {
            calls.add("events@" + tick + ":" + events);
        }

        @Override
        public void updateRankings(long tick) {
            calls.add("ranking@" + tick);
        }

        @Override
        public void evaluateEliminations(long tick) {
            calls.add("elimination@" + tick);
        }

        @Override
        public void evaluateFinish(long tick) {
            calls.add("finish@" + tick);
        }
    }
}
