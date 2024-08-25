package io.github.prismwork.emitrades;

import com.google.common.collect.ImmutableSet;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import io.github.prismwork.emitrades.config.EMITradesConfig;
import io.github.prismwork.emitrades.recipe.VillagerTrade;
import io.github.prismwork.emitrades.util.EntityEmiStack;
import io.github.prismwork.emitrades.util.TradeProfile;
import io.github.prismwork.emitrades.util.XPlatUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

@EmiEntrypoint
public class EMITradesPlugin implements EmiPlugin {
    public static final Logger LOGGER = LoggerFactory.getLogger("EMI Trades");
    public static final VillagerProfession WANDERING_TRADER_PLACEHOLDER = new VillagerProfession(
            "wandering_trader",
            entry -> false,
            entry -> false,
            ImmutableSet.of(),
            ImmutableSet.of(),
            SoundEvents.WANDERING_TRADER_YES
    );
    public static final EmiRecipeCategory VILLAGER_TRADES
            = new EmiRecipeCategory(ResourceLocation.fromNamespaceAndPath("emitrades", "villager_trades"), EmiStack.of(Items.EMERALD));
    public static EMITradesConfig.Config CONFIG;
    private static final File CONFIG_FILE = XPlatUtils.INSTANCE.getConfigPath().resolve("emitrades.json").toFile();

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void register(EmiRegistry registry) {
        CONFIG = EMITradesConfig.load(CONFIG_FILE);
        registry.addCategory(VILLAGER_TRADES);
        RandomSource random = RandomSource.create();
        for (VillagerProfession profession : BuiltInRegistries.VILLAGER_PROFESSION) {
            Villager villager = (Villager)
                    BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", "villager")).create(Minecraft.getInstance().level);
            if (villager != null) {
                villager.setVillagerData(villager.getVillagerData().setProfession(profession).setLevel(5));
                registry.addWorkstation(VILLAGER_TRADES, EntityEmiStack.ofScaled(villager, 8.0f));
            }
            AtomicInteger id = new AtomicInteger();
            Int2ObjectMap<VillagerTrades.ItemListing[]> offers = VillagerTrades.TRADES.get(profession);
            if (offers == null || offers.isEmpty()) continue;
            int level = 0;
            while (level < 5) {
                Villager villager1 = (Villager)
                        BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", "villager")).create(Minecraft.getInstance().level);
                if (villager1 != null) {
                    villager1.setVillagerData(villager1.getVillagerData().setProfession(profession).setLevel(level + 1));
                }
                for (VillagerTrades.ItemListing offer : offers.get(level + 1)) {
                    if (isVanillaFactory(offer)) {
                        registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(profession, offer, level + 1, villager1), id.get()));
                        id.getAndIncrement();
                    } else {
                        try {
                            int attempts = 5;
                            TreeSet<MerchantOffer> genOffers = new TreeSet<>(this::compareOffers);
                            MerchantOffer inOffer;
                            while (attempts > 0) {
                                inOffer = offer.getOffer(Minecraft.getInstance().player, random);
                                if (genOffers.add(inOffer))
                                    attempts++;
                                else
                                    attempts--;
                            }
                            int finalLevel = level;
                            genOffers.forEach(tradeOffer -> {
                                registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(profession, new FakeFactory(tradeOffer), finalLevel + 1, villager1), id.get()));
                                id.getAndIncrement();
                            });
                        } catch (Exception ignored) {}
                    }
                }
                level++;
            }
        }
        WanderingTrader wanderingTrader = (WanderingTrader) BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.fromNamespaceAndPath("minecraft", "wandering_trader"))
                .create(Minecraft.getInstance().level);
        registry.addWorkstation(VILLAGER_TRADES, EntityEmiStack.of(wanderingTrader));
        AtomicInteger wanderingTraderId = new AtomicInteger();
        VillagerTrades.WANDERING_TRADER_TRADES.forEach((lvl, offers) -> {
            for (VillagerTrades.ItemListing offer : offers) {
                if (isVanillaFactory(offer)) {
                    registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(WANDERING_TRADER_PLACEHOLDER, offer, lvl, wanderingTrader), wanderingTraderId.get()));
                    wanderingTraderId.getAndIncrement();
                } else {
                    try {
                        int attempts = 5;
                        TreeSet<MerchantOffer> genOffers = new TreeSet<>(this::compareOffers);
                        MerchantOffer inOffer;
                        while (attempts > 0) {
                            inOffer = offer.getOffer(Minecraft.getInstance().player, random);
                            if (genOffers.add(inOffer))
                                attempts++;
                            else
                                attempts--;
                        }
                        int finalLevel = lvl;
                        genOffers.forEach(tradeOffer -> {
                            registry.addRecipe(new VillagerTrade(new TradeProfile.DefaultImpl(WANDERING_TRADER_PLACEHOLDER, new FakeFactory(tradeOffer), finalLevel, wanderingTrader), wanderingTraderId.get()));
                            wanderingTraderId.getAndIncrement();
                        });
                    } catch (Exception ignored) {}
                }
            }
        });
        LOGGER.info("Reloaded.");
    }

    private static boolean isVanillaFactory(VillagerTrades.ItemListing offer) {
        return offer instanceof VillagerTrades.SuspiciousStewForEmerald ||
                offer instanceof VillagerTrades.EnchantedItemForEmeralds ||
                offer instanceof VillagerTrades.EnchantBookForEmeralds ||
                offer instanceof VillagerTrades.TreasureMapForEmeralds ||
                offer instanceof VillagerTrades.TippedArrowForItemsAndEmeralds ||
                offer instanceof VillagerTrades.DyedArmorForEmeralds ||
                offer instanceof VillagerTrades.EmeraldsForVillagerTypeItem ||
                offer instanceof VillagerTrades.ItemsForEmeralds ||
                offer instanceof VillagerTrades.EmeraldForItems ||
                offer instanceof VillagerTrades.ItemsAndEmeraldsToItems;
    }

    private int compareOffers(@NotNull MerchantOffer a, @NotNull MerchantOffer b) {
        int diff = BuiltInRegistries.ITEM.getId(a.getBaseCostA().getItem()) - BuiltInRegistries.ITEM.getId(b.getBaseCostA().getItem());
        if (diff != 0) return diff;
        diff = a.getItemCostB().map(offer -> BuiltInRegistries.ITEM.getId(offer.item().value())).orElse(BuiltInRegistries.ITEM.size()) - b.getItemCostB().map(offer -> BuiltInRegistries.ITEM.getId(offer.item().value())).orElse(BuiltInRegistries.ITEM.size());
        if (diff != 0) return diff;
        diff = BuiltInRegistries.ITEM.getId(a.getResult().getItem()) - BuiltInRegistries.ITEM.getId(b.getResult().getItem());
        return diff;
    }

    @ApiStatus.Internal
    public static final class FakeFactory
            implements VillagerTrades.ItemListing {
        public final ItemStack first;
        public final ItemStack second;
        public final ItemStack sell;

        public FakeFactory(MerchantOffer offer) {
            this.first = offer.getBaseCostA();
            this.second = offer.getItemCostB().map(ItemCost::itemStack).orElse(ItemStack.EMPTY);
            this.sell = offer.getResult();
        }

        @Nullable
        @Override
        public  MerchantOffer getOffer(Entity entity, RandomSource randomSource) {
            throw new AssertionError("Nobody should use this");
        }
    }
}
