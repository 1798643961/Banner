package com.mohistmc.banner.injection.world.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public interface InjectionEntityType<T extends Entity> {

    @Nullable
    default T spawn(ServerLevel worldserver, @Nullable ItemStack itemstack, @Nullable Player entityhuman, BlockPos blockposition, MobSpawnType enummobspawn, boolean flag, boolean flag1, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason) {
        return null;
    }

    @Nullable
    default T spawn(ServerLevel worldserver, BlockPos blockposition, MobSpawnType enummobspawn, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason) {
        return null;
    }

    @Nullable
    default  T spawn(ServerLevel worldserver, @Nullable CompoundTag nbttagcompound, @Nullable Consumer<T> consumer, BlockPos blockposition, MobSpawnType enummobspawn, boolean flag, boolean flag1, org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason spawnReason) {
        return null;
    }
}
