package de.sldk.mc.metrics;

import io.prometheus.client.Collector;
import io.prometheus.client.CollectorRegistry;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

public abstract class Metric {

    private final static String COMMON_PREFIX = "mc_";

    private final Plugin plugin;
    private final Collector collector;

    private boolean enabled = false;

    protected Metric(Plugin plugin, Collector collector) {
        this.plugin = plugin;
        this.collector = collector;
    }

    protected Plugin getPlugin() {
        return plugin;
    }

    /**
     * Collects the metric. A metric that is not async capable runs on the global region thread. On Paper, that is the
     * main thread. On Folia, it is the thread that ticks the global region. A failure is logged, and the future
     * completes normally, so that one metric cannot fail the scrape.
     */
    @Nullable
    public CompletableFuture<Void> collect() {
        if (!isEnabled()) {
            return null;
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        Runnable task = () -> {
            try {
                doCollectAsync().whenComplete((result, e) -> {
                    if (e != null) {
                        logException(e);
                    }
                    future.complete(null);
                });
            } catch (Exception e) {
                logException(e);
                future.complete(null);
            }
        };

        try {
            if (isAsyncCapable()) {
                CompletableFuture.runAsync(task);
            } else {
                Bukkit.getGlobalRegionScheduler().execute(plugin, task);
            }
        } catch (Exception e) {
            logException(e);
            future.complete(null);
        }

        return future;
    }

    protected abstract void doCollect();

    /**
     * Collects the metric, and completes when the values are set. Override it when the values come from other
     * threads, for example from the threads that own entities on Folia.
     */
    protected CompletableFuture<Void> doCollectAsync() {
        doCollect();
        return CompletableFuture.completedFuture(null);
    }

    /**
     * This method is called during the Metric collection
     * to determine if it is safe to do it async.
     * By default, all Metrics are sync unless this method
     * is overridden.
     */
    protected boolean isAsyncCapable() {
        return false;
    }

    private void logException(Throwable e) {
        final Logger log = plugin.getLogger();
        final String className = getClass().getSimpleName();

        log.throwing(className, "collect", e);
        log.info("Failed to collect " + className + ": ");
        e.printStackTrace();
    }

    protected static String prefix(String name) {
        return COMMON_PREFIX + name;
    }

    public void enable() {
        CollectorRegistry.defaultRegistry.register(collector);
        enabled = true;
    }

    public void disable() {
        CollectorRegistry.defaultRegistry.unregister(collector);
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
