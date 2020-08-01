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
            ScentFinder value = new ScentFinder(new Item.Properties().maxStackSize(16).setNoRepair().group(ItemGroup.TOOLS));
            itemRegistryEvent.getRegistry().register(value);
            DistExecutor.safeRunWhenOn(Dist.CLIENT, new Supplier<DistExecutor.SafeRunnable>() {
                @Override
                public DistExecutor.SafeRunnable get() {
                    return new DistExecutor.SafeRunnable() {
                        @Override
                        public void run() {
                            ItemModelsProperties.func_239418_a_(value, new ResourceLocation("angle"), new IItemPropertyGetter() {

                                private final Angle angle1 = new Angle();
                                private final Angle angle2 = new Angle();
                                @OnlyIn(Dist.CLIENT)
                                public float call(ItemStack item, @Nullable ClientWorld world, @Nullable LivingEntity livingEntity) {
                                    Entity entity = (Entity)(livingEntity != null ? livingEntity : item.func_234694_A_());
                                    if (entity == null) {
                                        return 0.0F;
                                    } else {
                                        if (world == null && entity.world instanceof ClientWorld) {
                                            world = (ClientWorld)entity.world;
                                        }
                                        BlockPos blockpos = ScentFinder.is_tracking(item) ? this.func_239442_a_(world, item.getOrCreateTag()) : this.func_239444_a_(world);
                                        long i = world.getGameTime();
                                        if (blockpos != null && !(entity.getPositionVec().squareDistanceTo((double)blockpos.getX() + 0.5D, entity.getPositionVec().getY(), (double)blockpos.getZ() + 0.5D) < (double)1.0E-5F)) {
                                            boolean flag = livingEntity instanceof PlayerEntity && ((PlayerEntity)livingEntity).isUser();
                                            double d1 = 0.0D;
                                            if (flag) {
                                                d1 = (double)livingEntity.rotationYaw;
                                            } else if (entity instanceof ItemFrameEntity) {
                                                d1 = this.func_239441_a_((ItemFrameEntity)entity);
                                            } else if (entity instanceof ItemEntity) {
                                                d1 = (double)(180.0F - ((ItemEntity)entity).func_234272_a_(0.5F) / ((float)Math.PI * 2F) * 360.0F);
                                            } else if (livingEntity != null) {
                                                d1 = (double)livingEntity.renderYawOffset;
                                            }

                                            d1 = MathHelper.positiveModulo(d1 / 360.0D, 1.0D);
                                            double d2 = this.func_239443_a_(Vector3d.func_237489_a_(blockpos), entity) / (double)((float)Math.PI * 2F);
                                            double d3;
                                            if (flag) {
                                                if (this.angle1.func_239448_a_(i)) {
                                                    this.angle1.func_239449_a_(i, 0.5D - (d1 - 0.25D));
                                                }

                                                d3 = d2 + this.angle1.field_239445_a_;
                                            } else {
                                                d3 = 0.5D - (d1 - 0.25D - d2);
                                            }

                                            return MathHelper.positiveModulo((float)d3, 1.0F);
                                        } else {
                                            if (this.angle2.func_239448_a_(i)) {
                                                this.angle2.func_239449_a_(i, Math.random());
                                            }

                                            double d0 = this.angle2.field_239445_a_ + (double)((float)item.hashCode() / 2.14748365E9F);
                                            return MathHelper.positiveModulo((float)d0, 1.0F);
                                        }
                                    }
                                }

                                @Nullable
                                private BlockPos func_239444_a_(ClientWorld p_239444_1_) {
                                    return p_239444_1_.func_230315_m_().func_236043_f_() ? p_239444_1_.func_239140_u_() : null;
                                }

                                @Nullable
                                private BlockPos func_239442_a_(World p_239442_1_, CompoundNBT p_239442_2_) {
                                    boolean flag = p_239442_2_.contains("PlayerPos");
                                    boolean flag1 = p_239442_2_.contains("PlayerDimension");
                                    if (flag && flag1) {
                                        Optional<RegistryKey<World>> optional = ScentFinder.getPlayerDimension(p_239442_2_);
                                        if (optional.isPresent() && p_239442_1_.func_234923_W_() == optional.get()) {
                                            return NBTUtil.readBlockPos(p_239442_2_.getCompound("PlayerPos"));
                                        }
                                    }

                                    return null;
                                }

                                private double func_239441_a_(ItemFrameEntity p_239441_1_) {
                                    Direction direction = p_239441_1_.getHorizontalFacing();
                                    int i = direction.getAxis().isVertical() ? 90 * direction.getAxisDirection().getOffset() : 0;
                                    return (double)MathHelper.wrapDegrees(180 + direction.getHorizontalIndex() * 90 + p_239441_1_.getRotation() * 45 + i);
                                }

                                private double func_239443_a_(Vector3d p_239443_1_, Entity p_239443_2_) {
                                    return Math.atan2(p_239443_1_.getZ() - p_239443_2_.getPosZ(), p_239443_1_.getX() - p_239443_2_.getPosX());
                                }
                            });
                        }
                    };
                }
            });
        }
    }
    static class Angle {
        private double field_239445_a_;
        private double field_239446_b_;
        private long field_239447_c_;

        private Angle() {
        }

        private boolean func_239448_a_(long p_239448_1_) {
            return this.field_239447_c_ != p_239448_1_;
        }

        private void func_239449_a_(long p_239449_1_, double p_239449_3_) {
            this.field_239447_c_ = p_239449_1_;
            double d0 = p_239449_3_ - this.field_239445_a_;
            d0 = MathHelper.positiveModulo(d0 + 0.5D, 1.0D) - 0.5D;
            this.field_239446_b_ += d0 * 0.1D;
            this.field_239446_b_ *= 0.8D;
            this.field_239445_a_ = MathHelper.positiveModulo(this.field_239445_a_ + this.field_239446_b_, 1.0D);
        }
    }
}
