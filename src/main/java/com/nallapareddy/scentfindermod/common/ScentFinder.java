package com.nallapareddy.scentfindermod.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class ScentFinder extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    public ScentFinder(Properties properties) {
        super(properties);
        setRegistryName("scent_finder");
    }

    @Override
    public String getTranslationKey(ItemStack stack) {
        return is_tracking(stack) ? "item.scentfindermod.scent_finder_tracking" : "item.scentfindermod.scent_finder";
    }

    public static boolean is_tracking(ItemStack item) {
        CompoundNBT compoundnbt = item.getTag();
        return compoundnbt != null && (compoundnbt.contains("PlayerDimension") || compoundnbt.contains("PlayerPos"));
    }

    /**
     * Returns true if this item has an enchantment glint. By default, this returns <code>stack.isItemEnchanted()</code>,
     * but other items can override it (for instance, written books always return true).
     *
     * Note that if you override this method, you generally want to also call the super version (on {@link Item}) to get
     * the glint for enchanted items. Of course, that is unnecessary if the overwritten version always returns true.
     */
    @Override
    public boolean hasEffect(ItemStack stack) {
        return is_tracking(stack) || super.hasEffect(stack);
    }

    public static Optional<RegistryKey<World>> getPlayerDimension(CompoundNBT tag) {
        return World.field_234917_f_.parse(NBTDynamicOps.INSTANCE, tag.get("PlayerDimension")).result();
    }

    /**
     * Called each tick as long the item is on a player inventory. Uses by maps to check if is on a player hand and
     * update it's contents.
     */
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (!worldIn.isRemote) {
            if (is_tracking(stack)) {
                CompoundNBT compoundnbt = stack.getOrCreateTag();
                if (compoundnbt.contains("PlayerTracked") && !compoundnbt.getBoolean("PlayerTracked")) {
                    return;
                }
                Optional<RegistryKey<World>> optional = getPlayerDimension(compoundnbt);
                if (optional.isPresent() && optional.get() == worldIn.func_234923_W_() && compoundnbt.contains("PlayerName")) {
                    String trackedPlayerName = compoundnbt.getString("PlayerName");
                    Optional<? extends PlayerEntity> tPlayerOptional = worldIn.getPlayers().stream().filter(p -> p.getName().getString().equals(trackedPlayerName)).findFirst();
                    if (tPlayerOptional.isPresent()) {
                        PlayerEntity trackedPlayer = tPlayerOptional.get();
                        compoundnbt.put("PlayerPos", writePos((int) trackedPlayer.lastTickPosX, (int) trackedPlayer.lastTickPosY, (int) trackedPlayer.lastTickPosZ));
                    }
                }
            }
        }
    }


    @Override
    public ActionResult<ItemStack> onItemRightClick(World worldIn, PlayerEntity playerIn, Hand handIn) {
        List<? extends PlayerEntity> players = new ArrayList<>(worldIn.getPlayers());
        if (players.size() > 0) {
            ItemStack heldItem = playerIn.getHeldItem(handIn);
            CompoundNBT tag;
            PlayerEntity curr = null;
            boolean flag = !playerIn.abilities.isCreativeMode && heldItem.getCount() == 1;
            if (flag) {
                tag = heldItem.getOrCreateTag();
            } else {
                ItemStack itemstack = new ItemStack(this, 1);
                tag = heldItem.hasTag() ? heldItem.getTag().copy() : new CompoundNBT();
                itemstack.setTag(tag);
                if (!playerIn.abilities.isCreativeMode) {
                    heldItem.shrink(1);
                }
                if (!playerIn.inventory.addItemStackToInventory(itemstack)) {
                    playerIn.dropItem(itemstack, false);
                }
            }
            players.sort(Comparator.comparing(o -> o.getName().getString()));
            if (tag.getString("PlayerName").isEmpty()) {
                curr = players.get(0);
            } else {
                String prevTracked = tag.getString("PlayerName");
                for (int i = 0; i < players.size(); i++) {
                    PlayerEntity c = players.get(i);
                    if (c.getName().getString().equals(prevTracked)) {
                        if (i == players.size() - 1) {
                            curr = players.get(0);
                        } else {
                            curr = players.get(i + 1);
                        }
                    }
                }
            }
            final String trackedPlayerName = curr.getName().getString();
            playerIn.sendStatusMessage(new StringTextComponent(String.format("Tracking %s", trackedPlayerName)), true);
            this.track(worldIn.func_234923_W_(), curr.lastTickPosX, curr.lastTickPosY, curr.lastTickPosZ, curr.getName().getString(), tag);
            heldItem.setTag(tag);
            return ActionResult.func_233538_a_(heldItem, worldIn.isRemote);
        } else {
            return super.onItemRightClick(worldIn, playerIn, handIn);
        }
    }

    public static CompoundNBT writePos(int x, int y, int z) {
        CompoundNBT compoundnbt = new CompoundNBT();
        compoundnbt.putInt("X", x);
        compoundnbt.putInt("Y", y);
        compoundnbt.putInt("Z", z);
        return compoundnbt;
    }

    private void track(RegistryKey<World> registryKey, double x, double y, double z, String playerName,  CompoundNBT cNBT) {
        cNBT.put("PlayerPos", writePos((int) x, (int) y, (int) z));
        World.field_234917_f_.encodeStart(NBTDynamicOps.INSTANCE, registryKey).resultOrPartial(LOGGER::error).ifPresent((p) -> {
            cNBT.put("PlayerDimension", p);
        });
        cNBT.putBoolean("PlayerTracked", true);
        cNBT.putString("PlayerName", playerName);
    }
}
