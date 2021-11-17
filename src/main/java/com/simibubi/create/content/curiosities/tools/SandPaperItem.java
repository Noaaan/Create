package com.simibubi.create.content.curiosities.tools;

import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.item.render.SimpleCustomRenderer;
import com.simibubi.create.foundation.utility.VecHelper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.simibubi.create.lib.item.CustomItemEnchantabilityItem;
import com.simibubi.create.lib.utility.NBTSerializer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.simibubi.create.lib.entity.FakePlayer;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class SandPaperItem extends Item implements CustomItemEnchantabilityItem {

	public SandPaperItem(Properties properties) {
		super(properties.durability(8));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
		ItemStack itemstack = playerIn.getItemInHand(handIn);
		InteractionResultHolder<ItemStack> FAIL = new InteractionResultHolder<>(InteractionResult.FAIL, itemstack);

		if (itemstack.getOrCreateTag()
			.contains("Polishing")) {
			playerIn.startUsingItem(handIn);
			return new InteractionResultHolder<>(InteractionResult.PASS, itemstack);
		}

		InteractionHand otherHand = handIn == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
		ItemStack itemInOtherHand = playerIn.getItemInHand(otherHand);
		if (SandPaperPolishingRecipe.canPolish(worldIn, itemInOtherHand)) {
			ItemStack item = itemInOtherHand.copy();
			ItemStack toPolish = item.split(1);
			playerIn.startUsingItem(handIn);
			itemstack.getOrCreateTag()
				.put("Polishing", NBTSerializer.serializeNBT(toPolish));
			playerIn.setItemInHand(otherHand, item);
			return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemstack);
		}

		HitResult raytraceresult = getPlayerPOVHitResult(worldIn, playerIn, ClipContext.Fluid.NONE);
		if (!(raytraceresult instanceof BlockHitResult))
			return FAIL;
		BlockHitResult ray = (BlockHitResult) raytraceresult;
		Vec3 hitVec = ray.getLocation();

		AABB bb = new AABB(hitVec, hitVec).inflate(1f);
		ItemEntity pickUp = null;
		for (ItemEntity itemEntity : worldIn.getEntitiesOfClass(ItemEntity.class, bb)) {
			if (!itemEntity.isAlive())
				continue;
			if (itemEntity.position()
				.distanceTo(playerIn.position()) > 3)
				continue;
			ItemStack stack = itemEntity.getItem();
			if (!SandPaperPolishingRecipe.canPolish(worldIn, stack))
				continue;
			pickUp = itemEntity;
			break;
		}

		if (pickUp == null)
			return FAIL;

		ItemStack item = pickUp.getItem()
			.copy();
		ItemStack toPolish = item.split(1);

		playerIn.startUsingItem(handIn);

		if (!worldIn.isClientSide) {
			itemstack.getOrCreateTag()
				.put("Polishing", NBTSerializer.serializeNBT(toPolish));
			if (item.isEmpty())
				pickUp.discard();
			else
				pickUp.setItem(item);
		}

		return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemstack);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level worldIn, LivingEntity entityLiving) {
		if (!(entityLiving instanceof Player))
			return stack;
		Player player = (Player) entityLiving;
		CompoundTag tag = stack.getOrCreateTag();
		if (tag.contains("Polishing")) {
			ItemStack toPolish = ItemStack.of(tag.getCompound("Polishing"));
			ItemStack polished =
				SandPaperPolishingRecipe.applyPolish(worldIn, entityLiving.position(), toPolish, stack);

			if (worldIn.isClientSide) {
				spawnParticles(entityLiving.getEyePosition(1)
					.add(entityLiving.getLookAngle()
						.scale(.5f)),
					toPolish, worldIn);
				return stack;
			}

			if (!polished.isEmpty()) {
				if (player instanceof FakePlayer) {
					player.drop(polished, false, false);
				} else {
					player.getInventory().placeItemBackInInventory(polished);
				}
			}
			tag.remove("Polishing");
			stack.hurtAndBreak(1, entityLiving, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
		}

		return stack;
	}

	public static void spawnParticles(Vec3 location, ItemStack polishedStack, Level world) {
		for (int i = 0; i < 20; i++) {
			Vec3 motion = VecHelper.offsetRandomly(Vec3.ZERO, world.random, 1 / 8f);
			world.addParticle(new ItemParticleOption(ParticleTypes.ITEM, polishedStack), location.x, location.y,
				location.z, motion.x, motion.y, motion.z);
		}
	}

	@Override
	public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
		if (!(entityLiving instanceof Player))
			return;
		Player player = (Player) entityLiving;
		CompoundTag tag = stack.getOrCreateTag();
		if (tag.contains("Polishing")) {
			ItemStack toPolish = ItemStack.of(tag.getCompound("Polishing"));
			player.getInventory().placeItemBackInInventory(toPolish);
			tag.remove("Polishing");
		}
	}

	//todo: port
	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);

//		BlockState newState = state.getToolModifiedState(level, pos, player, stack, ToolActions.AXE_SCRAPE);
//		if (newState != null) {
//			AllSoundEvents.SANDING_LONG.play(level, player, pos);
//			level.levelEvent(player, 3005, pos, 0); // Spawn particles
//		} else {
//			newState = state.getToolModifiedState(level, pos, player, stack, ToolActions.AXE_WAX_OFF);
//			if (newState != null) {
//				AllSoundEvents.SANDING_LONG.play(level, player, pos);
//				level.levelEvent(player, 3004, pos, 0); // Spawn particles
//			}
//		}
//
//		if (newState != null) {
//			level.setBlockAndUpdate(pos, newState);
//			if (player != null)
//				stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(p.getUsedItemHand()));
//			return InteractionResult.sidedSuccess(level.isClientSide);
//		}

		return InteractionResult.PASS;
	}

	//@Override
	public boolean canPerformAction(ItemStack stack/*, ToolAction toolAction*/) {
		return stack.getItem() instanceof AxeItem;
		//return toolAction == ToolActions.AXE_SCRAPE || toolAction == ToolActions.AXE_WAX_OFF;
	}

	@Override
	public SoundEvent getEatingSound() {
		return AllSoundEvents.SANDING_SHORT.getMainEvent();
	}

	@Override
	public UseAnim getUseAnimation(ItemStack stack) {
		return UseAnim.EAT;
	}

	@Override
	public int getUseDuration(ItemStack stack) {
		return 32;
	}

	@Override
	public int getItemEnchantability(ItemStack stack) {
		return 1;
	}

	@Override
	public int getEnchantmentValue() {
		return 5;
	}

//	@Override
//	@Environment(EnvType.CLIENT)
//	public void initializeClient(Consumer<IItemRenderProperties> consumer) {
//		consumer.accept(SimpleCustomRenderer.create(this, new SandPaperItemRenderer()));
//	}

}
