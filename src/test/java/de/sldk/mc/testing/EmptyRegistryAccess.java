package de.sldk.mc.testing;

import static org.mockito.Mockito.mock;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Keyed;
import org.bukkit.Registry;

/**
 * The Paper API loads registry values, such as villager types, from the server. Tests have no server. This
 * implementation gives empty registries, so that tests can load these classes and mock their values.
 */
public class EmptyRegistryAccess implements RegistryAccess {

    @Override
    @SuppressWarnings({"unchecked", "removal"})
    public <T extends Keyed> Registry<T> getRegistry(Class<T> type) {
        return mock(Registry.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Keyed> Registry<T> getRegistry(RegistryKey<T> key) {
        return mock(Registry.class);
    }
}
