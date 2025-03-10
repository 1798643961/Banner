package com.mohistmc.banner.mixin.world.level.block;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractCandleBlock.class)
public class MixinAbstractCandleBlock {

    @Inject(method = "onProjectileHit", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/AbstractCandleBlock;" +
                    "setLit(Lnet/minecraft/world/level/LevelAccessor;" +
                    "Lnet/minecraft/world/level/block/state/BlockState;" +
                    "Lnet/minecraft/core/BlockPos;Z)V",
            shift = At.Shift.BEFORE))
    private void banner$callBlockIgniteEvent(Level level, BlockState state, BlockHitResult hit, Projectile projectile, CallbackInfo ci) {
        // CraftBukkit start
        if (org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory.callBlockIgniteEvent(level, hit.getBlockPos(), projectile).isCancelled()) {
            return;
        }
        // CraftBukkit end
    }
}
