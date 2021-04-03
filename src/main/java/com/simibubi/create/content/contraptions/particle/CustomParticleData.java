package com.simibubi.create.content.contraptions.particle;

import com.mojang.serialization.Codec;

import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleEffect.Factory;
import net.minecraft.particle.ParticleType;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import com.simibubi.create.lib.extensions.helper.ParticleManagerHelper;

public interface CustomParticleData<T extends ParticleEffect> {
	Factory<T> getDeserializer();

	Codec<T> getCodec(ParticleType<T> type);

	default ParticleType<T> createType() {
		return new ParticleType<T>(false, getDeserializer()) {

			@Override
			public Codec<T> getCodec() {
				return CustomParticleData.this.getCodec(this);
			}
		};
	}

	@Environment(EnvType.CLIENT)
	ParticleFactory<T> getFactory();

	@Environment(EnvType.CLIENT)
	default void register(ParticleType<T> type, ParticleManager particles) {
		ParticleManagerHelper.registerFactory(particles, type, getFactory());
	}
}