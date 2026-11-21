package de.sldk.mc.metrics;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.prometheus.client.Collector;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public abstract class PlayerMetric extends Metric {

    private final Map<UUID, String> names = new ConcurrentHashMap<>();

    public PlayerMetric(Plugin plugin, Collector collector) {
        super(plugin, collector);
    }

    @Override
    public final void doCollect() {

        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            collect(player);
        }
    }

    protected abstract void collect(OfflinePlayer player);

    protected String getUid(OfflinePlayer player) {
        return player.getUniqueId().toString();
    }

    /**
     * For an offline player, {@link OfflinePlayer#getName()} can read the player's data file from disk on the main
     * thread. On a server with many players this blocks each scrape. Thus the name of an offline player comes from the
     * server's user cache, and each name is read only once.
     */
    protected String getNameOrUid(OfflinePlayer player) {
        Player onlinePlayer = player.getPlayer();
        if (onlinePlayer != null) {
            String name = onlinePlayer.getName();
            names.put(player.getUniqueId(), name);
            return name;
        }
        return names.computeIfAbsent(player.getUniqueId(), uid -> getOfflineNameOrUid(player));
    }

    private static String getOfflineNameOrUid(OfflinePlayer player) {
        PlayerProfile profile = player.getPlayerProfile();
        String name = profile.completeFromCache() ? profile.getName() : null;
        if (name == null) {
            name = player.getName();
        }
        return name != null ? name : player.getUniqueId().toString();
    }

}
