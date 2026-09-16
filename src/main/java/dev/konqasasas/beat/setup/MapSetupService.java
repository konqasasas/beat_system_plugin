package dev.konqasasas.beat.setup;

import dev.konqasasas.beat.map.BlockRegion;
import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import dev.konqasasas.beat.map.HighCourseMap;
import dev.konqasasas.beat.map.HighDifficultyMapConfig;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.TimeAttackMapConfig;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.persistence.PersistenceException;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class MapSetupService {
    private final MapConfigurationService maps;

    public MapSetupService(MapConfigurationService maps) {
        this.maps = maps;
    }

    public int addHighSpot(int courseNumber, BlockRegion region) throws PersistenceException {
        HighCourseMap course = highCourse(courseNumber);
        int number = nextNumber(course.spots());
        setHighSpot(courseNumber, number, region);
        return number;
    }

    public void setHighSpot(int courseNumber, int number, BlockRegion region) throws PersistenceException {
        requirePositive(number, "Spot");
        HighCourseMap course = highCourse(courseNumber);
        Map<Integer, BlockRegion> spots = mutable(course.spots());
        spots.put(number, region);
        saveHighCourse(courseNumber, new HighCourseMap(spots, course.goal(), course.start()));
    }

    public void removeHighSpot(int courseNumber, int number) throws PersistenceException {
        HighCourseMap course = highCourse(courseNumber);
        Map<Integer, BlockRegion> spots = mutable(course.spots());
        requireRemoved(spots.remove(number), "Spot " + number);
        saveHighCourse(courseNumber, new HighCourseMap(spots, course.goal(), course.start()));
    }

    public void setHighGoal(int courseNumber, BlockRegion region) throws PersistenceException {
        HighCourseMap c = highCourse(courseNumber);
        saveHighCourse(courseNumber, new HighCourseMap(c.spots(), region, c.start()));
    }

    public void removeHighGoal(int courseNumber) throws PersistenceException {
        HighCourseMap c = highCourse(courseNumber);
        requireRemoved(c.goal(), "Goal");
        saveHighCourse(courseNumber, new HighCourseMap(c.spots(), null, c.start()));
    }

    public void setHighStart(int courseNumber, MapLocation location) throws PersistenceException {
        HighCourseMap c = highCourse(courseNumber);
        saveHighCourse(courseNumber, new HighCourseMap(c.spots(), c.goal(), location));
    }

    public void removeHighStart(int courseNumber) throws PersistenceException {
        HighCourseMap c = highCourse(courseNumber);
        requireRemoved(c.start(), "Start");
        saveHighCourse(courseNumber, new HighCourseMap(c.spots(), c.goal(), null));
    }

    public void setHighPrepare(MapLocation location) throws PersistenceException {
        HighDifficultyMapConfig c = maps.high();
        maps.saveHigh(new HighDifficultyMapConfig(c.schemaVersion(), c.courses(), location, c.end()));
    }

    public void setHighEnd(MapLocation location) throws PersistenceException {
        HighDifficultyMapConfig c = maps.high();
        maps.saveHigh(new HighDifficultyMapConfig(c.schemaVersion(), c.courses(), c.prepare(), location));
    }

    public void setTimeAttackStart(BlockRegion region) throws PersistenceException {
        TimeAttackMapConfig c = maps.timeAttack();
        saveTa(new TimeAttackMapConfig(c.schemaVersion(), region, c.splits(), c.goal(), c.restart(), c.end()));
    }

    public int addTimeAttackSplit(BlockRegion region) throws PersistenceException {
        int number = nextNumber(maps.timeAttack().splits());
        setTimeAttackSplit(number, region);
        return number;
    }

    public void setTimeAttackSplit(int number, BlockRegion region) throws PersistenceException {
        requirePositive(number, "Split");
        TimeAttackMapConfig c = maps.timeAttack();
        Map<Integer, BlockRegion> splits = mutable(c.splits());
        splits.put(number, region);
        saveTa(new TimeAttackMapConfig(c.schemaVersion(), c.start(), splits, c.goal(), c.restart(), c.end()));
    }

    public void removeTimeAttackSplit(int number) throws PersistenceException {
        TimeAttackMapConfig c = maps.timeAttack();
        Map<Integer, BlockRegion> splits = mutable(c.splits());
        requireRemoved(splits.remove(number), "Split " + number);
        saveTa(new TimeAttackMapConfig(c.schemaVersion(), c.start(), splits, c.goal(), c.restart(), c.end()));
    }

    public void setTimeAttackGoal(BlockRegion region) throws PersistenceException {
        TimeAttackMapConfig c = maps.timeAttack();
        saveTa(new TimeAttackMapConfig(c.schemaVersion(), c.start(), c.splits(), region, c.restart(), c.end()));
    }

    public void setTimeAttackRestart(MapLocation location) throws PersistenceException {
        TimeAttackMapConfig c = maps.timeAttack();
        saveTa(new TimeAttackMapConfig(c.schemaVersion(), c.start(), c.splits(), c.goal(), location, c.end()));
    }

    public void setTimeAttackEnd(MapLocation location) throws PersistenceException {
        TimeAttackMapConfig c = maps.timeAttack();
        saveTa(new TimeAttackMapConfig(c.schemaVersion(), c.start(), c.splits(), c.goal(), c.restart(), location));
    }

    public int addEnduranceProgress(EnduranceProgressPoint point) throws PersistenceException {
        int number = nextNumber(maps.endurance().progresses());
        setEnduranceProgress(number, point);
        return number;
    }

    public int addEnduranceProgress(int number, EnduranceProgressPoint point) throws PersistenceException {
        requirePositive(number, "Progress");
        EnduranceMapConfig c = maps.endurance();
        Map<Integer, List<EnduranceProgressPoint>> progresses = mutable(c.progresses());
        List<EnduranceProgressPoint> points = new ArrayList<>(progresses.getOrDefault(number, List.of()));
        if (points.contains(point)) throw new IllegalArgumentException("同じProgress地点が既に登録されています");
        points.add(point);
        progresses.put(number, List.copyOf(points));
        saveEndurance(copyEndurance(c, progresses, c.goalProgress(), c.zone2Progress(), c.zone3Progress()));
        return points.size();
    }

    public void setEnduranceProgress(int number, EnduranceProgressPoint point) throws PersistenceException {
        requirePositive(number, "Progress");
        EnduranceMapConfig c = maps.endurance();
        Map<Integer, List<EnduranceProgressPoint>> progresses = mutable(c.progresses());
        progresses.put(number, List.of(point));
        saveEndurance(copyEndurance(c, progresses, c.goalProgress(), c.zone2Progress(), c.zone3Progress()));
    }

    public void removeEnduranceProgress(int number) throws PersistenceException {
        EnduranceMapConfig c = maps.endurance();
        Map<Integer, List<EnduranceProgressPoint>> progresses = mutable(c.progresses());
        requireRemoved(progresses.remove(number), "Progress " + number);
        Integer goal = equalsNumber(c.goalProgress(), number) ? null : c.goalProgress();
        Integer zone2 = equalsNumber(c.zone2Progress(), number) ? null : c.zone2Progress();
        Integer zone3 = equalsNumber(c.zone3Progress(), number) ? null : c.zone3Progress();
        saveEndurance(copyEndurance(c, progresses, goal, zone2, zone3));
    }

    public void removeEnduranceProgress(int number, int locationIndex) throws PersistenceException {
        requirePositive(number, "Progress");
        requirePositive(locationIndex, "地点");
        EnduranceMapConfig c = maps.endurance();
        List<EnduranceProgressPoint> existing = c.progresses().get(number);
        if (existing == null) throw new IllegalArgumentException("Progress " + number + " は未設定です");
        if (locationIndex > existing.size()) {
            throw new IllegalArgumentException("Progress " + number + " の地点 " + locationIndex + " は未設定です");
        }
        if (existing.size() == 1) {
            removeEnduranceProgress(number);
            return;
        }
        Map<Integer, List<EnduranceProgressPoint>> progresses = mutable(c.progresses());
        List<EnduranceProgressPoint> points = new ArrayList<>(existing);
        points.remove(locationIndex - 1);
        progresses.put(number, List.copyOf(points));
        saveEndurance(copyEndurance(c, progresses, c.goalProgress(), c.zone2Progress(), c.zone3Progress()));
    }

    public int setEnduranceGoal(EnduranceProgressPoint point) throws PersistenceException {
        EnduranceMapConfig c = maps.endurance();
        int number = nextNumber(c.progresses());
        Map<Integer, List<EnduranceProgressPoint>> progresses = mutable(c.progresses());
        progresses.put(number, List.of(point));
        saveEndurance(copyEndurance(c, progresses, number, c.zone2Progress(), c.zone3Progress()));
        return number;
    }

    public void setEnduranceZone(int zone, int progress) throws PersistenceException {
        EnduranceMapConfig c = maps.endurance();
        requireZone(zone);
        if (!c.progresses().containsKey(progress)) {
            throw new IllegalArgumentException("Progress " + progress + " は存在しません");
        }
        Integer zone2 = zone == 2 ? Integer.valueOf(progress) : c.zone2Progress();
        Integer zone3 = zone == 3 ? Integer.valueOf(progress) : c.zone3Progress();
        saveEndurance(copyEndurance(c, c.progresses(), c.goalProgress(), zone2, zone3));
    }

    public void setEnduranceZoneRestart(int zone, MapLocation location) throws PersistenceException {
        requireZone(zone);
        EnduranceMapConfig c = maps.endurance();
        saveEndurance(new EnduranceMapConfig(c.schemaVersion(), c.progresses(), c.goalProgress(),
                c.zone2Progress(), c.zone3Progress(), zone == 2 ? location : c.zone2Restart(),
                zone == 3 ? location : c.zone3Restart(), c.start(), c.end(), c.fallY()));
    }

    public void setEnduranceStart(MapLocation location) throws PersistenceException {
        EnduranceMapConfig c = maps.endurance();
        saveEndurance(new EnduranceMapConfig(c.schemaVersion(), c.progresses(), c.goalProgress(),
                c.zone2Progress(), c.zone3Progress(), c.zone2Restart(), c.zone3Restart(), location, c.end(), c.fallY()));
    }

    public void setEnduranceEnd(MapLocation location) throws PersistenceException {
        EnduranceMapConfig c = maps.endurance();
        saveEndurance(new EnduranceMapConfig(c.schemaVersion(), c.progresses(), c.goalProgress(),
                c.zone2Progress(), c.zone3Progress(), c.zone2Restart(), c.zone3Restart(), c.start(), location, c.fallY()));
    }

    public void setEnduranceFallY(double y) throws PersistenceException {
        if (!Double.isFinite(y)) throw new IllegalArgumentException("Yは有限値で指定してください");
        EnduranceMapConfig c = maps.endurance();
        saveEndurance(new EnduranceMapConfig(c.schemaVersion(), c.progresses(), c.goalProgress(),
                c.zone2Progress(), c.zone3Progress(), c.zone2Restart(), c.zone3Restart(), c.start(), c.end(), y));
    }

    private HighCourseMap highCourse(int courseNumber) {
        if (courseNumber < 1 || courseNumber > 5) throw new IllegalArgumentException("Course番号は1〜5です");
        return maps.high().courses().getOrDefault(courseNumber, new HighCourseMap(Map.of(), null, null));
    }

    private void saveHighCourse(int number, HighCourseMap course) throws PersistenceException {
        HighDifficultyMapConfig c = maps.high();
        Map<Integer, HighCourseMap> courses = mutable(c.courses());
        courses.put(number, course);
        maps.saveHigh(new HighDifficultyMapConfig(c.schemaVersion(), courses, c.prepare(), c.end()));
    }

    private void saveTa(TimeAttackMapConfig candidate) throws PersistenceException { maps.saveTimeAttack(candidate); }
    private void saveEndurance(EnduranceMapConfig candidate) throws PersistenceException { maps.saveEndurance(candidate); }

    private static EnduranceMapConfig copyEndurance(EnduranceMapConfig c,
            Map<Integer, List<EnduranceProgressPoint>> progresses,
            Integer goal, Integer zone2, Integer zone3) {
        return new EnduranceMapConfig(c.schemaVersion(), progresses, goal, zone2, zone3,
                c.zone2Restart(), c.zone3Restart(), c.start(), c.end(), c.fallY());
    }

    private static <K, V> Map<K, V> mutable(Map<K, V> source) { return new HashMap<>(source); }
    private static int nextNumber(Map<Integer, ?> values) { return values.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1; }
    private static void requirePositive(int number, String label) { if (number < 1) throw new IllegalArgumentException(label + "番号は1以上です"); }
    private static void requireZone(int zone) { if (zone != 2 && zone != 3) throw new IllegalArgumentException("Zone番号は2または3です"); }
    private static void requireRemoved(Object value, String label) { if (value == null) throw new IllegalArgumentException(label + " は未設定です"); }
    private static boolean equalsNumber(Integer value, int number) { return value != null && value == number; }
}
