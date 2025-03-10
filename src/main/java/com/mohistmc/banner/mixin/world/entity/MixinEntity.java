package com.mohistmc.banner.mixin.world.entity;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mohistmc.banner.injection.world.entity.InjectionEntity;
import net.minecraft.commands.CommandSource;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Nameable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityAccess;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_19_R3.CraftServer;
import org.bukkit.craftbukkit.v1_19_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_19_R3.block.CraftBlock;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_19_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.projectiles.ProjectileSource;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.ActivationRange;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;
import java.util.UUID;

@Mixin(Entity.class)
public abstract class MixinEntity implements Nameable, EntityAccess, CommandSource, InjectionEntity {

    @Shadow public Level level;
    @Shadow @Final public static int TOTAL_AIR_SUPPLY;
    @Shadow private float yRot;

    @Shadow public abstract double getX();

    @Shadow public abstract double getZ();

    @Shadow protected abstract void handleNetherPortal();

    @Shadow public abstract void setSecondsOnFire(int seconds);

    @Shadow protected abstract SoundEvent getSwimSound();

    @Shadow protected abstract SoundEvent getSwimSplashSound();

    @Shadow protected abstract SoundEvent getSwimHighSpeedSplashSound();

    @Shadow public abstract boolean isPushable();

    @Shadow public abstract Pose getPose();

    @Shadow public abstract String getScoreboardName();

    @Shadow private float xRot;
    @Shadow public int remainingFireTicks;
    @Shadow public boolean horizontalCollision;

    @Shadow protected abstract Vec3 collide(Vec3 vec);

    @Shadow public abstract double getY();

    @Shadow public abstract float getYRot();

    @Shadow public abstract float getXRot();

    @Shadow public int tickCount;

    @Shadow public abstract int getMaxAirSupply();

    @Shadow public abstract void setInvisible(boolean invisible);

    @Shadow @Nullable private Entity vehicle;

    @Shadow public abstract void gameEvent(net.minecraft.world.level.gameevent.GameEvent event, @Nullable Entity entity);

    @Shadow public ImmutableList<Entity> passengers;

    @Shadow @Nullable public abstract Entity getFirstPassenger();

    @Shadow @Final private static EntityDataAccessor<Integer> DATA_AIR_SUPPLY_ID;

    @Shadow public abstract SynchedEntityData getEntityData();

    @Shadow public abstract int getAirSupply();

    @Shadow @Final protected SynchedEntityData entityData;

    @Shadow public abstract boolean isSwimming();

    @Shadow public abstract boolean fireImmune();

    @Shadow public abstract boolean hurt(DamageSource source, float amount);

    @Shadow public abstract DamageSources damageSources();

    private CraftEntity bukkitEntity;
    public final org.spigotmc.ActivationRange.ActivationType activationType =
            org.spigotmc.ActivationRange.initializeEntityActivationType((Entity) (Object) this);
    public boolean defaultActivationState;
    public long activatedTick = Integer.MIN_VALUE;
    public boolean generation;
    public boolean persist = true;
    public boolean visibleByDefault = true;
    public boolean valid;
    public int maxAirTicks = getDefaultMaxAirSupply(); // CraftBukkit - SPIGOT-6907: re-implement LivingEntity#setMaximumAir()
    public org.bukkit.projectiles.ProjectileSource projectileSource; // For projectiles only
    public boolean lastDamageCancelled; // SPIGOT-5339, SPIGOT-6252, SPIGOT-6777: Keep track if the event was canceled
    public boolean persistentInvisibility = false;
    public BlockPos lastLavaContact;
    private static transient BlockPos banner$damageEventBlock;
    private static final int CURRENT_LEVEL = 2;

    @Override
    public void inactiveTick() {

    }

    @Override
    public CraftEntity getBukkitEntity() {
        if (bukkitEntity == null) {
            bukkitEntity = CraftEntity.getEntity(level.getCraftServer(), ((Entity) (Object) this));
        }
        return bukkitEntity;
    }

    @Override
    public int getDefaultMaxAirSupply() {
        return TOTAL_AIR_SUPPLY;
    }

    @Override
    public float getBukkitYaw() {
        return this.yRot;
    }

    @Override
    public boolean isChunkLoaded() {
        return level.hasChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }

    @Override
    public void postTick() {
        // No clean way to break out of ticking once the entity has been copied to a new world, so instead we move the portalling later in the tick cycle
        if (!(((Entity) (Object) this) instanceof ServerPlayer)) {
            this.handleNetherPortal();
        }
    }


    @Override
    public void setSecondsOnFire(int i, boolean callEvent) {
        if (callEvent) {
            EntityCombustEvent event = new EntityCombustEvent(this.getBukkitEntity(), i);
            this.level.getCraftServer().getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            i = event.getDuration();
        }
    }

    @Inject(method = "setSecondsOnFire", at = @At("HEAD"))
    private void banner$setSecondsOnFire(int seconds, CallbackInfo ci) {
        setSecondsOnFire(seconds, true);
    }

    @Override
    public SoundEvent getSwimSound0() {
        return getSwimSound();
    }

    @Override
    public SoundEvent getSwimSplashSound0() {
        return getSwimSplashSound();
    }

    @Override
    public SoundEvent getSwimHighSpeedSplashSound0() {
        return getSwimHighSpeedSplashSound();
    }

    @Override
    public boolean canCollideWithBukkit(Entity entity) {
        return isPushable();
    }

    @Inject(method = "getMaxAirSupply", cancellable = true, at = @At("RETURN"))
    private void banner$useBukkitMaxAir(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(this.maxAirTicks);
    }

    @Inject(method = "setPose", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;set(Lnet/minecraft/network/syncher/EntityDataAccessor;Ljava/lang/Object;)V"))
    public void banner$setPose$EntityPoseChangeEvent(Pose pose, CallbackInfo ci) {
        if (pose == this.getPose()) {
            ci.cancel();
            return;
        }
        EntityPoseChangeEvent event = new EntityPoseChangeEvent(this.getBukkitEntity(), org.bukkit.entity.Pose.values()[pose.ordinal()]);
        Bukkit.getPluginManager().callEvent(event);
    }

    @Inject(method = "setRot", cancellable = true, at = @At(value = "HEAD"))
    public void banner$infCheck(float yaw, float pitch, CallbackInfo ci) {
        // CraftBukkit start - yaw was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(yaw)) {
            this.yRot = 0;
            ci.cancel();
        }

        if (yaw == Float.POSITIVE_INFINITY || yaw == Float.NEGATIVE_INFINITY) {
            if (((Object) this) instanceof Player) {
                Bukkit.getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid yaw");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite yaw (Are you hacking?)");
            }
            this.yRot = 0;
            ci.cancel();
        }

        // pitch was sometimes set to NaN, so we need to set it back to 0
        if (Float.isNaN(pitch)) {
            this.xRot = 0;
            ci.cancel();
        }

        if (pitch == Float.POSITIVE_INFINITY || pitch == Float.NEGATIVE_INFINITY) {
            if (((Object) this) instanceof Player) {
                Bukkit.getLogger().warning(this.getScoreboardName() + " was caught trying to crash the server with an invalid pitch");
                ((CraftPlayer) this.getBukkitEntity()).kickPlayer("Infinite pitch (Are you hacking?)");
            }
            this.xRot = 0;
            ci.cancel();
        }
        // CraftBukkit end
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;handleNetherPortal()V"))
    public void banner$baseTick$moveToPostTick(Entity entity) {
        if ((Object) this instanceof ServerPlayer) this.handleNetherPortal();// CraftBukkit - // Moved up to postTick
    }

    @Redirect(method = "updateFluidHeightAndDoFluidPushing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getFlow(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 banner$setLava(FluidState instance, BlockGetter level, BlockPos pos) {
        if (instance.getType().is(FluidTags.LAVA)) {
            lastLavaContact = pos.immutable();
        }
        return instance.getFlow(level, pos);
    }

    @Redirect(method = "baseTick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/Entity;isInLava()Z"))
    private boolean banner$resetLava(Entity instance) {
        var ret = instance.isInLava();
        if (!ret) {
            this.lastLavaContact = null;
        }
        return ret;
    }

    @Redirect(method = "lavaHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    public void banner$setOnFireFromLava$bukkitEvent(Entity entity, int seconds) {
        var damager = (lastLavaContact == null) ? null : CraftBlock.at(level, lastLavaContact);
        CraftEventFactory.blockDamage = damager;
        if ((Object) this instanceof LivingEntity && remainingFireTicks <= 0) {
            var damagee = this.getBukkitEntity();
            EntityCombustEvent combustEvent = new EntityCombustByBlockEvent(damager, damagee, 15);
            Bukkit.getPluginManager().callEvent(combustEvent);

            if (!combustEvent.isCancelled()) {
                this.setSecondsOnFire(combustEvent.getDuration());
            }
        } else {
            // This will be called every single tick the entity is in lava, so don't throw an event
            this.setSecondsOnFire(15);
        }
    }

    @Inject(method = "lavaHurt", at = @At("RETURN"))
    private void banner$resetBlockDamage(CallbackInfo ci) {
        CraftEventFactory.blockDamage = null;
    }

    @ModifyArg(method = "move", index = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private BlockPos banner$captureBlockWalk(BlockPos pos) {
        banner$damageEventBlock = pos;
        return pos;
    }

    @Inject(method = "move", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/Block;stepOn(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/Entity;)V"))
    private void banner$resetBlockWalk(MoverType type, Vec3 pos, CallbackInfo ci) {
        banner$damageEventBlock = null;
    }

    /**
    @Inject(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity$MovementEmission;emitsAnything()Z"))
    private void banner$move$blockCollide(MoverType typeIn, Vec3 pos, CallbackInfo ci) {
        if (horizontalCollision && this.getBukkitEntity() instanceof Vehicle vehicle) {
            org.bukkit.block.Block block = this.level.getWorld().getBlockAt(Mth.floor(this.getX()), Mth.floor(this.getY()), Mth.floor(this.getZ()));
            Vec3 vec3d = this.collide(pos);
            if (pos.x > vec3d.x) {
                block = block.getRelative(BlockFace.EAST);
            } else if (vec3d.x < vec3d.x) {
                block = block.getRelative(BlockFace.WEST);
            } else if (pos.z > vec3d.z) {
                block = block.getRelative(BlockFace.SOUTH);
            } else if (pos.z < vec3d.z) {
                block = block.getRelative(BlockFace.NORTH);
            }

            if (block.getType() != org.bukkit.Material.AIR) {
                VehicleBlockCollisionEvent event = new VehicleBlockCollisionEvent(vehicle, block);
                Bukkit.getPluginManager().callEvent(event);
            }
        }
    }*/

    @Inject(method = "absMoveTo(DDDFF)V", at = @At("RETURN"))
    private void banner$loadChunk(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        if (this.valid)
            this.level.getChunk((int) Math.floor(this.getX()) >> 4, (int) Math.floor(this.getZ()) >> 4);
    }

    @Inject(method = "saveAsPassenger", cancellable = true, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/entity/Entity;getEncodeId()Ljava/lang/String;"))
    public void banner$writeUnlessRemoved$persistCheck(CompoundTag compound, CallbackInfoReturnable<Boolean> cir) {
        if (!this.persist)
            cir.setReturnValue(false);
    }


    @Inject(method = "saveWithoutId", at = @At(value = "INVOKE_ASSIGN", ordinal = 1, target = "Lnet/minecraft/nbt/CompoundTag;put(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"))
    public void banner$writeWithoutTypeId$InfiniteValueCheck(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if (Float.isNaN(this.getYRot())) {
            this.yRot = 0;
        }

        if (Float.isNaN(this.getXRot())) {
            this.xRot = 0;
        }
    }

    /**
    @Inject(method = "saveWithoutId", at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0, target = "Lnet/minecraft/nbt/CompoundTag;putUUID(Ljava/lang/String;Ljava/util/UUID;)V"))
    public void banner$writeWithoutTypeId$CraftBukkitNBT(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        compound.putLong("WorldUUIDLeast", this.level.getWorld().getUID().getLeastSignificantBits());
        compound.putLong("WorldUUIDMost", this.level.getWorld().getUID().getMostSignificantBits());
        compound.putInt("Bukkit.updateLevel", CURRENT_LEVEL);
        compound.putInt("Spigot.ticksLived", this.tickCount);
        if (!this.persist) {
            compound.putBoolean("Bukkit.persist", this.persist);
        }
        if (!this.visibleByDefault) {
            compound.putBoolean("Bukkit.visibleByDefault", this.visibleByDefault);
        }
        if (this.persistentInvisibility) {
            compound.putBoolean("Bukkit.invisible", this.persistentInvisibility);
        }
        if (maxAirTicks != getDefaultMaxAirSupply()) {
            compound.putInt("Bukkit.MaxAirSupply", getMaxAirSupply());
        }
    }*/

    @Inject(method = "saveWithoutId", at = @At(value = "RETURN"))
    public void banner$writeWithoutTypeId$StoreBukkitValues(CompoundTag compound, CallbackInfoReturnable<CompoundTag> cir) {
        if (this.bukkitEntity != null) {
            this.bukkitEntity.storeBukkitValues(compound);
        }
    }

    private static boolean isLevelAtLeast(CompoundTag tag, int level) {
        return tag.contains("Bukkit.updateLevel") && tag.getInt("Bukkit.updateLevel") >= level;
    }

    /**
    @Inject(method = "load", at = @At(value = "RETURN"))
    public void banner$read$ReadBukkitValues(CompoundTag compound, CallbackInfo ci) {
        // CraftBukkit start
        if ((Object) this instanceof LivingEntity entity) {
            this.tickCount = compound.getInt("Spigot.ticksLived");
        }
        this.persist = !compound.contains("Bukkit.persist") || compound.getBoolean("Bukkit.persist");
        this.visibleByDefault = !compound.contains("Bukkit.visibleByDefault") || compound.getBoolean("Bukkit.visibleByDefault");
        // CraftBukkit end

        // CraftBukkit start - Reset world
        if ((Object) this instanceof ServerPlayer) {
            Server server = Bukkit.getServer();
            org.bukkit.World bworld = null;

            String worldName = compound.getString("world");

            if (compound.contains("WorldUUIDMost") && compound.contains("WorldUUIDLeast")) {
                UUID uid = new UUID(compound.getLong("WorldUUIDMost"), compound.getLong("WorldUUIDLeast"));
                bworld = server.getWorld(uid);
            } else {
                bworld = server.getWorld(worldName);
            }

            if (bworld == null) {
                bworld = (((CraftServer) server).getServer().getLevel(Level.OVERWORLD)).getWorld();
            }

            ((ServerPlayer) (Object) this).setLevel(bworld == null ? null : ((CraftWorld) bworld).getHandle());
        }
        this.getBukkitEntity().readBukkitValues(compound);
        if (compound.contains("Bukkit.invisible")) {
            boolean bukkitInvisible = compound.getBoolean("Bukkit.invisible");
            this.setInvisible(bukkitInvisible);
            this.persistentInvisibility = bukkitInvisible;
        }
        if (compound.contains("Bukkit.MaxAirSupply")) {
            maxAirTicks = compound.getInt("Bukkit.MaxAirSupply");
        }
        // CraftBukkit end
    }*/

    @Inject(method = "setInvisible", cancellable = true, at = @At("HEAD"))
    private void banner$preventVisible(boolean invisible, CallbackInfo ci) {
        if (this.persistentInvisibility) {
            ci.cancel();
        }
    }

    @Inject(method = "spawnAtLocation(Lnet/minecraft/world/item/ItemStack;F)Lnet/minecraft/world/entity/item/ItemEntity;",
            cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    public void banner$entityDropItem(ItemStack stack, float offsetY, CallbackInfoReturnable<ItemEntity> cir, ItemEntity itemEntity) {
        EntityDropItemEvent event = new EntityDropItemEvent(this.getBukkitEntity(), (org.bukkit.entity.Item) (itemEntity).getBukkitEntity());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(null);
        }
    }

    @Redirect(method = "startRiding(Lnet/minecraft/world/entity/Entity;Z)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;addPassenger(Lnet/minecraft/world/entity/Entity;)V"))
    private void banner$startRiding(Entity entity, Entity pPassenger) {
        if (!(entity).banner$addPassenger(pPassenger)) {
            this.vehicle = null;
        }
    }

    @Redirect(method = "removeVehicle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;removePassenger(Lnet/minecraft/world/entity/Entity;)V"))
    private void banner$stopRiding(Entity entity, Entity passenger) {
        if (!(entity).banner$removePassenger(passenger)) {
            this.vehicle = entity;
        }
    }

    @Override
    public boolean banner$addPassenger(Entity entity) {
        if (entity.getVehicle() != (Object) this) {
            throw new IllegalStateException("Use x.startRiding(y), not y.addPassenger(x)");
        } else {
            // CraftBukkit start
            com.google.common.base.Preconditions.checkState(!(entity).getPassengers().contains(((Entity) (Object) this)), "Circular entity riding! %s %s", this, entity);

            CraftEntity craft = (CraftEntity) (entity.getBukkitEntity().getVehicle());
            Entity orig = craft == null ? null : craft.getHandle();
            if (getBukkitEntity() instanceof Vehicle && (entity).getBukkitEntity() instanceof org.bukkit.entity.LivingEntity) {
                VehicleEnterEvent event = new VehicleEnterEvent(
                        (Vehicle) getBukkitEntity(),
                        (entity.getBukkitEntity()
                ));
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) entity.getBukkitEntity().getVehicle();
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityMountEvent event = new org.spigotmc.event.entity.EntityMountEvent((entity).getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (this.passengers.isEmpty()) {
                this.passengers = ImmutableList.of(entity);
            } else {
                List<Entity> list = Lists.newArrayList(this.passengers);

                if (!this.level.isClientSide && entity instanceof Player && !(this.getFirstPassenger() instanceof Player)) {
                    list.add(0, entity);
                } else {
                    list.add(entity);
                }

                this.passengers = ImmutableList.copyOf(list);
            }

            this.gameEvent(GameEvent.ENTITY_MOUNT, entity);
        }
        return true; // CraftBukkit
    }

    @Override
    public boolean banner$removePassenger(Entity entity) {
        if (entity.getVehicle() == (Object) this) {
            throw new IllegalStateException("Use x.stopRiding(y), not y.removePassenger(x)");
        } else {
            // CraftBukkit start
            CraftEntity craft = (CraftEntity) (entity.getBukkitEntity().getVehicle());
            Entity orig = craft == null ? null : craft.getHandle();
            if (getBukkitEntity() instanceof Vehicle && (entity.getBukkitEntity() instanceof org.bukkit.entity.LivingEntity)) {
                VehicleExitEvent event = new VehicleExitEvent(
                        (Vehicle) getBukkitEntity(),
                        (org.bukkit.entity.LivingEntity) (entity.getBukkitEntity()
                ));
                // Suppress during worldgen
                if (this.valid) {
                    Bukkit.getPluginManager().callEvent(event);
                }
                CraftEntity craftn = (CraftEntity) (entity.getBukkitEntity().getVehicle());
                Entity n = craftn == null ? null : craftn.getHandle();
                if (event.isCancelled() || n != orig) {
                    return false;
                }
            }
            // CraftBukkit end
            // Spigot start
            org.spigotmc.event.entity.EntityDismountEvent event = new org.spigotmc.event.entity.EntityDismountEvent((entity).getBukkitEntity(), this.getBukkitEntity());
            // Suppress during worldgen
            if (this.valid) {
                Bukkit.getPluginManager().callEvent(event);
            }
            if (event.isCancelled()) {
                return false;
            }
            // Spigot end
            if (this.passengers.size() == 1 && this.passengers.get(0) == entity) {
                this.passengers = ImmutableList.of();
            } else {
                this.passengers = this.passengers.stream().filter((entity1) -> entity1 != entity)
                        .collect(ImmutableList.toImmutableList());
            }

            entity.boardingCooldown = 60;
            this.gameEvent(GameEvent.ENTITY_DISMOUNT, entity);
        }
        return true; // CraftBukkit
    }

    @Inject(method = "setSwimming", cancellable = true, at = @At(value = "HEAD"))
    public void banner$setSwimming$EntityToggleSwimEvent(boolean flag, CallbackInfo ci) {
        // CraftBukkit start
        if (this.valid && this.isSwimming() != flag && (Object) this instanceof LivingEntity) {
            if (CraftEventFactory.callToggleSwimEvent((LivingEntity) (Object) this, flag).isCancelled()) {
                ci.cancel();
            }
        }
        // CraftBukkit end
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;setSecondsOnFire(I)V"))
    public void banner$onStruckByLightning$EntityCombustByEntityEvent0(Entity entity, int seconds) {
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = entity.getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        // CraftBukkit start - Call a combust event when lightning strikes
        EntityCombustByEntityEvent entityCombustEvent = new EntityCombustByEntityEvent(stormBukkitEntity, thisBukkitEntity, 8);
        pluginManager.callEvent(entityCombustEvent);
        if (!entityCombustEvent.isCancelled()) {
            this.setSecondsOnFire(entityCombustEvent.getDuration());
        }
        // CraftBukkit end
    }

    @Inject(method = "setAirSupply", cancellable = true, at = @At(value = "HEAD"))
    public void banner$setAir$EntityAirChangeEvent(int air, CallbackInfo ci) {
        // CraftBukkit start
        EntityAirChangeEvent event = new EntityAirChangeEvent(this.getBukkitEntity(), air);
        // Suppress during worldgen
        if (this.valid) {
            event.getEntity().getServer().getPluginManager().callEvent(event);
        }
        if (event.isCancelled() && this.getAirSupply() != -1) {
            ci.cancel();
            this.getEntityData().markDirty(DATA_AIR_SUPPLY_ID);
            return;
        }
        this.entityData.set(DATA_AIR_SUPPLY_ID, event.getAmount());
        // CraftBukkit end
    }

    @Redirect(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    public boolean banner$onStruckByLightning$EntityCombustByEntityEvent1(Entity instance, DamageSource source, float amount) {
        final org.bukkit.entity.Entity thisBukkitEntity = this.getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = instance.getBukkitEntity();
        final PluginManager pluginManager = Bukkit.getPluginManager();
        if (thisBukkitEntity instanceof Hanging) {
            HangingBreakByEntityEvent hangingEvent = new HangingBreakByEntityEvent((Hanging) thisBukkitEntity, stormBukkitEntity);
            pluginManager.callEvent(hangingEvent);

            if (hangingEvent.isCancelled()) {
                return false;
            }
        }

        if (this.fireImmune()) {
            return false;
        }
        CraftEventFactory.entityDamage = instance;
        if (!this.hurt(this.damageSources().lightningBolt(), amount)) {
            CraftEventFactory.entityDamage = null;
            return false;
        }
        return true;
    }

    @Override
    public ActivationRange.ActivationType bridge$activationType() {
        return activationType;
    }

    @Override
    public long bridge$activatedTick() {
        return activatedTick;
    }

    @Override
    public void banner$setActivatedTick(long activatedTick) {
        this.activatedTick = activatedTick;
    }

    @Override
    public boolean bridge$defaultActivationState() {
        return defaultActivationState;
    }

    @Override
    public void banner$setDefaultActivationState(boolean state) {
        defaultActivationState = state;
    }

    @Override
    public boolean bridge$generation() {
        return generation;
    }

    @Override
    public void banner$setGeneration(boolean gen) {
        this.generation = gen;
    }

    @Override
    public boolean bridge$persist() {
        return persist;
    }

    @Override
    public void banner$setPersist(boolean persist) {
        this.persist = persist;
    }

    @Override
    public boolean bridge$visibleByDefault() {
        return visibleByDefault;
    }

    @Override
    public void banner$setVisibleByDefault(boolean visibleByDefault) {
        this.visibleByDefault = visibleByDefault;
    }

    @Override
    public boolean bridge$valid() {
        return valid;
    }

    @Override
    public void banner$setValid(boolean valid) {
        this.valid = valid;
    }

    @Override
    public int bridge$maxAirTicks() {
        return maxAirTicks;
    }

    @Override
    public void banner$setMaxAirTicks(int maxAirTicks) {
        this.maxAirTicks = maxAirTicks;
    }

    @Override
    public ProjectileSource bridge$projectileSource() {
        return projectileSource;
    }

    @Override
    public void banner$setProjectileSource(ProjectileSource projectileSource) {
        this.projectileSource = projectileSource;
    }

    @Override
    public boolean bridge$lastDamageCancelled() {
        return lastDamageCancelled;
    }

    @Override
    public void banner$setLastDamageCancelled(boolean lastDamageCancelled) {
        this.lastDamageCancelled = lastDamageCancelled;
    }

    @Override
    public boolean bridge$persistentInvisibility() {
        return persistentInvisibility;
    }

    @Override
    public void banner$setPersistentInvisibility(boolean persistentInvisibility) {
        this.persistentInvisibility = persistentInvisibility;
    }

    @Override
    public BlockPos bridge$lastLavaContact() {
        return lastLavaContact;
    }

    @Override
    public void banner$setLastLavaContact(BlockPos lastLavaContact) {
        this.lastLavaContact = lastLavaContact;
    }
}
