package org.bukkit.craftbukkit.v1_19_R3.block.data.type;

import org.bukkit.block.data.type.BubbleColumn;
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData;

public abstract class CraftBubbleColumn extends CraftBlockData implements BubbleColumn {

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty DRAG = getBoolean("drag");

    @Override
    public boolean isDrag() {
        return get(DRAG);
    }

    @Override
    public void setDrag(boolean drag) {
        set(DRAG, drag);
    }
}
