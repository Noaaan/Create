package com.simibubi.create.foundation.gui;

import com.simibubi.create.lib.transfer.item.ItemHandlerHelper;
import com.simibubi.create.lib.transfer.item.ItemStackHandler;

public abstract class GhostItemContainer<T> extends ContainerBase<T> implements IClearableContainer {

	public ItemStackHandler ghostInventory;

	protected GhostItemContainer(MenuType<?> type, int id, Inventory inv, FriendlyByteBuf extraData) {
		super(type, id, inv, extraData);
	}

	protected GhostItemContainer(MenuType<?> type, int id, Inventory inv, T contentHolder) {
		super(type, id, inv, contentHolder);
	}

	protected abstract ItemStackHandler createGhostInventory();

	protected abstract boolean allowRepeats();

	@Override
	protected void initAndReadInventory(T contentHolder) {
		ghostInventory = createGhostInventory();
	}

	@Override
	public void clearContents() {
		for (int i = 0; i < ghostInventory.getSlots(); i++)
			ghostInventory.setStackInSlot(i, ItemStack.EMPTY);
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slotIn) {
		return slotIn.container == playerInventory;
	}

	@Override
	public boolean canDragTo(Slot slotIn) {
		if (allowRepeats())
			return true;
		return slotIn.container == playerInventory;
	}

	@Override
	public void clicked(int slotId, int dragType, ClickType clickTypeIn, Player player) {
		ItemStack held = playerInventory.getSelected();
		if (slotId < 36)
			super.clicked(slotId, dragType, clickTypeIn, player);
		else if (clickTypeIn == ClickType.THROW)
			return;

		int slot = slotId - 36;
		if (clickTypeIn == ClickType.CLONE) {
			if (player.isCreative() && held.isEmpty()) {
				ItemStack stackInSlot = ghostInventory.getStackInSlot(slot)
						.copy();
				stackInSlot.setCount(stackInSlot.getMaxStackSize());
				playerInventory.setPickedItem(stackInSlot);
				return;
			}
			return;
		}

		if (held.isEmpty()) {
			ghostInventory.setStackInSlot(slot, ItemStack.EMPTY);
			getSlot(slotId).setChanged();
			return;
		}

		ItemStack insert = held.copy();
		insert.setCount(1);
		ghostInventory.setStackInSlot(slot, insert);
		getSlot(slotId).setChanged();
	}

	@Override
	public ItemStack quickMoveStack(Player playerIn, int index) {
		if (index < 36) {
			ItemStack stackToInsert = playerInventory.getItem(index);
			for (int i = 0; i < ghostInventory.getSlots(); i++) {
				ItemStack stack = ghostInventory.getStackInSlot(i);
				if (!allowRepeats() && ItemHandlerHelper.canItemStacksStack(stack, stackToInsert))
					break;
				if (stack.isEmpty()) {
					ItemStack copy = stackToInsert.copy();
					copy.setCount(1);
					ghostInventory.insertItem(i, copy, false);
					getSlot(i + 36).setChanged();
					break;
				}
			}
		} else {
			ghostInventory.extractItem(index - 36, 1, false);
			getSlot(index).setChanged();
		}
		return ItemStack.EMPTY;
	}



}
