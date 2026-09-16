package dev.konqasasas.beat.command;

import dev.konqasasas.beat.configuration.ConfigurationFiles;
import dev.konqasasas.beat.map.BlockRegion;
import dev.konqasasas.beat.map.EnduranceMapConfig;
import dev.konqasasas.beat.map.EnduranceProgressPoint;
import dev.konqasasas.beat.map.HighCourseMap;
import dev.konqasasas.beat.map.MapLocation;
import dev.konqasasas.beat.map.persistence.MapConfigurationService;
import dev.konqasasas.beat.map.validation.MapValidationService;
import dev.konqasasas.beat.map.validation.ValidationReport;
import dev.konqasasas.beat.persistence.PersistenceException;
import dev.konqasasas.beat.setup.MapSetupService;
import dev.konqasasas.beat.setup.NumberRangeFormatter;
import dev.konqasasas.beat.setup.RegionVisualization;
import dev.konqasasas.beat.setup.RegionVisualizer;
import dev.konqasasas.beat.setup.SelectionException;
import dev.konqasasas.beat.setup.SelectionService;
import dev.konqasasas.beat.setup.SetupWand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SetupCommand {
    private static final List<String> ROOTS =
            List.of("wand", "clear", "high", "ta", "endurance", "show", "info", "list", "validate", "help");
    private final MapConfigurationService maps;
    private final MapSetupService setup;
    private final SelectionService selections;
    private final SetupWand wand;
    private final RegionVisualizer visualizer;
    private final MapValidationService validation;
    private final boolean onlineMode;
    private final ConfigurationFiles messages;

    public SetupCommand(MapConfigurationService maps, MapSetupService setup, SelectionService selections,
            SetupWand wand, RegionVisualizer visualizer, MapValidationService validation, boolean onlineMode,
            ConfigurationFiles messages) {
        this.maps = maps;
        this.setup = setup;
        this.selections = selections;
        this.wand = wand;
        this.visualizer = visualizer;
        this.validation = validation;
        this.onlineMode = onlineMode;
        this.messages = messages;
    }

    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || equals(args[0], "help")) {
            help(sender, args.length > 1 ? args[1] : null);
            return true;
        }
        try {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "wand" -> giveWand(requirePlayer(sender));
                case "clear" -> clear(requirePlayer(sender));
                case "high" -> high(requirePlayer(sender), args);
                case "ta" -> timeAttack(requirePlayer(sender), args);
                case "endurance" -> endurance(requirePlayer(sender), args);
                case "show" -> show(requirePlayer(sender), args);
                case "info" -> info(sender, args);
                case "list" -> list(sender, args);
                case "validate" -> validate(sender, args);
                default -> usage(sender);
            };
        } catch (SelectionException | PersistenceException | IllegalArgumentException exception) {
            sender.sendMessage(msg("commands.setup.error", "[BEAT] ERROR: {message}",
                    Map.of("message", exception.getMessage())));
            return true;
        }
    }

    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) return matching(ROOTS, args[0]);
        if (args.length == 2 && equals(args[0], "help")) return matching(List.of("high", "ta", "endurance"), args[1]);
        if (args.length == 2 && equals(args[0], "validate")) return matching(List.of("high", "ta", "endurance", "all"), args[1]);
        if (args.length == 2 && List.of("show", "info", "list").contains(lower(args[0])))
            return matching(List.of("high", "ta", "endurance"), args[1]);
        if (args.length == 2 && equals(args[0], "high")) return matching(List.of("spot", "goal", "start", "prepare", "end"), args[1]);
        if (args.length == 2 && equals(args[0], "ta")) return matching(List.of("start", "split", "goal", "restart", "end"), args[1]);
        if (args.length == 2 && equals(args[0], "endurance")) return matching(List.of("progress", "goal", "zone", "zone-restart", "start", "end", "fall-y"), args[1]);
        if (args.length == 3 && equals(args[0], "high")) return matching(highOperations(args[1]), args[2]);
        if (args.length == 3 && equals(args[0], "ta")) return matching(taOperations(args[1]), args[2]);
        if (args.length == 3 && equals(args[0], "endurance")) return matching(enduranceOperations(args[1]), args[2]);
        if (args.length == 3 && equals(args[0], "show")) return matching(showTargets(args[1]), args[2]);
        if (args.length == 3 && (equals(args[0], "info") || equals(args[0], "list")))
            return matching(infoTargets(args[1]), args[2]);
        if (args.length == 4 && equals(args[0], "endurance") && (equals(args[1], "zone") || equals(args[1], "zone-restart")))
            return matching(List.of("2", "3"), args[3]);
        if (args.length == 4 && equals(args[0], "endurance") && equals(args[1], "fall-y")) return matching(List.of("here"), args[3]);
        if (args.length == 4 && equals(args[0], "show") && equals(args[1], "endurance") && equals(args[2], "progress")) return matching(List.of("all"), args[3]);
        if (args.length == 4 && equals(args[0], "show") && equals(args[1], "high") && (equals(args[2], "all") || equals(args[2], "course"))) return matching(List.of("1", "2", "3", "4", "5"), args[3]);
        return List.of();
    }

    public void reload() throws PersistenceException {
        try {
            maps.reload();
        } finally {
            selections.clearAll();
        }
    }

    private boolean giveWand(Player player) {
        player.sendMessage(wand.give(player)
                ? msg("commands.setup.wand-given", "[BEAT] 範囲選択ツールを渡しました。")
                : msg("commands.setup.wand-owned", "[BEAT] 範囲選択ツールは既に所持しています。"));
        return true;
    }

    private boolean clear(Player player) {
        selections.clear(player.getUniqueId());
        player.sendMessage(msg("commands.setup.selection-cleared", "[BEAT] 選択をクリアしました。"));
        return true;
    }

    private boolean high(Player player, String[] a) throws SelectionException, PersistenceException {
        if (a.length < 3) return usage(player);
        String target = lower(a[1]);
        String operation = lower(a[2]);
        if (target.equals("spot")) {
            if (operation.equals("add") && a.length == 4) {
                int number = setup.addHighSpot(course(a[3]), selected(player));
                return saved(player, "High Course " + a[3] + " Spot " + number, true);
            }
            if (a.length == 5 && operation.equals("set")) {
                setup.setHighSpot(course(a[3]), positive(a[4], "Spot"), selected(player));
                return saved(player, "High Course " + a[3] + " Spot " + a[4], true);
            }
            if (a.length == 5 && operation.equals("remove")) {
                setup.removeHighSpot(course(a[3]), positive(a[4], "Spot"));
                return saved(player, "High Spotを削除しました", false);
            }
        }
        if (target.equals("goal") && a.length == 4) {
            int course = course(a[3]);
            if (operation.equals("set")) {
                BlockRegion region = selected(player);
                setup.setHighGoal(course, region);
                return saved(player, "High Course " + course + " " + target, true);
            }
            if (operation.equals("remove")) {
                setup.removeHighGoal(course);
                return saved(player, "High " + target + "を削除しました", false);
            }
        }
        if (target.equals("start") && a.length == 4) {
            int course = course(a[3]);
            if (operation.equals("set")) setup.setHighStart(course, location(player));
            else if (operation.equals("remove")) setup.removeHighStart(course);
            else return usage(player);
            return saved(player, "High Course " + course + " Start", false);
        }
        if (a.length == 3 && operation.equals("set") && target.equals("prepare")) {
            setup.setHighPrepare(location(player)); return saved(player, "High Prepare", false);
        }
        if (a.length == 3 && operation.equals("set") && target.equals("end")) {
            setup.setHighEnd(location(player)); return saved(player, "High End", false);
        }
        return usage(player);
    }

    private boolean timeAttack(Player player, String[] a) throws SelectionException, PersistenceException {
        if (a.length < 3) return usage(player);
        String target = lower(a[1]);
        String op = lower(a[2]);
        if (target.equals("start") && op.equals("set") && a.length == 3) {
            setup.setTimeAttackStart(selected(player)); return saved(player, "TA Start", true);
        }
        if (target.equals("split")) {
            if (op.equals("add") && a.length == 3) {
                int n = setup.addTimeAttackSplit(selected(player)); return saved(player, "TA Split " + n, true);
            }
            if (op.equals("set") && a.length == 4) {
                setup.setTimeAttackSplit(positive(a[3], "Split"), selected(player)); return saved(player, "TA Split " + a[3], true);
            }
            if (op.equals("remove") && a.length == 4) {
                setup.removeTimeAttackSplit(positive(a[3], "Split")); return saved(player, "TA Splitを削除しました", false);
            }
        }
        if (target.equals("goal") && op.equals("set") && a.length == 3) {
            setup.setTimeAttackGoal(selected(player)); return saved(player, "TA Goal", true);
        }
        if (target.equals("restart") && op.equals("set") && a.length == 3) {
            setup.setTimeAttackRestart(location(player)); return saved(player, "TA Restart", false);
        }
        if (target.equals("end") && op.equals("set") && a.length == 3) {
            setup.setTimeAttackEnd(location(player)); return saved(player, "TA End", false);
        }
        return usage(player);
    }

    private boolean endurance(Player player, String[] a) throws SelectionException, PersistenceException {
        if (a.length < 3) return usage(player);
        String target = lower(a[1]);
        String op = lower(a[2]);
        if (target.equals("progress")) {
            if (op.equals("add") && a.length == 3) {
                int n = setup.addEnduranceProgress(progressPoint(player));
                return saved(player, "Endurance Progress " + n + " 地点 1", false);
            }
            if (op.equals("add") && a.length == 4) {
                int number = positive(a[3], "Progress");
                int index = setup.addEnduranceProgress(number, progressPoint(player));
                return saved(player, "Endurance Progress " + number + " 地点 " + index, false);
            }
            if (op.equals("set") && a.length == 4) {
                setup.setEnduranceProgress(positive(a[3], "Progress"), progressPoint(player));
                return saved(player, "Endurance Progress " + a[3] + " 地点 1", false);
            }
            if (op.equals("remove") && a.length == 4) {
                setup.removeEnduranceProgress(positive(a[3], "Progress")); return saved(player, "Endurance Progressを削除しました", false);
            }
            if (op.equals("remove") && a.length == 5) {
                setup.removeEnduranceProgress(positive(a[3], "Progress"), positive(a[4], "地点"));
                return saved(player, "Endurance Progress " + a[3] + " の地点 " + a[4] + " を削除しました", false);
            }
        }
        if (target.equals("goal") && op.equals("set") && a.length == 3) {
            int n = setup.setEnduranceGoal(progressPoint(player));
            return saved(player, "Endurance Goal (Progress " + n + ")", false);
        }
        if (target.equals("zone") && op.equals("set") && a.length == 5) {
            setup.setEnduranceZone(zone(a[3]), positive(a[4], "Progress")); return saved(player, "Endurance Zone " + a[3], false);
        }
        if (target.equals("zone-restart") && op.equals("set") && a.length == 4) {
            setup.setEnduranceZoneRestart(zone(a[3]), location(player)); return saved(player, "Endurance Zone " + a[3] + " Restart", false);
        }
        if (target.equals("start") && op.equals("set") && a.length == 3) {
            setup.setEnduranceStart(location(player)); return saved(player, "Endurance Start", false);
        }
        if (target.equals("end") && op.equals("set") && a.length == 3) {
            setup.setEnduranceEnd(location(player)); return saved(player, "Endurance End", false);
        }
        if (target.equals("fall-y") && op.equals("set") && a.length == 4) {
            setup.setEnduranceFallY(equals(a[3], "here") ? player.getLocation().getY() : decimal(a[3], "Y"));
            return saved(player, "Endurance Fall Y", false);
        }
        return usage(player);
    }

    private boolean show(Player player, String[] a) {
        if (a.length >= 2 && equals(a[1], "endurance")) {
            if (a.length == 3 && equals(a[2], "off")) {
                boolean hidden = visualizer.hideEndurancePoints(player);
                player.sendMessage(hidden
                        ? msg("commands.setup.show-endurance-off", "[BEAT] 耐久ProgressのDUST表示を停止しました。")
                        : msg("commands.setup.show-endurance-not-active", "[BEAT] 耐久ProgressのDUST表示は有効ではありません。"));
                return true;
            }
            List<EnduranceProgressPoint> points = resolveEndurancePoints(a);
            boolean persistent = a.length == 3 && equals(a[2], "all")
                    || a.length == 4 && equals(a[2], "progress") && equals(a[3], "all");
            int displayed = visualizer.showEndurancePoints(player,
                    persistent
                            ? () -> maps.endurance().progresses().values().stream().flatMap(List::stream).toList()
                            : () -> points,
                    persistent);
            if (displayed == 0) player.sendMessage(msg("commands.setup.show-empty", "[BEAT] 現在のワールドで表示できる地点がありません。"));
            else player.sendMessage(msg(persistent ? "commands.setup.show-point-count-persistent" : "commands.setup.show-point-count",
                    persistent
                            ? "[BEAT] {count}件の地点をDUSTで継続表示します。停止: /beat setup show endurance off"
                            : "[BEAT] {count}件の地点をDUSTで表示します（本人にのみ表示）。",
                    Map.of("count", displayed)));
            return true;
        }
        List<RegionVisualization> regions = resolveRegions(a);
        int displayed = visualizer.show(player, regions);
        if (displayed == 0) player.sendMessage(msg("commands.setup.show-empty", "[BEAT] 現在のワールドで表示できる範囲がありません。"));
        else player.sendMessage(msg("commands.setup.show-count", "[BEAT] {count}件の範囲を表示します（本人にのみ表示）。",
                Map.of("count", displayed)));
        return true;
    }

    private List<RegionVisualization> resolveRegions(String[] a) {
        if (a.length < 3) throw new IllegalArgumentException("使用例: /beat setup show high all");
        List<RegionVisualization> result = new ArrayList<>();
        String competition = lower(a[1]);
        if (competition.equals("high")) {
            if (equals(a[2], "all") && a.length == 3) maps.high().courses().values().forEach(c -> addCourse(result, c));
            else if (equals(a[2], "course") && a.length == 4) addCourse(result, requiredCourse(course(a[3])));
            else if (equals(a[2], "spot") && a.length == 5) add(result, requiredCourse(course(a[3])).spots().get(positive(a[4], "Spot")), "spot");
            else throw new IllegalArgumentException("使用例: /beat setup show high spot <course> <spot>");
        } else if (competition.equals("ta") && equals(a[2], "all") && a.length == 3) {
            add(result, maps.timeAttack().start(), "start");
            maps.timeAttack().splits().values().forEach(r -> add(result, r, "progress"));
            add(result, maps.timeAttack().goal(), "goal");
        } else throw new IllegalArgumentException("表示対象が正しくありません");
        if (result.isEmpty()) throw new IllegalArgumentException("対象範囲が設定されていません");
        return result;
    }

    private List<EnduranceProgressPoint> resolveEndurancePoints(String[] a) {
        if (a.length < 3) throw new IllegalArgumentException("使用例: /beat setup show endurance progress <number|all>");
        EnduranceMapConfig config = maps.endurance();
        List<EnduranceProgressPoint> result;
        if (equals(a[2], "all") && a.length == 3
                || equals(a[2], "progress") && a.length == 4 && equals(a[3], "all")) {
            result = config.progresses().values().stream().flatMap(List::stream).toList();
        } else if (equals(a[2], "progress") && a.length == 4) {
            int number = positive(a[3], "Progress");
            result = config.progresses().get(number);
            if (result == null) throw new IllegalArgumentException("Progress " + number + " は未設定です");
        } else {
            throw new IllegalArgumentException("使用例: /beat setup show endurance progress <number|all>");
        }
        if (result.isEmpty()) throw new IllegalArgumentException("対象地点が設定されていません");
        return result;
    }

    private boolean info(CommandSender sender, String[] a) {
        if (a.length == 4 && equals(a[1], "endurance") && equals(a[2], "progress")) {
            int number = positive(a[3], "Progress");
            List<EnduranceProgressPoint> points = maps.endurance().progresses().get(number);
            if (points == null) throw new IllegalArgumentException("Progress " + number + " は未設定です");
            sender.sendMessage(msg("commands.setup.info-header", "{primary}--- {label} ---{reset}",
                    Map.of("label", "Progress %03d".formatted(number))));
            for (int index = 0; index < points.size(); index++) {
                EnduranceProgressPoint point = points.get(index);
                sender.sendMessage(msg("commands.setup.info-point", "地点 {index}: {world} ({x}, {y}, {z})",
                        Map.of("index", index + 1, "world", point.world(), "x", point.x(), "y", point.y(), "z", point.z())));
            }
            return true;
        }
        BlockRegion region;
        String label;
        if (a.length == 5 && equals(a[1], "high") && equals(a[2], "spot")) {
            int course = course(a[3]); int spot = positive(a[4], "Spot"); region = requiredCourse(course).spots().get(spot); label = "High Course " + course + " Spot " + spot;
        } else if (a.length == 4 && equals(a[1], "ta") && equals(a[2], "split")) {
            int n = positive(a[3], "Split"); region = maps.timeAttack().splits().get(n); label = "TA Split " + n;
        } else throw new IllegalArgumentException("使用例: /beat setup info endurance progress <number>");
        if (region == null) throw new IllegalArgumentException(label + " は未設定です");
        sender.sendMessage(msg("commands.setup.info-header", "{primary}--- {label} ---{reset}", Map.of("label", label)));
        sender.sendMessage(msg("commands.setup.info-world", "World: {world}", Map.of("world", region.world())));
        sender.sendMessage(msg("commands.setup.info-x", "X: {min} - {max}", Map.of("min", region.minX(), "max", region.maxX())));
        sender.sendMessage(msg("commands.setup.info-y", "Y: {y}", Map.of("y", region.y())));
        sender.sendMessage(msg("commands.setup.info-z", "Z: {min} - {max}", Map.of("min", region.minZ(), "max", region.maxZ())));
        return true;
    }

    private boolean list(CommandSender sender, String[] a) {
        java.util.Set<Integer> values;
        String label;
        if (a.length == 3 && equals(a[1], "endurance") && equals(a[2], "progress")) {
            values = maps.endurance().progresses().keySet(); label = "Progress";
        } else if (a.length == 4 && equals(a[1], "high") && equals(a[2], "spot")) {
            values = requiredCourse(course(a[3])).spots().keySet(); label = "Spot";
        } else if (a.length == 3 && equals(a[1], "ta") && equals(a[2], "split")) {
            values = maps.timeAttack().splits().keySet(); label = "Split";
        } else throw new IllegalArgumentException("使用例: /beat setup list endurance progress");
        sender.sendMessage(msg("commands.setup.list-values", "{label}: {values}", Map.of("label", label, "values", NumberRangeFormatter.format(values))));
        sender.sendMessage(msg("commands.setup.list-missing", "Missing: {values}", Map.of("values", NumberRangeFormatter.missing(values))));
        if (label.equals("Progress")) sender.sendMessage(msg("commands.setup.list-goal", "Goal: {value}", Map.of("value", formatNullable(maps.endurance().goalProgress()))));
        return true;
    }

    private boolean validate(CommandSender sender, String[] a) {
        if (a.length != 2) throw new IllegalArgumentException("使用例: /beat setup validate <high|ta|endurance|all>");
        ValidationReport report = switch (lower(a[1])) {
            case "high" -> validation.validateHigh(maps.high());
            case "ta" -> validation.validateTimeAttack(maps.timeAttack());
            case "endurance" -> validation.validateEndurance(maps.endurance());
            case "all" -> validation.validateAll(maps.high(), maps.timeAttack(), maps.endurance(), onlineMode);
            default -> throw new IllegalArgumentException("competitionは high / ta / endurance / all です");
        };
        sender.sendMessage(msg("commands.setup.validation-state", "Validation: {state}", Map.of("state", report.passed() ? "PASSED" : "FAILED")));
        report.issues().forEach(issue -> sender.sendMessage(msg("commands.setup.validation-issue", "{severity} [{code}] {message}",
                Map.of("severity", issue.severity(), "code", issue.code(), "message", issue.message()))));
        sender.sendMessage(msg("commands.setup.validation-counts", "ERROR: {errors} / WARNING: {warnings}",
                Map.of("errors", report.errorCount(), "warnings", report.warningCount())));
        return true;
    }

    private void addCourse(List<RegionVisualization> result, HighCourseMap course) {
        if (course == null) return;
        course.spots().values().forEach(r -> add(result, r, "spot"));
        add(result, course.goal(), "goal");
    }

    private void add(List<RegionVisualization> result, BlockRegion region, String kind) {
        if (region == null) return;
        Particle fallback = switch (kind) { case "goal" -> Particle.FLAME; case "start" -> Particle.HAPPY_VILLAGER; default -> Particle.END_ROD; };
        result.add(new RegionVisualization(region, visualizer.configuredParticle(kind, fallback)));
    }

    private HighCourseMap requiredCourse(int course) {
        HighCourseMap found = maps.high().courses().get(course);
        if (found == null) throw new IllegalArgumentException("Course " + course + " は未設定です");
        return found;
    }

    private BlockRegion selected(Player player) throws SelectionException { return selections.requireRegion(player.getUniqueId()); }
    private boolean saved(Player player, String target, boolean clearSelection) {
        if (clearSelection) selections.clear(player.getUniqueId());
        player.sendMessage(msg("commands.setup.saved", "[BEAT] 保存しました: {target}", Map.of("target", target)));
        return true;
    }
    private static MapLocation location(Player p) { var l = p.getLocation(); return new MapLocation(l.getWorld().getName(), l.getX(), l.getY(), l.getZ(), l.getYaw(), l.getPitch()); }
    private static EnduranceProgressPoint progressPoint(Player p) {
        var l = p.getLocation();
        return new EnduranceProgressPoint(l.getWorld().getName(), l.getX(), l.getY(), l.getZ());
    }
    private static Player requirePlayer(CommandSender sender) { if (sender instanceof Player p) return p; throw new IllegalArgumentException("この操作はゲーム内のプレイヤーから実行してください"); }
    private static int positive(String value, String label) { try { int n = Integer.parseInt(value); if (n < 1) throw new NumberFormatException(); return n; } catch (NumberFormatException e) { throw new IllegalArgumentException(label + "番号は1以上の整数です"); } }
    private static int course(String value) { int n = positive(value, "Course"); if (n > 5) throw new IllegalArgumentException("Course番号は1〜5です"); return n; }
    private static int zone(String value) { int n = positive(value, "Zone"); if (n != 2 && n != 3) throw new IllegalArgumentException("Zone番号は2または3です"); return n; }
    private static double decimal(String value, String label) { try { return Double.parseDouble(value); } catch (NumberFormatException e) { throw new IllegalArgumentException(label + "は数値で指定してください"); } }
    private static String formatNullable(Integer n) { return n == null ? "未設定" : "%03d".formatted(n); }
    private static boolean equals(String a, String b) { return a.equalsIgnoreCase(b); }
    private static String lower(String value) { return value.toLowerCase(Locale.ROOT); }
    private static List<String> matching(List<String> candidates, String value) { String p = lower(value); return candidates.stream().filter(c -> lower(c).startsWith(p)).toList(); }
    private static List<String> highOperations(String target) { return switch (lower(target)) { case "spot" -> List.of("add", "set", "remove"); case "goal", "start" -> List.of("set", "remove"); case "prepare", "end" -> List.of("set"); default -> List.of(); }; }
    private static List<String> taOperations(String target) { return lower(target).equals("split") ? List.of("add", "set", "remove") : List.of("set"); }
    private static List<String> enduranceOperations(String target) { return lower(target).equals("progress") ? List.of("add", "set", "remove") : List.of("set"); }
    private static List<String> showTargets(String c) { return switch (lower(c)) { case "high" -> List.of("all", "course", "spot"); case "ta" -> List.of("all"); case "endurance" -> List.of("all", "progress", "off"); default -> List.of(); }; }
    private static List<String> infoTargets(String c) { return switch (lower(c)) { case "high" -> List.of("spot"); case "ta" -> List.of("split"); case "endurance" -> List.of("progress"); default -> List.of(); }; }
    private boolean usage(CommandSender sender) { sender.sendMessage(msg("commands.setup.usage", "[BEAT] 使用方法が正しくありません。/beat setup help を確認してください。")); return true; }

    private void help(CommandSender sender, String section) {
        if (section == null) {
            messages.messages("commands.setup.help", List.of("{primary}--- BEAT マップセットアップ ---{reset}", "範囲を登録する場合:",
                    "1. /beat setup wand で選択ツールを取得", "2. 左クリックでPos1 / 右クリックでPos2",
                    "3. /beat setup <競技> ... で登録", "詳細: /beat setup help <high|ta|endurance>",
                    "確認: /beat setup show|info|list|validate ...")).forEach(sender::sendMessage);
            return;
        }
        switch (lower(section)) {
            case "high" -> messages.messages("commands.setup.help-high", Arrays.asList("/beat setup high spot add <course>", "/beat setup high spot set <course> <spot>", "/beat setup high goal set <course>", "/beat setup high start set <course>", "/beat setup high prepare|end set")).forEach(sender::sendMessage);
            case "ta" -> messages.messages("commands.setup.help-ta", Arrays.asList("/beat setup ta start set", "/beat setup ta split add", "/beat setup ta split set|remove <number>", "/beat setup ta goal set", "/beat setup ta restart|end set")).forEach(sender::sendMessage);
            case "endurance" -> messages.messages("commands.setup.help-endurance", Arrays.asList("/beat setup endurance progress add [number]", "/beat setup endurance progress set <number>", "/beat setup endurance progress remove <number> [location-index]", "/beat setup endurance goal set", "/beat setup endurance zone set <2|3> <progress>", "/beat setup endurance zone-restart set <2|3>", "/beat setup endurance start|end set", "/beat setup endurance fall-y set <here|y>")).forEach(sender::sendMessage);
            default -> usage(sender);
        }
    }

    private String msg(String path, String fallback) { return messages.message(path, fallback); }
    private String msg(String path, String fallback, Map<String, ?> values) { return messages.message(path, fallback, values); }
}
