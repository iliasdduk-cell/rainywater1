package com.rainywater;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.fabricmc.fabric.api.client.render.fluid.v1.SimpleFluidRenderHandler;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.fluid.Fluids;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import org.lwjgl.glfw.GLFW;

public class RainyWaterClient implements ClientModInitializer {
    public static final SimpleParticleType RIPPLE = FabricParticleTypes.simple();

    // Dark, slightly murky water tint (RGB)
    private static final int WATER_TINT = 0x2C3C42;

    // 0 = off, 1 = light, 2 = rain, 3 = storm
    private static final String[] NAMES = {"Выкл", "Лёгкий дождь", "Дождь", "Гроза"};
    private static final float[] RAIN = {0f, 0.55f, 1.0f, 1.0f};
    private static final float[] THUNDER = {0f, 0f, 0f, 0.8f};
    private static int level = 2;           // default: rain
    private static float rainNow = 0f;      // smoothed values
    private static float thunderNow = 0f;

    private static KeyBinding cycleKey;

    public static float fakeRain() { return rainNow; }
    public static float fakeThunder() { return thunderNow; }

    @Override
    public void onInitializeClient() {
        TargetEsp.init();
        Registry.register(Registries.PARTICLE_TYPE, Identifier.of("rainywater", "ripple"), RIPPLE);
        ParticleFactoryRegistry.getInstance().register(RIPPLE, RippleParticle.Factory::new);

        FluidRenderHandlerRegistry.INSTANCE.register(
                Fluids.WATER, Fluids.FLOWING_WATER, SimpleFluidRenderHandler.coloredWater(WATER_TINT));

        cycleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rainywater.cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_V, "key.categories.rainywater"));

        ClientTickEvents.END_CLIENT_TICK.register(RainyWaterClient::clientTick);
        ClientTickEvents.END_WORLD_TICK.register(RainyWaterClient::spawnRipples);
    }

    private static void clientTick(MinecraftClient mc) {
        while (cycleKey.wasPressed()) {
            level = (level + 1) % NAMES.length;
            if (mc.player != null) {
                mc.player.sendMessage(Text.literal("Rainy Water: " + NAMES[level]), true);
            }
        }
        // smooth fade in / out (~3-4 seconds)
        rainNow += (RAIN[level] - rainNow) * 0.03f;
        thunderNow += (THUNDER[level] - thunderNow) * 0.03f;
        if (Math.abs(RAIN[level] - rainNow) < 0.002f) rainNow = RAIN[level];
        if (Math.abs(THUNDER[level] - thunderNow) < 0.002f) thunderNow = THUNDER[level];

        // distant thunder in storm mode
        ClientWorld world = mc.world;
        if (world != null && mc.player != null && level == 3 && world.random.nextInt(450) == 0) {
            double ang = world.random.nextDouble() * Math.PI * 2;
            double dist = 40 + world.random.nextDouble() * 60;
            world.playSound(mc.player.getX() + Math.cos(ang) * dist, mc.player.getY() + 20,
                    mc.player.getZ() + Math.sin(ang) * dist,
                    SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER,
                    4.0f, 0.6f + world.random.nextFloat() * 0.4f, false);
        }
    }

    private static void spawnRipples(ClientWorld world) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || !world.isRaining()) return;

        Random rnd = world.random;
        float strength = world.getRainGradient(1.0f);
        int attempts = (int) (40 * strength);
        BlockPos center = mc.player.getBlockPos();

        for (int i = 0; i < attempts; i++) {
            int x = center.getX() + rnd.nextInt(33) - 16;
            int z = center.getZ() + rnd.nextInt(33) - 16;
            int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING, x, z);
            BlockPos waterPos = new BlockPos(x, top - 1, z);
            var fluid = world.getFluidState(waterPos);
            if (!fluid.isIn(FluidTags.WATER) || !fluid.isStill()) continue;
            double y = waterPos.getY() + fluid.getHeight(world, waterPos) + 0.01;
            world.addParticle(RIPPLE, x + rnd.nextDouble(), y, z + rnd.nextDouble(), 0, 0, 0);
        }
    }
}
