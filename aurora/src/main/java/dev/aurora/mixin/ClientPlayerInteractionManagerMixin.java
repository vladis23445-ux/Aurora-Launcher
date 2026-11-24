package dev.aurora.mixin;

import dev.aurora.Aurora;
import dev.aurora.event.Events;
import dev.aurora.util.QuestTracker;
import dev.aurora.util.TargetManager;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Ловим атаку игрока: запоминаем цель (для TargetESP/TargetHUD)
 * и определяем критический удар по ванильным условиям.
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(method = "attackEntity", at = @At("HEAD"))
    private void aurora$onAttack(PlayerEntity player, Entity target, CallbackInfo ci) {
        if (Aurora.get() == null || !(target instanceof LivingEntity living)) {
            return;
        }
        boolean critical = player.fallDistance > 0.0f
                && !player.isOnGround()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.isInLava();

        try {
            TargetManager.setTarget(living);
            Aurora.get().getBus().post(new Events.Attack(living, critical));
        } catch (Throwable ignored) {
        }
    }

    /** Счётчик добытых блоков для заданий. */
    @Inject(method = "breakBlock", at = @At("HEAD"), require = 0)
    private void aurora$onBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (Aurora.get() == null) {
            return;
        }
        QuestTracker.onBlockBroken();
    }
}