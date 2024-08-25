package io.github.prismwork.emitrades.recipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import io.github.prismwork.emitrades.EMITradesPlugin;
import io.github.prismwork.emitrades.util.EntityEmiStack;
import io.github.prismwork.emitrades.util.ListEmiStack;
import io.github.prismwork.emitrades.util.TradeProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VillagerTrade implements EmiRecipe {
    private final TradeProfile profile;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final List<EmiIngredient> catalysts;
    private final int id;
    private final MutableComponent title;

    @SuppressWarnings("UnstableApiUsage")
    public VillagerTrade(TradeProfile profile, int id) {
        this.profile = profile;
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.catalysts = profile.villager() != null ?
                List.of(EntityEmiStack.ofScaled(profile.villager(), 12.0f)) : List.of();
        this.id = id;
        VillagerProfession internalProf = profile.profession();
        if (internalProf.equals(EMITradesPlugin.WANDERING_TRADER_PLACEHOLDER)) {
            this.title = Component.translatable("emi.emitrades.placeholder.wandering_trader");
        } else {
            this.title = Component.translatable("entity.minecraft.villager." + profile.profession().name().substring(profile.profession().name().lastIndexOf(":") + 1))
                    .append(" - ").append(Component.translatable("emi.emitrades.profession.lvl." + profile.level()));
        }
        VillagerTrades.ItemListing offer = profile.offer();
        if (offer instanceof VillagerTrades.EmeraldForItems factory) {
            inputs.add(0, EmiStack.of(factory.itemStack.itemStack()));
            inputs.add(1, EmiStack.EMPTY);
            outputs.add(0, EmiStack.of(Items.EMERALD, factory.emeraldAmount));
        } else if (offer instanceof VillagerTrades.ItemsForEmeralds factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, factory.emeraldCost));
            inputs.add(1, EmiStack.EMPTY);
            outputs.add(0, EmiStack.of(factory.itemStack));
        } else if (offer instanceof VillagerTrades.SuspiciousStewForEmerald factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, 1));
            inputs.add(1, EmiStack.EMPTY);
            ItemStack stack = new ItemStack(Items.SUSPICIOUS_STEW, 1);
            stack.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new SuspiciousStewEffects(factory.effects.effects()));
            outputs.add(0, EmiStack.of(stack));
        } else if (offer instanceof VillagerTrades.ItemsAndEmeraldsToItems factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, factory.emeraldCost));
            inputs.add(1, EmiStack.of(factory.fromItem.itemStack()));
            outputs.add(0, EmiStack.of(factory.toItem));
        } else if (offer instanceof VillagerTrades.EnchantedItemForEmeralds factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, Math.min(factory.baseEmeraldCost + 5, 64)));
            inputs.add(1, EmiStack.EMPTY);

            List<EmiStack> out = new ArrayList<>();
            int enchantability = factory.itemStack.getItem().getEnchantmentValue();
            int power = 5 + 15 + 1
                    + (enchantability / 4 + 1) + (enchantability / 4 + 1);
            var tradeableEnchants = Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(EnchantmentTags.ON_TRADED_EQUIPMENT);
            tradeableEnchants.ifPresent(enchantments -> {
                EnchantmentHelper.getAvailableEnchantmentResults(power, factory.itemStack, enchantments.stream()).forEach(
                        entry -> {
                            Enchantment enchantment = entry.enchantment.value();
                            for (int i = enchantment.getMinLevel(); i <= enchantment.getMaxLevel(); i++){
                                ItemStack stack = factory.itemStack.copy();
                                stack.enchant(entry.enchantment, i);
                                out.add(EmiStack.of(stack));
                            }
                        }
                );
            });


            outputs.add(0, new ListEmiStack(out, factory.itemStack.getCount()));
        } else if (offer instanceof VillagerTrades.EmeraldsForVillagerTypeItem factory) {
            List<EmiStack> stacks = new ArrayList<>();
            factory.trades.values().forEach(item -> stacks.add(EmiStack.of(item)));
            inputs.add(0, new ListEmiIngredient(stacks, factory.cost));
            inputs.add(1, EmiStack.EMPTY);
            outputs.add(0, EmiStack.of(Items.EMERALD));
        } else if (offer instanceof VillagerTrades.TippedArrowForItemsAndEmeralds factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, factory.emeraldCost));
            inputs.add(1, EmiStack.of(factory.fromItem, factory.fromCount));

            List<EmiStack> out = new ArrayList<>();
            var brewingRegistry = Minecraft.getInstance().level.potionBrewing();
            BuiltInRegistries.POTION.holders().filter((potion) ->
                    !potion.value().getEffects().isEmpty() && brewingRegistry.isBrewablePotion(potion)).forEach(
                    potion -> {
                        ItemStack stack = factory.toItem.copy();
                        stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion));
                        out.add(EmiStack.of(stack));
                    }
            );

            outputs.add(0, new ListEmiStack(out, factory.toCount));
        } else if (offer instanceof VillagerTrades.EnchantBookForEmeralds factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, 5));
            inputs.add(1, EmiStack.of(Items.BOOK));

            List<EmiStack> out = new ArrayList<>();
            Minecraft.getInstance().level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getTag(factory.tradeableEnchantments).ifPresent(enchantments -> {
                enchantments.forEach(enchantmentEntry -> {
                    int min = Math.max(enchantmentEntry.value().getMinLevel(), factory.minLevel);
                    int max = Math.min(enchantmentEntry.value().getMaxLevel(), factory.maxLevel);

                    for (int i = min; i <= max; i++) {
                        ItemStack stack = EnchantedBookItem.createForEnchantment(
                                new EnchantmentInstance(enchantmentEntry, i)
                        );
                        out.add(EmiStack.of(stack));
                    }
                });
            });

            outputs.add(0, new ListEmiStack(out, 1));
        } else if (offer instanceof VillagerTrades.TreasureMapForEmeralds factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, factory.emeraldCost));
            inputs.add(1, EmiStack.of(Items.COMPASS));
            outputs.add(0, EmiStack.of(Items.FILLED_MAP));
        } else if (offer instanceof VillagerTrades.DyedArmorForEmeralds factory) {
            inputs.add(0, EmiStack.of(Items.EMERALD, factory.value));
            inputs.add(1, EmiStack.EMPTY);
            outputs.add(0, EmiStack.of(factory.item));
        } else if (offer instanceof EMITradesPlugin.FakeFactory factory) {
            inputs.add(0, EmiStack.of(factory.first));
            inputs.add(1, EmiStack.of(factory.second));
            outputs.add(0, EmiStack.of(factory.sell));
        } else {
            inputs.add(0, EmiStack.EMPTY);
            inputs.add(1, EmiStack.EMPTY);
            outputs.add(0, EmiStack.EMPTY);
        }
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return catalysts;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return EMITradesPlugin.VILLAGER_TRADES;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return ResourceLocation.fromNamespaceAndPath("emitrades", "villager_trades/" + profile.profession().name().substring(profile.profession().name().lastIndexOf(":") + 1) + "_" + id);
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        Font textRenderer = Minecraft.getInstance().font;
        int extraWidth = catalysts.isEmpty() ? 0 : 21;
        return (catalysts.isEmpty() || !EMITradesPlugin.CONFIG.enable3DVillagerModelInRecipes) ?
                Math.max(86, textRenderer.width(title) + 2) :
                Math.max(extraWidth + 85, extraWidth + textRenderer.width(title));
    }

    @Override
    public int getDisplayHeight() {
        return 28;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font textRenderer = Minecraft.getInstance().font;
        if (catalysts.isEmpty() || !EMITradesPlugin.CONFIG.enable3DVillagerModelInRecipes) {
            widgets.addText(title,
                    (getDisplayWidth() - textRenderer.width(title)) / 2, 0, 16777215, true);
            widgets.addSlot(inputs.get(0), getDisplayWidth() / 2 - 42, 10);
            widgets.addSlot(inputs.get(1), getDisplayWidth() / 2 - 22, 10);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, getDisplayWidth() / 2 - 3, 10);
            SlotWidget outputSlot = new SlotWidget(outputs.get(0), getDisplayWidth() / 2 + 22, 10).recipeContext(this);
            wrapOutput(widgets, outputSlot);
        } else {
            SlotWidget villagerSlot = new SlotWidget(catalysts.get(0), 1, 6).drawBack(false);
            if (profile.villager() instanceof Villager villager) {
                villagerSlot.appendTooltip(Component.translatable("emi.emitrades.profession.lvl." + villager.getVillagerData().getLevel()).withStyle(ChatFormatting.YELLOW));
            }
            widgets.add(villagerSlot);
            widgets.addText(title,
                    21, 0, 16777215, true);
            widgets.addSlot(inputs.get(0), 21, 10);
            widgets.addSlot(inputs.get(1), 41, 10);
            widgets.addTexture(EmiTexture.EMPTY_ARROW, 60, 10);
            SlotWidget outputSlot = new SlotWidget(outputs.get(0), 85, 10).recipeContext(this);
            wrapOutput(widgets, outputSlot);
        }
    }

    private void wrapOutput(WidgetHolder widgets, SlotWidget outputSlot) {
        if (profile.offer() instanceof VillagerTrades.DyedArmorForEmeralds) {
            outputSlot = outputSlot.appendTooltip(Component.translatable("emi.emitrades.random_colored").withStyle(ChatFormatting.YELLOW));
        } else if (profile.offer() instanceof VillagerTrades.SuspiciousStewForEmerald) {
            outputSlot = outputSlot.appendTooltip(Component.translatable("emi.emitrades.random_effect").withStyle(ChatFormatting.YELLOW));
        } else if (profile.offer() instanceof VillagerTrades.TreasureMapForEmeralds) {
            outputSlot = outputSlot.appendTooltip(Component.translatable("emi.emitrades.random_structure").withStyle(ChatFormatting.YELLOW));
        }
        widgets.add(outputSlot);
    }
}
