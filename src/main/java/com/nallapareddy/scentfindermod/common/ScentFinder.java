package com.nallapareddy.scentfindermod.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ScentFinder extends Item {
    private static final Logger LOGGER = LogManager.getLogger();

    public ScentFinder(Properties properties) {
        super(properties);
        setRegistryName("scent_finder");
    }

    @Override
    public String getDescriptionId(ItemStack stack) {
        return is_tracking(stack) ? "item.scentfindermod.scent_finder_tracking" : "item.scentfindermod.scent_finder";
    }

    public static boolean is_tracking(ItemStack item) {
        CompoundNBT compoundnbt = item.getTag();
        return compoundnbt != null && (compoundnbt.contains("PlayerDimension") || compoundnbt.contains("PlayerPos"));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return is_tracking(stack) || super.isFoil(stack);
    }


    public static Optional<RegistryKey<World>> getPlayerDimension(CompoundNBT tag) {
        return World.RESOURCE_KEY_CODEC.parse(NBTDynamicOps.INSTANCE, tag.get("PlayerDimension")).result();
    }

    /**
     * Called each tick as long the item is on a player inventory. Uses by maps to check if is on a player hand and
     * update it's contents.
     */
    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        if (worldIn.isClientSide) {
            if (is_tracking(stack)) {
                CompoundNBT compoundnbt = stack.getOrCreateTag();
                if (compoundnbt.contains("PlayerTracked") && !compoundnbt.getBoolean("PlayerTracked")) {
                    return;
                }
                Optional<RegistryKey<World>> optional = getPlayerDimension(compoundnbt);
                if (optional.isPresent() && optional.get() == worldIn.dimension() && compoundnbt.contains("PlayerName")) {
                    String trackedPlayerName = compoundnbt.getString("PlayerName");
                    Optional<? extends PlayerEntity> tPlayerOptional = worldIn.players().stream().filter(p -> p.getName().getString().equals(trackedPlayerName)).findFirst();
                    if (tPlayerOptional.isPresent()) {
                        PlayerEntity trackedPlayer = tPlayerOptional.get();
                        Vector3d currentPosition = trackedPlayer.position();
                        compoundnbt.put("PlayerPos", writePos((int) currentPosition.x, (int) currentPosition.y, (int) currentPosition.z));
                    }
                }
            }
        }
    }


    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity playerIn, Hand hand) {
        List<? extends PlayerEntity> players = new ArrayList<>(world.players());
        if (players.size() > 0) {
            ItemStack heldItem = playerIn.getItemInHand(hand);
            CompoundNBT tag = heldItem.getOrCreateTag();
            PlayerEntity curr = null;
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
                if (curr == null) {
                    LOGGER.info("Player {} was not found, resetting compass", prevTracked);
                    curr = players.get(0);
                }
            }
            try {
                final String trackedPlayerName = curr.getName().getString();
                playerIn.displayClientMessage(new StringTextComponent(String.format("Tracking %s", trackedPlayerName)), true);
                this.track(world.dimension(), curr.position().x, curr.position().y, curr.position().z, curr.getName().toString(), tag);
                heldItem.setTag(tag);
            } catch (Exception e) {
                LOGGER.error("Compass could not track was trying to track {}!!!", curr);
                throw e;
            }
            return ActionResult.success(heldItem);
        } else {
            return super.use(world, playerIn, hand);
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
        World.RESOURCE_KEY_CODEC.encodeStart(NBTDynamicOps.INSTANCE, registryKey).resultOrPartial(LOGGER::error).ifPresent((p) -> {
            cNBT.put("PlayerDimension", p);
        });
        cNBT.putBoolean("PlayerTracked", true);
        cNBT.putString("PlayerName", playerName);
    }
}
