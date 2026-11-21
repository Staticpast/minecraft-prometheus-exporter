package de.sldk.mc.metrics;

import de.sldk.mc.metrics.player.PlayerStatisticLoaderFromFile;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.Statistic;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.UUID;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PlayerStatisticLoaderFromFileTest {
    private static Plugin plugin;
    private static PlayerStatisticLoaderFromFile playerStatisticLoader;

    @BeforeAll
    static void beforeAllTests() {
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        playerStatisticLoader = new PlayerStatisticLoaderFromFile(plugin);
    }

    @Test
    void use_world_as_fallback_world() {
        assertThat(playerStatisticLoader.getDefaultWorld(
                Paths.get("src", "test", "resources", "server", "invalid.properties").toFile()
                        .getAbsolutePath())).isEqualTo(
                playerStatisticLoader.DEFAULT_WORLD);
    }

    @Test
    void get_default_world() {
        assertThat(playerStatisticLoader.getDefaultWorld(playerStatisticLoader.SERVER_PROPERTIES)).isEqualTo(
                playerStatisticLoader.DEFAULT_WORLD);
    }

    @Test
    void get_custom_world() {
        assertThat(playerStatisticLoader.getDefaultWorld(
                Paths.get("src", "test", "resources", "server", "custom.properties").toFile()
                        .getAbsolutePath())).isEqualTo("custom_world");
    }

    @Test
    void statistics_are_read_again_when_the_file_changes(@TempDir Path worldContainer) throws IOException {
        UUID uuid = UUID.fromString("eab53112-b93f-11ea-b3de-0242ac130004");
        Path statsFile = worldContainer.resolve("world").resolve("stats").resolve(uuid + ".json");
        Files.createDirectories(statsFile.getParent());
        PlayerStatisticLoaderFromFile loader = new PlayerStatisticLoaderFromFile(mockPluginWithWorldContainer(worldContainer));
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(uuid);

        writeJumps(statsFile, 1, FileTime.fromMillis(1_000_000L));
        assertThat(loader.getPlayerStatistics(player)).containsEntry(Statistic.JUMP, 1);

        // The player logs out, and the server writes the file again.
        writeJumps(statsFile, 7, FileTime.fromMillis(2_000_000L));
        assertThat(loader.getPlayerStatistics(player)).containsEntry(Statistic.JUMP, 7);
    }

    @Test
    void statistics_are_not_read_again_when_the_file_is_the_same(@TempDir Path worldContainer) throws IOException {
        UUID uuid = UUID.fromString("eab53112-b93f-11ea-b3de-0242ac130004");
        Path statsFile = worldContainer.resolve("world").resolve("stats").resolve(uuid + ".json");
        Files.createDirectories(statsFile.getParent());
        PlayerStatisticLoaderFromFile loader = new PlayerStatisticLoaderFromFile(mockPluginWithWorldContainer(worldContainer));
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(uuid);

        writeJumps(statsFile, 1, FileTime.fromMillis(1_000_000L));
        loader.getPlayerStatistics(player);

        writeJumps(statsFile, 7, FileTime.fromMillis(1_000_000L));
        assertThat(loader.getPlayerStatistics(player)).containsEntry(Statistic.JUMP, 1);
    }

    @Test
    void player_without_a_file_has_no_statistics(@TempDir Path worldContainer) {
        PlayerStatisticLoaderFromFile loader = new PlayerStatisticLoaderFromFile(mockPluginWithWorldContainer(worldContainer));
        OfflinePlayer player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        assertThat(loader.getPlayerStatistics(player)).isEmpty();
    }

    private static Plugin mockPluginWithWorldContainer(Path worldContainer) {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        when(plugin.getServer()).thenReturn(server);
        when(server.getWorldContainer()).thenReturn(worldContainer.toFile());
        return plugin;
    }

    private static void writeJumps(Path statsFile, int jumps, FileTime modified) throws IOException {
        Files.writeString(statsFile, "{\"stats\":{\"minecraft:custom\":{\"minecraft:jump\":" + jumps + "}}}");
        Files.setLastModifiedTime(statsFile, modified);
    }
}
