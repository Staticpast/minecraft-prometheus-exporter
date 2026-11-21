package de.sldk.mc.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

/**
 * A metric that is not async capable runs on the global region scheduler, which works on Paper and on Folia. A metric
 * that fails must not fail the scrape.
 */
class MetricTest {

    private MockedStatic<Bukkit> bukkit;
    private GlobalRegionScheduler scheduler;
    private Plugin plugin;

    @BeforeEach
    void setUp() {
        CollectorRegistry.defaultRegistry.clear();
        bukkit = mockStatic(Bukkit.class);
        scheduler = mock(GlobalRegionScheduler.class);
        bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);
        plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("MetricTest"));
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void sync_metric_runs_on_the_global_region_scheduler() throws Exception {
        runTasksAtOnce();
        AtomicInteger runs = new AtomicInteger();
        Metric metric = new TestMetric(plugin, runs::incrementAndGet);
        metric.enable();

        metric.collect().get(1, TimeUnit.SECONDS);

        assertThat(runs).hasValue(1);
    }

    @Test
    void failing_metric_completes_the_future() throws Exception {
        runTasksAtOnce();
        Metric metric = new TestMetric(plugin, () -> {
            throw new IllegalStateException("broken");
        });
        metric.enable();

        CompletableFuture<Void> future = metric.collect();

        future.get(1, TimeUnit.SECONDS);
        assertThat(future).isCompletedWithValue(null);
    }

    @Test
    void scheduler_that_throws_completes_the_future() throws Exception {
        doThrow(new UnsupportedOperationException()).when(scheduler).execute(eq(plugin), any(Runnable.class));
        Metric metric = new TestMetric(plugin, () -> {
        });
        metric.enable();

        CompletableFuture<Void> future = metric.collect();

        future.get(1, TimeUnit.SECONDS);
        assertThat(future).isCompletedWithValue(null);
    }

    private void runTasksAtOnce() {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(1).run();
            return null;
        }).when(scheduler).execute(eq(plugin), any(Runnable.class));
    }

    private static class TestMetric extends Metric {

        private static final Gauge GAUGE = Gauge.build().name("mc_test_metric").help("Test").create();

        private final Runnable collect;

        TestMetric(Plugin plugin, Runnable collect) {
            super(plugin, GAUGE);
            this.collect = collect;
        }

        @Override
        protected void doCollect() {
            collect.run();
        }
    }
}
