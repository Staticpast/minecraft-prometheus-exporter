package de.sldk.mc.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import io.prometheus.client.CollectorRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Villager;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class VillagersTest {

	private static final String VILLAGERS_METRIC_NAME = "mc_villagers_total";
	private static final String[] METRIC_LABELS = new String[] {"world", "type", "profession", "level"};
	private static final String WORLD_NAME = "world_name";

	private static final CollectorRegistry REGISTRY = CollectorRegistry.defaultRegistry;

	private MockedStatic<Bukkit> bukkit;
	private Plugin plugin;
	private Villagers villagersMetric;
	private World world;

	@BeforeEach
	void beforeEachTest() {
		REGISTRY.clear();
		plugin = mock(Plugin.class);
		villagersMetric = new Villagers(plugin);
		villagersMetric.enable();
		world = mock(World.class);
		when(world.getName()).thenReturn(WORLD_NAME);
		bukkit = mockStatic(Bukkit.class);
		bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
	}

	@AfterEach
	void afterEachTest() {
		bukkit.close();
	}

	@Test
	void givenVillagersExpectCorrectCount() throws Exception {
		List<Villager> villagers = new ArrayList<>();
		for (int i = 0; i < 2; i++) {
			villagers.add(mockVillager(mockType("desert"), mockProfession("farmer"), 1));
		}
		for (int i = 0; i < 3; i++) {
			villagers.add(mockVillager(mockType("plains"), mockProfession("none"), 2));
		}
		when(world.getEntitiesByClass(Villager.class)).thenReturn(villagers);

		villagersMetric.doCollectAsync().get(1, TimeUnit.SECONDS);

		assertThat(sample("desert", "farmer", "1")).isEqualTo(2);
		assertThat(sample("plains", "none", "2")).isEqualTo(3);
	}

	@Test
	void removedVillagerIsNotCounted() throws Exception {
		Villager removed = mock(Villager.class);
		EntityScheduler scheduler = mock(EntityScheduler.class);
		when(removed.getScheduler()).thenReturn(scheduler);
		when(scheduler.run(eq(plugin), any(), any())).thenReturn(null);
		Villager present = mockVillager(mockType("desert"), mockProfession("farmer"), 1);
		when(world.getEntitiesByClass(Villager.class)).thenReturn(List.of(removed, present));

		villagersMetric.doCollectAsync().get(1, TimeUnit.SECONDS);

		assertThat(sample("desert", "farmer", "1")).isEqualTo(1);
	}

	private Double sample(String type, String profession, String level) {
		return REGISTRY.getSampleValue(VILLAGERS_METRIC_NAME, METRIC_LABELS,
				new String[] {WORLD_NAME, type, profession, level});
	}

	// Villager types and professions are registry values. Their constants need a running server.
	private static Villager.Type mockType(String key) {
		Villager.Type type = mock(Villager.Type.class);
		when(type.getKey()).thenReturn(NamespacedKey.minecraft(key));
		return type;
	}

	private static Villager.Profession mockProfession(String key) {
		Villager.Profession profession = mock(Villager.Profession.class);
		when(profession.getKey()).thenReturn(NamespacedKey.minecraft(key));
		return profession;
	}

	/**
	 * The entity scheduler of the villager runs each task at once.
	 */
	private Villager mockVillager(Villager.Type type, Villager.Profession profession, int level) {
		Villager villager = mock(Villager.class);
		when(villager.getVillagerType()).thenReturn(type);
		when(villager.getProfession()).thenReturn(profession);
		when(villager.getVillagerLevel()).thenReturn(level);

		EntityScheduler scheduler = mock(EntityScheduler.class);
		when(villager.getScheduler()).thenReturn(scheduler);
		when(scheduler.run(eq(plugin), any(), any())).thenAnswer(invocation -> {
			ScheduledTask task = mock(ScheduledTask.class);
			invocation.<Consumer<ScheduledTask>>getArgument(1).accept(task);
			return task;
		});
		return villager;
	}
}
