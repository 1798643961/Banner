package com.mohistmc.banner.mixin.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BambooLeaves;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.bukkit.craftbukkit.v1_19_R3.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BambooStalkBlock.class)
public abstract class MixinBambooStalkBlock extends Block {

    @Shadow @Final public static IntegerProperty AGE;

    @Shadow @Final public static EnumProperty<BambooLeaves> LEAVES;

    @Shadow @Final public static IntegerProperty STAGE;

    public MixinBambooStalkBlock(Properties properties) {
        super(properties);
    }

    @Redirect(method = "performBonemeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getValue(Lnet/minecraft/world/level/block/state/properties/Property;)Ljava/lang/Comparable;"))
    private <T extends Comparable<T>> T banner$skipIfCancel(BlockState state, Property<T> property) {
        if (!state.is(Blocks.BAMBOO)) {
            return (T) Integer.valueOf(1);
        } else {
            return state.getValue(property);
        }
    }

    /**
     * @author wdog5
     * @reason
     */
    @Overwrite
    protected void growBamboo(BlockState blockStateIn, Level worldIn, BlockPos posIn, RandomSource rand, int height) {
        BlockState blockstate = worldIn.getBlockState(posIn.below());
        BlockPos blockpos = posIn.below(2);
        BlockState blockstate1 = worldIn.getBlockState(blockpos);
        BambooLeaves bambooleaves = BambooLeaves.NONE;

        boolean update = false;

        if (height >= 1) {
            if (blockstate.is(Blocks.BAMBOO) && blockstate.getValue(LEAVES) != BambooLeaves.NONE) {
                if (blockstate.is(Blocks.BAMBOO) && blockstate.getValue(LEAVES) != BambooLeaves.NONE) {
                    bambooleaves = BambooLeaves.LARGE;
                    if (blockstate1.is(Blocks.BAMBOO)) {
                        update = true;
                    }
                }
            } else {
                bambooleaves = BambooLeaves.SMALL;
            }
        }

        int newAge = blockStateIn.getValue(AGE) != 1 && !blockstate1.is(Blocks.BAMBOO) ? 0 : 1;
        int newState = (height < 11 || !(rand.nextFloat() < 0.25F)) && height != 15 ? 0 : 1;

        if (CraftEventFactory.handleBlockSpreadEvent(worldIn, posIn, posIn.above(),
                this.defaultBlockState().setValue(AGE, newAge).setValue(LEAVES, bambooleaves).setValue(STAGE, newState), 3)) {
            if (update) {
                worldIn.setBlock(posIn.below(), blockstate.setValue(LEAVES, BambooLeaves.SMALL), 3);
                worldIn.setBlock(blockpos, blockstate1.setValue(LEAVES, BambooLeaves.NONE), 3);
            }
        }
    }
}
