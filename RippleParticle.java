package com.rainywater;

import net.minecraft.client.particle.*;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/** Flat expanding ring lying on the water surface. */
public class RippleParticle extends SpriteBillboardParticle {
    private final float maxScale;

    protected RippleParticle(ClientWorld world, double x, double y, double z, SpriteProvider sprites) {
        super(world, x, y, z);
        this.setSprite(sprites);
        this.maxAge = 22 + this.random.nextInt(10);
        this.maxScale = 0.35f + this.random.nextFloat() * 0.35f;
        this.scale = 0.02f;
        this.red = 0.85f;
        this.green = 0.92f;
        this.blue = 1.0f;
        this.alpha = 0.0f;
        this.collidesWithWorld = false;
        this.velocityX = this.velocityY = this.velocityZ = 0;
    }

    @Override
    public void tick() {
        this.prevPosX = this.x;
        this.prevPosY = this.y;
        this.prevPosZ = this.z;
        if (this.age++ >= this.maxAge) {
            this.markDead();
            return;
        }
        float t = (float) this.age / this.maxAge;
        this.scale = MathHelper.lerp(1.0f - (1.0f - t) * (1.0f - t), 0.02f, maxScale);
        this.alpha = 0.55f * (1.0f - t) * Math.min(1.0f, t * 8.0f);
    }

    @Override
    public void buildGeometry(VertexConsumer vc, Camera camera, float tickDelta) {
        Vec3d cam = camera.getPos();
        float px = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - cam.getX());
        float py = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - cam.getY());
        float pz = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - cam.getZ());
        float s = this.scale;
        int light = this.getBrightness(tickDelta);
        float u0 = getMinU(), u1 = getMaxU(), v0 = getMinV(), v1 = getMaxV();

        vc.vertex(px - s, py, pz - s).texture(u0, v0).color(red, green, blue, alpha).light(light);
        vc.vertex(px - s, py, pz + s).texture(u0, v1).color(red, green, blue, alpha).light(light);
        vc.vertex(px + s, py, pz + s).texture(u1, v1).color(red, green, blue, alpha).light(light);
        vc.vertex(px + s, py, pz - s).texture(u1, v0).color(red, green, blue, alpha).light(light);
    }

    @Override
    public ParticleTextureSheet getType() {
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Factory implements ParticleFactory<SimpleParticleType> {
        private final SpriteProvider sprites;

        public Factory(SpriteProvider sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientWorld world,
                                       double x, double y, double z,
                                       double vx, double vy, double vz) {
            return new RippleParticle(world, x, y, z, sprites);
        }
    }
}
