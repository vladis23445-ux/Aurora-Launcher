package dev.aurora.event;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;

public final class Events {

    private Events() {
    }

    /** Изменение здоровья сущности на клиенте (ловим в LivingEntityMixin#setHealth). */
    public record HealthChange(LivingEntity entity, float delta) {
    }

    /** Игрок атаковал сущность (ClientPlayerInteractionManager#attackEntity). */
    public record Attack(LivingEntity target, boolean critical) {
    }

    /** Локальный игрок прыгнул. */
    public record Jump(Vec3d position) {
    }

    /** Один клиентский тик. */
    public record Tick() {
    }

    /** Нажатие/отпускание клавиши, action = GLFW action. */
    public record Key(int key, int action) {
    }
}
