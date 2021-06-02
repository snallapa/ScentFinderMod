package com.nallapareddy.scentfindermod;

import com.nallapareddy.scentfindermod.common.ScentFinder;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("scentfindermod")
public class ScentFinderMod
{
    private static final Logger LOGGER = LogManager.getLogger();

    public ScentFinderMod() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
    public static class RegistryEvents {
        @SubscribeEvent
        public static void registerItems(final RegistryEvent.Register<Item> itemRegistryEvent) {
            // register a new block here
            ScentFinder value = new ScentFinder(new Item.Properties().stacksTo(1).setNoRepair().tab(ItemGroup.TAB_TOOLS));
            itemRegistryEvent.getRegistry().register(value);
            DistExecutor.safeRunWhenOn(Dist.CLIENT, new Supplier<DistExecutor.SafeRunnable>() {
                @Override
                public DistExecutor.SafeRunnable get() {
                    return new DistExecutor.SafeRunnable() {
                        @Override
                        public void run() {
                            ItemModelsProperties.register(value, new ResourceLocation("angle"), new IItemPropertyGetter() {
                                private final Angle wobble = new Angle();
                                private final Angle wobbleRandom = new Angle();

                                @OnlyIn(Dist.CLIENT)
                                public float call(ItemStack item, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
                                    Entity entity = (Entity)(livingEntity != null ? livingEntity : item.getEntityRepresentation());
                                    if (entity == null) {
                                        return 0.0F;
                                    } else {
                                        if (world == null && entity.level instanceof ClientWorld) {
                                            world = (ClientWorld)entity.level;
                                        }

                                        BlockPos blockpos = CompassItem.isLodestoneCompass(item) ? this.getPlayerPosition(world, item.getOrCreateTag()) : this.getSpawnPosition(world);
                                        long i = world.getGameTime();
                                        if (blockpos != null && !(entity.position().distanceToSqr((double)blockpos.getX() + 0.5D, entity.position().y(), (double)blockpos.getZ() + 0.5D) < (double)1.0E-5F)) {
                                            boolean flag = livingEntity instanceof PlayerEntity && ((PlayerEntity)livingEntity).isLocalPlayer();
                                            double d1 = 0.0D;
                                            if (flag) {
                                                d1 = (double)livingEntity.yRot;
                                            } else if (entity instanceof ItemFrameEntity) {
                                                d1 = this.getFrameRotation((ItemFrameEntity)entity);
                                            } else if (entity instanceof ItemEntity) {
                                                d1 = (double)(180.0F - ((ItemEntity)entity).getSpin(0.5F) / ((float)Math.PI * 2F) * 360.0F);
                                            } else if (livingEntity != null) {
                                                d1 = (double)livingEntity.yBodyRot;
                                            }

                                            d1 = MathHelper.positiveModulo(d1 / 360.0D, 1.0D);
                                            double d2 = this.getAngleTo(Vector3d.atCenterOf(blockpos), entity) / (double)((float)Math.PI * 2F);
                                            double d3;
                                            if (flag) {
                                                if (this.wobble.shouldUpdate(i)) {
                                                    this.wobble.update(i, 0.5D - (d1 - 0.25D));
                                                }

                                                d3 = d2 + this.wobble.rotation;
                                            } else {
                                                d3 = 0.5D - (d1 - 0.25D - d2);
                                            }

                                            return MathHelper.positiveModulo((float)d3, 1.0F);
                                        } else {
                                            if (this.wobbleRandom.shouldUpdate(i)) {
                                                this.wobbleRandom.update(i, Math.random());
                                            }

                                            double d0 = this.wobbleRandom.rotation + (double)((float)item.hashCode() / 2.14748365E9F);
                                            return MathHelper.positiveModulo((float)d0, 1.0F);
                                        }
                                    }
                                }

                                @Nullable
                                private BlockPos getSpawnPosition(ClientWorld world) {
                                    return world.dimensionType().natural() ? world.getSharedSpawnPos() : null;
                                }

                                @Nullable
                                private BlockPos getPlayerPosition(World world, CompoundNBT nbt) {
                                    boolean flag = nbt.contains("PlayerPos");
                                    boolean flag1 = nbt.contains("PlayerDimension");
                                    if (flag && flag1) {
                                        Optional<RegistryKey<World>> optional = ScentFinder.getPlayerDimension(nbt);
                                        if (optional.isPresent() && world.dimension() == optional.get()) {
                                            return NBTUtil.readBlockPos(nbt.getCompound("PlayerPos"));
                                        }
                                    }

                                    return null;
                                }

                                private double getFrameRotation(ItemFrameEntity p_239441_1_) {
                                    Direction direction = p_239441_1_.getDirection();
                                    int i = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getStep() : 0;
                                    return (double)MathHelper.wrapDegrees(180 + direction.get2DDataValue() * 90 + p_239441_1_.getRotation() * 45 + i);
                                }

                                private double getAngleTo(Vector3d p_239443_1_, Entity p_239443_2_) {
                                    return Math.atan2(p_239443_1_.z() - p_239443_2_.getZ(), p_239443_1_.x() - p_239443_2_.getX());
                                }
                            });
                        }
                    };
                }
            });
        }
    }
    static class Angle {
        private double rotation;
        private double deltaRotation;
        private long lastUpdateTick;

        private Angle() {
        }

        private boolean shouldUpdate(long p_239448_1_) {
            return this.lastUpdateTick != p_239448_1_;
        }

        private void update(long p_239449_1_, double p_239449_3_) {
            this.lastUpdateTick = p_239449_1_;
            double d0 = p_239449_3_ - this.rotation;
            d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
            this.deltaRotation += d0 * 0.1D;
            this.deltaRotation *= 0.8D;
            this.rotation = MathHelper.positiveModulo(this.rotation + this.deltaRotation, 1.0D);
        }
    }
}
