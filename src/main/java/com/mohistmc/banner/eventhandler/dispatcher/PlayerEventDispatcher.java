package com.mohistmc.banner.eventhandler.dispatcher;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class PlayerEventDispatcher {

    public static void dispatcherPlayer() {
        EntitySleepEvents.START_SLEEPING.register((entity, sleepingPos) -> {
            if (!entity.getLevel().dimensionType().bedWorks()) {
                explodeBed(entity.getBlockStateOn(), entity.getLevel(), entity.getOnPos());
            }
        });
    }


    // CraftBukkit start
    private static void explodeBed(BlockState iblockdata, Level world, BlockPos blockposition) {
        {
            {
                world.removeBlock(blockposition, false);
                BlockPos blockposition1 = blockposition.relative((Direction) (iblockdata.getValue(BedBlock.FACING)).getOpposite());

                if (world.getBlockState(blockposition1).getBlock() instanceof BedBlock) {
                    world.removeBlock(blockposition1, false);
                }

                Vec3 vec3d = blockposition.getCenter();

                world.explode((Entity) null, world.damageSources().badRespawnPointExplosion(vec3d), (ExplosionDamageCalculator) null, vec3d, 5.0F, true, Level.ExplosionInteraction.BLOCK);
            }
        }
    }
    // CraftBukkit end

}
