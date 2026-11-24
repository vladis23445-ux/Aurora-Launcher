package dev.aurora.mixin;

import dev.aurora.Aurora;
import dev.aurora.event.Events;
import dev.aurora.modules.KillEffects;
import dev.aurora.util.QuestTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Данные для Damage Numbers (дельта здоровья) и Jump Circles.
 * setHealth вызывается и при уроне, и при лечении — одного хука хватает.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "setHealth", at = @At("HEAD"))
    private void aurora$onSetHealth(float health, CallbackInfo ci) {
        if (Aurora.get() == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getEntityWorld() == null || !self.getEntityWorld().isClient()) {
            return;
        }
        float delta = self.getHealth() - health;
        if (Math.abs(delta) < 0.05f) {
            return;
        }
        try {
            Aurora.get().getBus().post(new Events.HealthChange(self, delta));
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "onDeath", at = @At("HEAD"), require = 0)
    private void aurora$onDeath(net.minecraft.entity.damage.DamageSource source, CallbackInfo ci) {
        if (Aurora.get() == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.getEntityWorld() == null || !self.getEntityWorld().isClient()) {
            return;
        }
        // только убийства игроком и только не свой труп
        var attackerEntity = source.getAttacker();
        if (!(attackerEntity instanceof PlayerEntity attacker) || attacker != MinecraftClient.getInstance().player) {
            return;
        }
        if (self == MinecraftClient.getInstance().player) {
            return;
        }
        KillEffects.onEntityDeath(new net.minecraft.util.math.Vec3d(self.getX(), self.getY(), self.getZ()));
        QuestTracker.onKill();
    }

    @Inject(method = "jump", at = @At("HEAD"))
    private void aurora$onJump(CallbackInfo ci) {
        if (Aurora.get() == null) {
            return;
        }
        LivingEntity self = (LivingEntity) (Object) this;
        PlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || self != player) {
            return;
        }
        try {
            Aurora.get().getBus().post(new Events.Jump(new net.minecraft.util.math.Vec3d(self.getX(), self.getY(), self.getZ())));
            QuestTracker.onJump();
        } catch (Throwable ignored) {
        }
    }
}
