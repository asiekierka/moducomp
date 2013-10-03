package pl.asie.moducomp;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

final class CreativeTabModuComp extends CreativeTabs
{
    CreativeTabModuComp(String name)
    {
        super(name);
    }

    @SideOnly(Side.CLIENT)
    public int getTabIconItemIndex()
    {
        return ModularComputing.instance.itemCPUAreia.itemID;
    }
}