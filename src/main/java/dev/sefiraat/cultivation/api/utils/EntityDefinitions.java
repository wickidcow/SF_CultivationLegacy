package dev.sefiraat.cultivation.api.utils;

import dev.sefiraat.cultivation.Cultivation;
import dev.sefiraat.sefilib.entity.LivingEntityCategory;
import dev.sefiraat.sefilib.entity.LivingEntityDefinition;
import dev.sefiraat.sefilib.entity.LivingEntitySelector;
import org.bukkit.Server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class EntityDefinitions {

    private EntityDefinitions() {
        throw new IllegalStateException("Utility class");
    }

    private static Set<LivingEntityDefinition> passiveMobs;
    private static Set<LivingEntityDefinition> hostileMobs;
    private static Set<LivingEntityDefinition> bossMobs;
    private static Set<LivingEntityDefinition> flyingMobs;

    static {
        Server server = Cultivation.getInstance().getServer();

        try {
            passiveMobs = LivingEntitySelector.start()
                .includeCategories(LivingEntityCategory.PASSIVE)
                .process(LivingEntitySelector.MatchType.MATCH_ALL);
        } catch (Exception e) {
            passiveMobs = new HashSet<>();
            server.getLogger().severe(e.getMessage());
        }

        try {
            hostileMobs = LivingEntitySelector.start()
                .includeCategories(LivingEntityCategory.HOSTILE)
                .process(LivingEntitySelector.MatchType.MATCH_ALL);
        } catch (Exception e) {
            hostileMobs = new HashSet<>();
            server.getLogger().severe(e.getMessage());
        }

        try {
            bossMobs = LivingEntitySelector.start()
                .includeCategories(LivingEntityCategory.BOSS)
                .process(LivingEntitySelector.MatchType.MATCH_ALL);
        } catch (Exception e) {
            bossMobs = new HashSet<>();
            server.getLogger().severe(e.getMessage());
        }

        try {
            flyingMobs = LivingEntitySelector.start()
                .includeCategories(LivingEntityCategory.FLYING)
                .process(LivingEntitySelector.MatchType.MATCH_ALL);
        } catch (Exception e) {
            flyingMobs = new HashSet<>();
            server.getLogger().severe(e.getMessage());
        }
    }

    public static Set<LivingEntityDefinition> getPassiveMobs() {
        return Collections.unmodifiableSet(passiveMobs);
    }

    public static Set<LivingEntityDefinition> getHostileMobs() {
        return Collections.unmodifiableSet(hostileMobs);
    }

    public static Set<LivingEntityDefinition> getBossMobs() {
        return Collections.unmodifiableSet(bossMobs);
    }

    public static Set<LivingEntityDefinition> getFlyingMobs() {
        return flyingMobs;
    }
}
