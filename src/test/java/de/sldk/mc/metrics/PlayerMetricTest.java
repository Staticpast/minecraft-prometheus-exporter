package de.sldk.mc.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.destroystokyo.paper.profile.PlayerProfile;
import java.util.UUID;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * For an offline player, {@link OfflinePlayer#getName()} can read the player's data file on the main thread. The
 * name lookup must use the user cache first and read each name only once.
 */
@ExtendWith(MockitoExtension.class)
class PlayerMetricTest {

    private static final UUID UID = UUID.fromString("eab53112-b93f-11ea-b3de-0242ac130004");

    @Mock
    private Plugin plugin;

    @Mock
    private OfflinePlayer offlinePlayer;

    @Mock
    private PlayerProfile profile;

    private PlayerMetric metric;

    @BeforeEach
    void setUp() {
        metric = new PlayerOnline(plugin);
        when(offlinePlayer.getUniqueId()).thenReturn(UID);
    }

    @Test
    void offline_name_comes_from_the_user_cache() {
        mockUserCache("cached_name");

        assertThat(metric.getNameOrUid(offlinePlayer)).isEqualTo("cached_name");
        verify(offlinePlayer, never()).getName();
    }

    @Test
    void offline_name_is_looked_up_only_once() {
        mockUserCache("cached_name");

        metric.getNameOrUid(offlinePlayer);
        metric.getNameOrUid(offlinePlayer);

        verify(offlinePlayer, times(1)).getPlayerProfile();
    }

    @Test
    void offline_name_falls_back_to_the_player_data() {
        when(offlinePlayer.getPlayerProfile()).thenReturn(profile);
        when(profile.completeFromCache()).thenReturn(false);
        when(offlinePlayer.getName()).thenReturn("data_name");

        assertThat(metric.getNameOrUid(offlinePlayer)).isEqualTo("data_name");
    }

    @Test
    void offline_player_without_a_name_gets_the_uid() {
        when(offlinePlayer.getPlayerProfile()).thenReturn(profile);
        when(profile.completeFromCache()).thenReturn(false);

        assertThat(metric.getNameOrUid(offlinePlayer)).isEqualTo(UID.toString());
    }

    @Test
    void online_name_replaces_the_cached_name() {
        mockUserCache("old_name");
        metric.getNameOrUid(offlinePlayer);

        Player onlinePlayer = mock(Player.class);
        when(onlinePlayer.getName()).thenReturn("new_name");
        when(offlinePlayer.getPlayer()).thenReturn(onlinePlayer);
        assertThat(metric.getNameOrUid(offlinePlayer)).isEqualTo("new_name");

        when(offlinePlayer.getPlayer()).thenReturn(null);
        assertThat(metric.getNameOrUid(offlinePlayer)).isEqualTo("new_name");
    }

    private void mockUserCache(String name) {
        when(offlinePlayer.getPlayerProfile()).thenReturn(profile);
        when(profile.completeFromCache()).thenReturn(true);
        when(profile.getName()).thenReturn(name);
    }
}
