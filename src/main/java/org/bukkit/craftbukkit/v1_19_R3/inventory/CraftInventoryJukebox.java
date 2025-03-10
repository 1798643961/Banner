package org.bukkit.craftbukkit.v1_19_R3.inventory;

import net.minecraft.world.Container;
import org.bukkit.block.Jukebox;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.JukeboxInventory;

/**
 * @author Mgazul by MohistMC
 * @date 2023/3/26 4:56:10
 */
public class CraftInventoryJukebox extends CraftInventory implements JukeboxInventory {

    public CraftInventoryJukebox(Container inventory) {
        super(inventory);
    }

    @Override
    public void setRecord(ItemStack item) {
        if (item == null) {
            inventory.removeItem(0, 0); // Second parameter is unused in TileEntityJukebox
        } else {
            setItem(0, item);
        }
    }

    @Override
    public ItemStack getRecord() {
        return getItem(0);
    }

    @Override
    public Jukebox getHolder() {
        return (Jukebox) inventory.getOwner();
    }
}
