package dev.compasses.emi_assortments.neoforge;

import cc.abbie.emi_ores.forge.EmiOresForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod("emi_assortments")
public class Main {
    public Main(ModContainer container, IEventBus modBus) {
        new EmiOresForge(modBus);
    }
}
