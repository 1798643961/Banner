package com.mohistmc.banner.mixin.world.entity.projectile;

import com.mohistmc.banner.injection.world.entity.projectile.InjectionArrow;
import net.minecraft.world.entity.projectile.Arrow;
import org.spongepowered.asm.mixin.Mixin;

// TODO fix inject methods
@Mixin(Arrow.class)
public class MixinArrow implements InjectionArrow {
}
