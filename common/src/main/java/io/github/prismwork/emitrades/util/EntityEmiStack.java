package io.github.prismwork.emitrades.util;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.emi.emi.EmiPort;
import dev.emi.emi.EmiUtil;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.screen.tooltip.RemainderTooltipComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("DuplicatedCode")
public class EntityEmiStack extends EmiStack {
    private final @Nullable Entity entity;
    private final float scale;

    protected EntityEmiStack(@Nullable Entity entity, float scale) {
        this.entity = entity;
        this.scale = scale;
    }

    public static EntityEmiStack of(@Nullable Entity entity) {
        return ofScaled(entity, 8.0f);
    }

    public static EntityEmiStack ofScaled(@Nullable Entity entity, float scale) {
        return new EntityEmiStack(entity, scale);
    }

    @Override
    public EmiStack copy() {
        EntityEmiStack stack = ofScaled(entity, scale);
        stack.setRemainder(getRemainder().copy());
        stack.comparison = comparison;
        return stack;
    }

    @Override
    public boolean isEmpty() {
        return entity == null;
    }

    @Override
    public void render(GuiGraphics draw, int x, int y, float delta, int flags) {
        if (entity != null) {
            MouseHandler mouse = Minecraft.getInstance().mouseHandler;
            if (entity instanceof LivingEntity living)
                drawLivingEntity(draw, x, y, scale, (float) mouse.xpos(), (float) mouse.ypos(), living);
            else
                drawEntity(draw, x, y, scale, (float) mouse.xpos(), (float) mouse.ypos(), entity);
        }
    }

    @Override
    public DataComponentPatch getComponentChanges() {
        return DataComponentPatch.EMPTY;
    }

    @Override
    public Object getKey() {
        return entity;
    }

    @Override
    public ResourceLocation getId() {
        if (entity == null) throw new RuntimeException("Entity is null");
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
    }

    @Override
    public List<Component> getTooltipText() {
        return List.of(getName());
    }

    @Override
    public List<ClientTooltipComponent> getTooltip() {
        List<ClientTooltipComponent> list = new ArrayList<>();
        if (entity != null) {
            list.addAll(getTooltipText().stream().map(EmiPort::ordered).map(ClientTooltipComponent::create).toList());
            String mod;
            if (entity instanceof Villager villager) {
                mod = EmiUtil.getModName(BuiltInRegistries.VILLAGER_PROFESSION.getKey(villager.getVillagerData().getProfession()).getNamespace());
            } else {
                mod = EmiUtil.getModName(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace());
            }
            list.add(ClientTooltipComponent.create(EmiPort.ordered(EmiPort.literal(mod, ChatFormatting.BLUE, ChatFormatting.ITALIC))));
            if (!getRemainder().isEmpty()) {
                list.add(new RemainderTooltipComponent(this));
            }
        }
        return list;
    }

    @Override
    public Component getName() {
        return entity != null ? entity.getName() : EmiPort.literal("yet another missingno");
    }

    public static void drawLivingEntity(GuiGraphics ctx, int x, int y, float size, float mouseX, float mouseY, LivingEntity entity) {
        float mouseX0 = (ctx.guiWidth() + 51) - mouseX;
        float mouseY0 = (ctx.guiHeight() + 75 - 50) - mouseY;
        float f = (float) Math.atan(mouseX0 / 40.0F);
        float g = (float) Math.atan(mouseY0 / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float h = entity.yBodyRot;
        float i = entity.getYRot();
        float j = entity.getXRot();
        float k = entity.yHeadRotO;
        float l = entity.yHeadRot;
        entity.yBodyRot = 180.0F + f * 20.0F;
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        draw(ctx, x, y, size, quaternionf, quaternionf2, entity);
        entity.yBodyRot = h;
        entity.setYRot(i);
        entity.setXRot(j);
        entity.yHeadRotO = k;
        entity.yHeadRot = l;
    }

    public static void drawEntity(GuiGraphics ctx, int x, int y, float size, float mouseX, float mouseY, Entity entity) {
        float mouseX0 = (ctx.guiWidth() + 51) - mouseX;
        float mouseY0 = (ctx.guiHeight() + 75 - 50) - mouseY;
        float f = (float) Math.atan(mouseX0 / 40.0F);
        float g = (float) Math.atan(mouseY0 / 40.0F);
        Quaternionf quaternionf = (new Quaternionf()).rotateZ(3.1415927F);
        Quaternionf quaternionf2 = (new Quaternionf()).rotateX(g * 20.0F * 0.017453292F);
        quaternionf.mul(quaternionf2);
        float i = entity.getYRot();
        float j = entity.getXRot();
        entity.setYRot(180.0F + f * 40.0F);
        entity.setXRot(-g * 20.0F);
        draw(ctx, x, y, size, quaternionf, quaternionf2, entity);
        entity.setYRot(i);
        entity.setXRot(j);
    }

    @SuppressWarnings("deprecation")
    private static void draw(GuiGraphics ctx, int x, int y, float size, Quaternionf quaternion, @Nullable Quaternionf quaternion2, Entity entity) {
        ctx.pose().pushPose();
        ctx.pose().translate(x + 8, y + 16, 50.0);
        ctx.pose().mulPose((new Matrix4f()).scaling(size, size, -size));
        ctx.pose().mulPose(quaternion);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
        if (quaternion2 != null) {
            quaternion2.conjugate();
            dispatcher.overrideCameraOrientation(quaternion2);
        }

        dispatcher.setRenderShadow(false);
        RenderSystem.runAsFancy(() ->
                dispatcher.render(entity, 0.0, 0.0, 0.0, 0.0F, 1.0F, ctx.pose(), ctx.bufferSource(), 15728880)
        );
        ctx.flush();
        dispatcher.setRenderShadow(true);
        ctx.pose().popPose();
        Lighting.setupFor3DItems();
    }
}
