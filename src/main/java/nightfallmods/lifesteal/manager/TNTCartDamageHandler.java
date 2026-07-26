package nightfallmods.lifesteal.manager;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Items;

public class TNTCartDamageHandler {

    private static final float BOW_CAP      = 0.65f;
    private static final float CROSSBOW_CAP = 0.80f;

    public static float applyCapIfNeeded(float damage, ServerPlayer trigger, ServerPlayer target) {
        double maxHealth = target.getAttributeValue(Attributes.MAX_HEALTH);
        var held = trigger.getMainHandItem();
        float cap = held.getItem() == Items.CROSSBOW
                ? (float)(maxHealth * CROSSBOW_CAP)
                : (float)(maxHealth * BOW_CAP);
        return Math.min(damage, cap);
    }
}
