package info.jbcs.minecraft.chisel.carving;

import info.jbcs.minecraft.chisel.Chisel;
import info.jbcs.minecraft.chisel.Configurations;
import info.jbcs.minecraft.chisel.block.BlockMarbleSlab;
import info.jbcs.minecraft.chisel.carving.CarvableVariation.CarvableVariationCTM;
import info.jbcs.minecraft.chisel.client.render.CTM;
import info.jbcs.minecraft.chisel.client.render.TextureSubmap;
import info.jbcs.minecraft.chisel.item.ItemCarvable;
import info.jbcs.minecraft.utilities.GeneralClient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockPane;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.IBlockAccess;
import cpw.mods.fml.common.registry.GameRegistry;

public class CarvableHelper
{
    static final String modName = "chisel";
    public static final ArrayList<Block> chiselBlocks = new ArrayList<Block>();

    public static final int NORMAL = 0;
    public static final int TOPSIDE = 1;
    public static final int TOPBOTSIDE = 2;
    public static final int CTM3 = 3;
    public static final int CTMV = 4;
    public static final int CTMH = 5;
    public static final int V9 = 6;
    public static final int V4 = 7;
    public static final int CTMX = 8;

    public CarvableHelper()
    {}

    public ArrayList<CarvableVariation> variations = new ArrayList<CarvableVariation>();
    CarvableVariation[] map = new CarvableVariation[16];
    public boolean forbidChiseling = false;
    public String blockName;

    public void addVariation(String description, int metadata, Block bb)
    {
        addVariation(description, metadata, null, bb, 0);
    }

    public void addVariation(String description, int metadata, Block bb, int blockMeta)
    {
        addVariation(description, metadata, null, bb, blockMeta);
    }

    public void addVariation(String description, int metadata, Block bb, int blockMeta, Material material)
    {
        addVariation(description, metadata, null, bb, blockMeta);
    }

    public void addVariation(String description, int metadata, String texture)
    {
        addVariation(description, metadata, texture, null, 0);
    }

    public void addVariation(String description, int metadata, String texture, Block block, int blockMeta)
    {
        if (variations.size() >= 16)
            return;

        if (blockName == null && block != null)
            blockName = block.getLocalizedName();
        else if (blockName == null && description != null)
            blockName = description;

        CarvableVariation variation = new CarvableVariation();
        variation.description = description;
        variation.metadata = metadata;
        variation.blockName = blockName;

        if (texture != null)
        {
            variation.texture = texture;

            String path = "/assets/" + modName + "/textures/blocks/" + variation.texture;

            boolean any = Chisel.class.getResource(path + ".png") != null;
            boolean ctm3 = Chisel.class.getResource(path + "-ctm1.png") != null && Chisel.class.getResource(path + "-ctm2.png") != null
                    && Chisel.class.getResource(path + "-ctm3.png") != null;
            boolean ctmv = Chisel.class.getResource(path + "-ctmv.png") != null;
            boolean ctmh = Chisel.class.getResource(path + "-ctmh.png") != null;
            boolean side = Chisel.class.getResource(path + "-side.png") != null;
            boolean top = Chisel.class.getResource(path + "-top.png") != null;
            boolean bot = Chisel.class.getResource(path + "-bottom.png") != null;
            boolean v9 = Chisel.class.getResource(path + "-v9.png") != null;
            boolean v4 = Chisel.class.getResource(path + "-v4.png") != null;
            boolean ctmx = Chisel.class.getResource(path + "-ctm.png") != null;

            if (ctm3)
            {
                variation.kind = CTM3;
            }
            else if (ctmh && top)
            {
                variation.kind = CTMH;
            }
            else if (ctmv && top)
            {
                variation.kind = CTMV;
            }
            else if (bot && top && side)
            {
                variation.kind = TOPBOTSIDE;
            }
            else if (top && side)
            {
                variation.kind = TOPSIDE;
            }
            else if (v9)
            {
                variation.kind = V9;
            }
            else if (v4)
            {
                variation.kind = V4;
            }
            else if (any && ctmx && !Configurations.disableCTM)
            {
                variation.kind = CTMX;
            }
            else if (any)
            {
                variation.kind = NORMAL;
            }
            else
            {
                throw new RuntimeException("No valid textures found for chisel block variation '" + description + "' (" + variation.texture + ")");
            }
        }
        else
        {
            variation.block = block;
            variation.kind = 2;
            variation.blockMeta = blockMeta;
        }

        variations.add(variation);
        map[metadata] = variation;
    }

    public CarvableVariation getVariation(int metadata)
    {
        if (metadata < 0 || metadata > 15)
            metadata = 0;

        CarvableVariation variation = map[metadata];
        if (variation == null)
            return null;

        return variation;
    }   

    public IIcon getIcon(int side, int metadata)
    {
        if (metadata < 0 || metadata > 15)
            metadata = 0;

        CarvableVariation variation = map[metadata];
        if (variation == null)
            return GeneralClient.getMissingIcon();

        switch (variation.kind)
        {
        case NORMAL:
            return variation.icon;
        case TOPSIDE:
            if (side == 0 || side == 1)
                return variation.iconTop;
            else
                return variation.icon;
        case TOPBOTSIDE:
            if (side == 1)
                return variation.iconTop;
            else if (side == 0)
                return variation.iconBot;
            else
                return variation.icon;
        case CTM3:
            return variation.ctm.seams[0].icons[0];
        case CTMV:
            if (side < 2)
                return variation.iconTop;
            else
                return variation.seamsCtmVert.icons[0];
        case CTMH:
            if (side < 2)
                return variation.iconTop;
            else
                return variation.seamsCtmVert.icons[0];
        case V9:
            return variation.variations9.icons[4];
        case V4:
            return variation.variations9.icons[0];
        case CTMX:
            return variation.icon;
        }

        return GeneralClient.getMissingIcon();
    }

    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side)
    {
        int metadata = MathHelper.clamp_int(world.getBlockMetadata(x, y, z), 0, 15);

        CarvableVariation variation = map[metadata];
        if (variation == null)
            return GeneralClient.getMissingIcon();

        switch (variation.kind)
        {
        case NORMAL:
        case TOPSIDE:
        case TOPBOTSIDE:
            return getIcon(side, metadata);
        case CTM3:
            int tex = CTM.getTexture(world, x, y, z, side);

            int row = tex / 16;
            int col = tex % 16;

            return variation.ctm.seams[col / 4].icons[col % 4 + row * 4];
        case CTMV:
        {
            if (side < 2)
                return variation.iconTop;

            Block block = world.getBlock(x, y, z);
            boolean topConnected = CTM.isConnected(world, x, y + 1, z, side, block, metadata);
            boolean botConnected = CTM.isConnected(world, x, y - 1, z, side, block, metadata);

            if (topConnected && botConnected)
                return variation.seamsCtmVert.icons[2];
            if (topConnected && !botConnected)
                return variation.seamsCtmVert.icons[3];
            if (!topConnected && botConnected)
                return variation.seamsCtmVert.icons[1];
            return variation.seamsCtmVert.icons[0];
        }
        case CTMH:
            if (side < 2)
                return variation.iconTop;

            Block block = world.getBlock(x, y, z);

            boolean p;
            boolean n;
            boolean reverse = side == 2 || side == 4;

            if (side < 4)
            {
                p = CTM.isConnected(world, x - 1, y, z, side, block, metadata);
                n = CTM.isConnected(world, x + 1, y, z, side, block, metadata);
            }
            else
            {
                p = CTM.isConnected(world, x, y, z - 1, side, block, metadata);
                n = CTM.isConnected(world, x, y, z + 1, side, block, metadata);
            }

            if (p && n)
                return variation.seamsCtmVert.icons[1];
            else if (p)
                return variation.seamsCtmVert.icons[reverse ? 2 : 3];
            else if (n)
                return variation.seamsCtmVert.icons[reverse ? 3 : 2];
            return variation.seamsCtmVert.icons[0];
        case V9:
        case V4:
            int index = x + y * 606731 + z * 571163 + side * 555491;
            if (index < 0)
                index = -index;

            return variation.variations9.icons[index % ((variation.kind == V9) ? 9 : 4)];
        case CTMX:
            return variation.icon;
        }

        return GeneralClient.getMissingIcon();
    }

    public void register(Block block, String name)
    {
        register(block, name, ItemCarvable.class);
    }

    public void registerBlock(Block block, String name)
    {
        registerBlock(block, name, ItemCarvable.class);
    }

    void registerBlock(Block block, String name, Class<? extends ItemBlock> cl)
    {
        block.setBlockName(name);
        GameRegistry.registerBlock(block, cl, name);
        chiselBlocks.add(block);
    }

    public void register(Block block, String name, Class<? extends ItemBlock> cl)
    {
        registerBlock(block, name, cl);

        if (block instanceof BlockMarbleSlab)
        {
            BlockMarbleSlab slab = (BlockMarbleSlab) block;
        }

        for (CarvableVariation variation : variations)
        {
            registerVariation(name, variation, block, variation.metadata);

            if (block instanceof BlockMarbleSlab && ((BlockMarbleSlab) block).isBottom)
            {
                BlockMarbleSlab slab = (BlockMarbleSlab) block;

                if (!forbidChiseling)
                {
                    Carving.chisel.addVariation(name + ".top", slab.top, variation.metadata, 0);
                    Carving.chisel.setGroupClass(name + ".top", name);
                }
            }
        }
    }

    public void registerVariation(String name, CarvableVariation variation, Block block, int blockMeta)
    {
        // TODO Multipart registry goes here
        if (forbidChiseling)
            return;

        if (variation.block == null)
        {
            Carving.chisel.addVariation(name, block, blockMeta, variation.metadata);
        }
        else
        {
            Carving.chisel.addVariation(name, variation.block, variation.blockMeta, variation.metadata);
        }
    }

    public void registerBlockIcons(String modName, Block block, IIconRegister register)
    {
        for (CarvableVariation variation : variations)
        {
            if (variation.block != null)
            {
                variation.block.registerBlockIcons(register);

                if (variation.block instanceof BlockPane)
                {
                    variation.icon = variation.block.getBlockTextureFromSide(2);
                    variation.iconTop = ((BlockPane) variation.block).getBlockTextureFromSide(0);
                    variation.iconBot = ((BlockPane) variation.block).getBlockTextureFromSide(0);

                }
                else
                {
                    switch (variation.kind)
                    {
                    case NORMAL:
                        variation.icon = variation.block.getIcon(2, variation.blockMeta);
                        break;
                    case TOPSIDE:
                        variation.icon = variation.block.getIcon(2, variation.blockMeta);
                        variation.iconTop = variation.block.getIcon(0, variation.blockMeta);
                        break;
                    case TOPBOTSIDE:
                        variation.icon = variation.block.getIcon(2, variation.blockMeta);
                        variation.iconTop = variation.block.getIcon(1, variation.blockMeta);
                        variation.iconBot = variation.block.getIcon(0, variation.blockMeta);
                        break;
                    }
                }
            }
            else
            {
                switch (variation.kind)
                {
                case NORMAL:
                    variation.icon = register.registerIcon(modName + ":" + variation.texture);
                    break;
                case TOPSIDE:
                    variation.icon = register.registerIcon(modName + ":" + variation.texture + "-side");
                    variation.iconTop = register.registerIcon(modName + ":" + variation.texture + "-top");
                    break;
                case TOPBOTSIDE:
                    variation.icon = register.registerIcon(modName + ":" + variation.texture + "-side");
                    variation.iconTop = register.registerIcon(modName + ":" + variation.texture + "-top");
                    variation.iconBot = register.registerIcon(modName + ":" + variation.texture + "-bottom");
                    break;
                case CTM3:
                    CarvableVariationCTM ctm = new CarvableVariationCTM();
                    ctm.seams[0] = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-ctm1"), 4, 4);
                    ctm.seams[1] = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-ctm2"), 4, 4);
                    ctm.seams[2] = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-ctm3"), 4, 4);
                    variation.ctm = ctm;
                    break;
                case CTMV:
                    variation.seamsCtmVert = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-ctmv"), 2, 2);
                    variation.iconTop = register.registerIcon(modName + ":" + variation.texture + "-top");
                    break;
                case CTMH:
                    variation.seamsCtmVert = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-ctmh"), 2, 2);
                    variation.iconTop = register.registerIcon(modName + ":" + variation.texture + "-top");
                    break;
                case V9:
                    variation.variations9 = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-v9"), 3, 3);
                    break;
                case V4:
                    variation.variations9 = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-v4"), 2, 2);
                    break;
                case CTMX:
                    variation.icon = register.registerIcon(modName + ":" + variation.texture);
                    variation.submap = new TextureSubmap(register.registerIcon(modName + ":" + variation.texture + "-ctm"), 4, 4);
                    variation.submapSmall = new TextureSubmap(variation.icon, 2, 2);
                    break;
                }
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void registerSubBlocks(Block block, CreativeTabs tabs, List list)
    {
        for (CarvableVariation variation : variations)
        {
            list.add(new ItemStack(block, 1, variation.metadata));
        }
    }

    public void setChiselBlockName(String name)
    {
        blockName = name;
    }

    public static Set<Block> getChiselBlockSet()
    {
        HashSet<Block> tools = new HashSet<Block>();
        tools.addAll(CarvableHelper.chiselBlocks);
        return tools;
    }
}
