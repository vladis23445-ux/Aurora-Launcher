package dev.aurora.modules;

import dev.aurora.event.EventBus;
import dev.aurora.module.Category;
import dev.aurora.module.Module;

/** AutoSprint: автоматически включает бег, когда игрок идёт вперёд. */
public class AutoSprint extends Module {

    public AutoSprint(EventBus bus) {
        super(bus, "AutoSprint", "Автоматический бег вперёд", Category.MISC);
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.options == null) {
            return;
        }
        if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking()) {
            mc.player.setSprinting(true);
        }
    }
}
