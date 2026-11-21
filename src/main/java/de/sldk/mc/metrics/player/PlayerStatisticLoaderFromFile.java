package de.sldk.mc.metrics.player;

import com.google.common.base.Suppliers;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Fetches player stats stored in
 * <code>data/<default-world>/stats/&lt;UUID&gt;.json</code> files.
 * <p>
 * The server writes a player's file when the player logs out. Thus a file is read again when its modification time
 * changes.
 */
public class PlayerStatisticLoaderFromFile implements PlayerStatisticLoader {
    public final String SERVER_PROPERTIES = "server.properties";
    public final String DEFAULT_WORLD = "world";

    private final Plugin plugin;
    private final Logger logger;
    private final Supplier<Path> statsFolder = Suppliers.memoize(this::findStatsFolder);
    private final Map<UUID, StatsFile> statsFiles = new ConcurrentHashMap<>();

    private record StatsFile(FileTime modified, Map<Enum<?>, Integer> stats) {
    }

    private static final Map<String, Enum<?>> mapStatNameToStat = Arrays
            .stream(PlayerStatisticLoaderFromBukkit.STATISTICS)
            .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e));

    public PlayerStatisticLoaderFromFile(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    @Override
    public Map<Enum<?>, Integer> getPlayerStatistics(OfflinePlayer offlinePlayer) {
        final UUID uuid = offlinePlayer.getUniqueId();
        final Path path = statsFolder.get().resolve(uuid + ".json");

        final FileTime modified;
        try {
            modified = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            statsFiles.remove(uuid);
            return new HashMap<>();
        }

        StatsFile cached = statsFiles.get(uuid);
        if (cached != null && cached.modified().equals(modified)) {
            return cached.stats();
        }

        Map<Enum<?>, Integer> stats;
        try {
            stats = getPlayersStats(path);
        } catch (Exception e) {
            String msg = String.format("Could not read player stats from JSON at '%s'", path);
            logger.log(Level.FINE, msg, e);
            stats = new HashMap<>();
        }
        statsFiles.put(uuid, new StatsFile(modified, stats));
        return stats;
    }

    private Path findStatsFolder() {
        Path worldContainer = plugin.getServer().getWorldContainer().getAbsoluteFile().toPath();
        Path folder = worldContainer.resolve(getDefaultWorld(SERVER_PROPERTIES)).resolve("stats");
        logger.fine("Reading player stats from folder " + folder);
        return folder;
    }

    public String getDefaultWorld(String fileName) {
        try (BufferedReader read = new BufferedReader(new FileReader(fileName))) {
            String line;
            String prefix = "level-name=";
            while ((line = read.readLine()) != null) {
                if (line.startsWith(prefix)) {
                    return line.replace(prefix, "");
                }
            }
        } catch (IOException e) {
            logger.log(Level.FINE, "Failed to read level name from server properties file. ", e);
        }

        return DEFAULT_WORLD;
    }

    /**
     * Given a player's stats json file, map each stat to a value.
     */
    private Map<Enum<?>, Integer> getPlayersStats(Path path) throws IOException {
        DocumentContext ctx = JsonPath.parse(path.toFile());
        Map<String, Object> fileStats = ctx.read(JsonPath.compile("$.stats.minecraft:custom"));
        return fileStats.keySet().stream().filter(mapStatNameToStat::containsKey)
                .filter(e -> fileStats.get(e) instanceof Integer)
                .collect(Collectors.toMap(mapStatNameToStat::get, e -> (Integer) fileStats.get(e)));
    }
}
