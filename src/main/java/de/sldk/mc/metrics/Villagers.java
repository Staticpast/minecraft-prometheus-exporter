package de.sldk.mc.metrics;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.prometheus.client.Gauge;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Get total count of Villagers.
 * <p>
 * Labelled by
 * <ul>
 *     <li> World ({@link World#getName()})
 *     <li> Type, e.g. 'desert', 'plains ({@link org.bukkit.entity.Villager.Type})
 *     <li> Profession, e.g. 'fisherman', 'farmer', or 'none' ({@link org.bukkit.entity.Villager.Profession})
 *     <li> Level ({@link Villager#getVillagerLevel()})
 * </ul>
 */
public class Villagers extends Metric {

    private static final Gauge VILLAGERS = Gauge.build()
            .name(prefix("villagers_total"))
            .help("Villagers total count, labelled by world, type, profession, and level")
            .labelNames("world", "type", "profession", "level")
            .create();

    public Villagers(Plugin plugin) {
        super(plugin, VILLAGERS);
    }

    @Override
    protected void doCollect() {
        throw new UnsupportedOperationException("Villagers collects with doCollectAsync");
    }

    /**
     * On Folia, the state of a villager can be read only on the thread that owns the villager. Thus each villager is
     * read on its entity scheduler. On Paper, that is the main thread.
     */
    @Override
    protected CompletableFuture<Void> doCollectAsync() {
        List<CompletableFuture<VillagerGrouping>> groupings = new ArrayList<>();
        for (World world : Bukkit.getWorlds()) {
            for (Villager villager : world.getEntitiesByClass(Villager.class)) {
                groupings.add(read(world.getName(), villager));
            }
        }

        return CompletableFuture.allOf(groupings.toArray(CompletableFuture[]::new)).thenRun(() -> {
            Map<VillagerGrouping, Long> counts = groupings.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            VILLAGERS.clear();
            counts.forEach((grouping, count) -> VILLAGERS
                    .labels(grouping.world(), grouping.type(), grouping.profession(), Integer.toString(grouping.level()))
                    .set(count));
        });
    }

    /**
     * Completes with null when the villager is removed before the read.
     */
    private CompletableFuture<VillagerGrouping> read(String world, Villager villager) {
        CompletableFuture<VillagerGrouping> grouping = new CompletableFuture<>();
        ScheduledTask task = villager.getScheduler().run(getPlugin(),
                t -> grouping.complete(new VillagerGrouping(world, villager)),
                () -> grouping.complete(null));
        if (task == null) {
            grouping.complete(null);
        }
        return grouping;
    }

    private record VillagerGrouping(String world, String type, String profession, int level) {

        VillagerGrouping(String world, Villager villager) {
            this(world, villager.getVillagerType().getKey().getKey(), villager.getProfession().getKey().getKey(),
                    villager.getVillagerLevel());
        }
    }
}
