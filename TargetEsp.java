package com.rainywater;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

/**
 * Cosmetic marker around the entity you attack / aim at.
 * Depth-tested like any normal object: NOT visible through walls.
 */
public final class TargetEsp {
    private static final String[] MODES = {"Выкл", "Кольцо", "Орбита", "Сканер"};
    private static int mode = 1;
    private static KeyBinding key;
    private static LivingEntity target;
    private static long lastHit;
    private static final int SHOW_TICKS = 40;

    private TargetEsp() {}

    public static void init() {
        key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.rainywater.target", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.rainywater"));

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            if (world.isClient() && entity instanceof LivingEntity le) {
                target = le;
                lastHit = world.getTime();
            }
            return ActionResult.PASS;
        });
        ClientTickEvents.END_CLIENT_TICK.register(TargetEsp::tick);
        WorldRenderEvents.AFTER_ENTITIES.register(TargetEsp::render);
    }

    private static void tick(MinecraftClient mc) {
        while (key.wasPressed()) {
            mode = (mode + 1) % MODES.length;
            if (mc.player != null) mc.player.sendMessage(Text.literal("TargetESP: " + MODES[mode]), true);
        }
        if (mc.world != null && mc.targetedEntity instanceof LivingEntity le && le != mc.player) {
            target = le;
            lastHit = mc.world.getTime();
        }
    }

    private static void render(WorldRenderContext ctx) {
        if (mode == 0 || target == null) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientWorld w = mc.world;
        if (w == null || ctx.consumers() == null || ctx.matrixStack() == null) return;
        if (!target.isAlive() || target.isRemoved()) { target = null; return; }

        float tp = ctx.tickCounter().getTickProgress(false);
        float age = (w.getTime() - lastHit) + tp;
        if (age > SHOW_TICKS) return;
        float fade = age < SHOW_TICKS - 10 ? 1f : 1f - (age - (SHOW_TICKS - 10)) / 10f;

        Vec3d cam = ctx.camera().getPos();
        Vec3d p = target.getLerpedPos(tp);
        float t = w.getTime() + tp;

        MatrixStack ms = ctx.matrixStack();
        ms.push();
        ms.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
        Matrix4f m = ms.peek().getPositionMatrix();
        VertexConsumer vc = ctx.consumers().getBuffer(RenderLayer.getLightning());

        // pink, flashes toward red when the target is hurt
        float hurt = MathHelper.clamp(target.hurtTime / 10f, 0f, 1f);
        float r = MathHelper.lerp(hurt, 0.85f, 1.0f);
        float g = MathHelper.lerp(hurt, 0.20f, 0.25f);
        float b = MathHelper.lerp(hurt, 1.00f, 0.35f);
        float rad = target.getWidth() * 0.75f + 0.25f;
        float h = target.getHeight();

        switch (mode) {
            case 1 -> { // ground ring with glow
                float pulse = 1f + 0.06f * MathHelper.sin(t * 0.3f);
                float R = rad * pulse;
                float y = 0.03f;
                band(vc, m, y, R - 0.05f, R + 0.05f, 0, 6.2832f, 56, r, g, b, 0.9f * fade, 0.9f * fade);
                band(vc, m, y, R + 0.05f, R + 0.35f, 0, 6.2832f, 56, r, g, b, 0.5f * fade, 0f);
                band(vc, m, y, R - 0.35f, R - 0.05f, 0, 6.2832f, 56, r, g, b, 0f, 0.5f * fade);
            }
            case 2 -> { // three arcs orbiting the body
                for (int i = 0; i < 3; i++) {
                    float start = t * 0.12f + i * 2.0944f;
                    float y = h * (0.5f + 0.4f * MathHelper.sin(t * 0.08f + i * 2.1f));
                    float R = rad + 0.1f;
                    band(vc, m, y, R - 0.04f, R + 0.04f, start, start + 1.2f, 20, r, g, b, 0.95f * fade, 0.95f * fade);
                    band(vc, m, y, R + 0.04f, R + 0.22f, start, start + 1.2f, 20, r, g, b, 0.45f * fade, 0f);
                    band(vc, m, y, R - 0.22f, R - 0.04f, start, start + 1.2f, 20, r, g, b, 0f, 0.45f * fade);
                }
            }
            case 3 -> { // ring sweeping up and down the body + faint ground ring
                float y = h * (0.5f + 0.5f * MathHelper.sin(t * 0.1f));
                float R = rad + 0.05f;
                band(vc, m, y, R - 0.04f, R + 0.04f, 0, 6.2832f, 48, r, g, b, 0.95f * fade, 0.95f * fade);
                band(vc, m, y, R + 0.04f, R + 0.25f, 0, 6.2832f, 48, r, g, b, 0.4f * fade, 0f);
                band(vc, m, 0.03f, rad - 0.04f, rad + 0.04f, 0, 6.2832f, 48, r, g, b, 0.35f * fade, 0.35f * fade);
            }
            default -> {}
        }
        ms.pop();
    }

    /** Flat ring band between radii r0..r1 and angles a0..a1, double sided. */
    private static void band(VertexConsumer vc, Matrix4f m, float y, float r0, float r1,
                             float a0, float a1, int seg,
                             float R, float G, float B, float al0, float al1) {
        for (int i = 0; i < seg; i++) {
            float s = a0 + (a1 - a0) * i / seg;
            float e = a0 + (a1 - a0) * (i + 1) / seg;
            float c1 = MathHelper.cos(s), s1 = MathHelper.sin(s);
            float c2 = MathHelper.cos(e), s2 = MathHelper.sin(e);
            // front
            v(vc, m, c1 * r0, y, s1 * r0, R, G, B, al0);
            v(vc, m, c1 * r1, y, s1 * r1, R, G, B, al1);
            v(vc, m, c2 * r1, y, s2 * r1, R, G, B, al1);
            v(vc, m, c2 * r0, y, s2 * r0, R, G, B, al0);
            // back
            v(vc, m, c2 * r0, y, s2 * r0, R, G, B, al0);
            v(vc, m, c2 * r1, y, s2 * r1, R, G, B, al1);
            v(vc, m, c1 * r1, y, s1 * r1, R, G, B, al1);
            v(vc, m, c1 * r0, y, s1 * r0, R, G, B, al0);
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, float x, float y, float z,
                          float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a);
    }
}
