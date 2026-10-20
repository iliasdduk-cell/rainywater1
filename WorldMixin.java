package com.rainywater.mixin;

import com.rainywater.RainyWaterClient;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes the CLIENT think it is raining, whatever the server says.
 * Vanilla then renders rain, rain sounds, splashes, darker sky and fog by itself.
 */
@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(method = "getRainGradient", at = @At("RETURN"), cancellable = true)
    private void rainywater$rain(float delta, CallbackInfoReturnable<Float> cir) {
        if (((World) (Object) this).isClient()) {
            float fake = RainyWaterClient.fakeRain();
            if (fake > cir.getReturnValueF()) cir.setReturnValue(fake);
        }
    }

    @Inject(method = "getThunderGradient", at = @At("RETURN"), cancellable = true)
    private void rainywater$thunder(float delta, CallbackInfoReturnable<Float> cir) {
        if (((World) (Object) this).isClient()) {
            float fake = RainyWaterClient.fakeThunder();
            if (fake > cir.getReturnValueF()) cir.setReturnValue(fake);
        }
    }
}
