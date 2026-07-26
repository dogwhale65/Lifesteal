package nightfallmods.lifesteal.mixin;

import nightfallmods.lifesteal.config.ServerConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class EffectBlockMixin {

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true)
    private void lifesteal$blockEffects(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if (!((Object) this instanceof ServerPlayer)) return;

        // MobEffect is a plain registry object before 1.20.5, so the id comes from the registry.
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect());
        if (id == null) return;

        ServerConfig cfg = ServerConfig.getInstance();
        if (cfg.isEffectBlocked(id.toString(), effect.getAmplifier())) {
            cir.setReturnValue(false);
        }
    }
}

