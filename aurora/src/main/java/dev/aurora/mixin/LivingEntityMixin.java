package dev.aurora.mixin;

import dev.aurora.Aurora;
import dev.aurora.event.Events;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Р”Р°РЅРЅС‹Рµ РґР»СЏ Damage Numbers (РґРµР»СЊС‚Р° Р·РґРѕСЂРѕРІСЊСЏ) Рё Jump Circles.
 * setHealth РІС‹Р·С‹РІР°РµС‚СЃСЏ Рё РїСЂРё СѓСЂРѕРЅРµ, Рё РїСЂРё Р»РµС‡РµРЅРёРё вЂ” РѕРґРЅРѕРіРѕ С…СѓРєР° С…РІР°С‚Р°РµС‚.
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
        } catch (Throwable ignored) {
        }
    }
}
