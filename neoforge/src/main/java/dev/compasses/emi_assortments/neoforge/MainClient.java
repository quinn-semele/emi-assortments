package dev.compasses.emi_assortments.neoforge;

import cc.abbie.emi_ores.forge.client.EmiOresForgeClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(value = "emi_assortments", dist = Dist.CLIENT)
public class MainClient {
    public MainClient(IEventBus modBus) {
        modBus.addListener(EmiOresForgeClient::init);
    }
}
