package io.github.prismwork.emitrades.util;

import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;

public interface TradeProfile {
    VillagerTrades.ItemListing offer();

    VillagerProfession profession();

    int level();

    AbstractVillager villager();

    record DefaultImpl(VillagerProfession profession,
                       VillagerTrades.ItemListing offer,
                       int level,
                       AbstractVillager villager) implements TradeProfile {
        @Override
        public VillagerTrades.ItemListing offer() {
            return offer;
        }

        @Override
        public VillagerProfession profession() {
            return profession;
        }

        @Override
        public int level() {
            return level;
        }

        @Override
        public AbstractVillager villager() {
            return villager;
        }
    }
}
