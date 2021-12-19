package com.simibubi.create.lib.mixin.common;

import com.jamieswhiteshirt.reachentityattributes.ReachEntityAttributes;
import com.simibubi.create.AllItems;
import com.simibubi.create.lib.event.PlayerTickEndCallback;
import com.simibubi.create.lib.helper.EntityHelper;
import com.simibubi.create.lib.transfer.TransferUtil;
import com.simibubi.create.lib.transfer.fluid.FluidStack;

import com.simibubi.create.lib.transfer.fluid.IFluidHandler;

import com.simibubi.create.lib.utility.FluidHandlerData;
import com.simibubi.create.lib.utility.LazyOptional;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.lib.event.ServerPlayerCreationCallback;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player {

	public ServerPlayerMixin(Level level, BlockPos blockPos, float f, GameProfile gameProfile) {
		super(level, blockPos, f, gameProfile);
	}

	@Shadow
	public abstract Level getLevel();

	@Unique
	private IFluidHandler create$lastViewedHandler = null;

	@Inject(at = @At("RETURN"), method = "<init>")
	private void init(MinecraftServer minecraftServer, ServerLevel serverLevel, GameProfile gameProfile, CallbackInfo ci) {
		ServerPlayerCreationCallback.EVENT.invoker().onCreate((ServerPlayer) (Object) this);
	}

	@Inject(at = @At("TAIL"), method = "tick()V")
	public void create$clientEndOfTickEvent(CallbackInfo ci) {
		if (!getLevel().isClientSide()) {
			if (getItemBySlot(EquipmentSlot.HEAD).is(AllItems.GOGGLES.get())) {
				double reach = ReachEntityAttributes.getReachDistance(this, 4);
				HitResult hit = pick(reach, 0, false);
				if (hit instanceof BlockHitResult blockHit) {
					LazyOptional<IFluidHandler> optional = TransferUtil.getFluidHandler(level, blockHit.getBlockPos(), blockHit.getDirection());
					if (optional.isPresent()) {
						IFluidHandler handler = optional.orElse(null);
						if (!checkSameAndUpdate(handler)) {
							FluidHandlerData.sendToClient((ServerPlayer) (Object) this, handler);
						}
					}
				}
			}
		}
		PlayerTickEndCallback.EVENT.invoker().onEndOfPlayerTick((ServerPlayer) (Object) this);
	}

	/**
	 * @return true if handlers are equivalent
	 */
	private boolean checkSameAndUpdate(IFluidHandler handler) {
		if (create$lastViewedHandler == null) return handler == null;

		if (create$lastViewedHandler.getTanks() != handler.getTanks()) {
			create$lastViewedHandler = handler;
			return false;
		}

		for (int i = 0; i < create$lastViewedHandler.getTanks(); i++) {
			FluidStack last = create$lastViewedHandler.getFluidInTank(i);
			FluidStack current = handler.getFluidInTank(i);
			if (!last.isFluidEqual(current)) {
				create$lastViewedHandler = handler;
				return false;
			}
		}

		return true;
	}
}
