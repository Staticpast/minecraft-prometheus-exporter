package de.sldk.mc.metrics;

import de.sldk.mc.collectors.TpsCollector;
import io.prometheus.client.Gauge;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class Tps extends Metric {

    private static final Gauge TPS = Gauge.build()
            .name(prefix("tps"))
            .help("Server TPS (ticks per second)")
            .create();

    private ScheduledTask task;
    private TpsCollector tpsCollector = new TpsCollector();

    public Tps(Plugin plugin) {
        super(plugin, TPS);
    }

    /**
     * On Folia, the collector runs on the global region, so this is the TPS of the global region. Folia has no API for
     * the TPS of the other regions.
     */
    @Override
    public void enable() {
        super.enable();
        this.task = Bukkit.getGlobalRegionScheduler()
                .runAtFixedRate(getPlugin(), t -> tpsCollector.run(), 1, TpsCollector.POLL_INTERVAL);
    }

    @Override
    public void disable() {
        super.disable();
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void doCollect() {
        TPS.set(tpsCollector.getAverageTPS());
    }
}
