package info.jbcs.minecraft.chisel;

import info.jbcs.minecraft.chisel.block.BlockCarvable;
import info.jbcs.minecraft.chisel.carving.Carving;
import info.jbcs.minecraft.chisel.client.gui.GuiChisel;
import info.jbcs.minecraft.chisel.entity.EntityBallOMoss;
import info.jbcs.minecraft.chisel.entity.EntityCloudInABottle;
import info.jbcs.minecraft.chisel.entity.EntitySmashingRock;
import info.jbcs.minecraft.chisel.inventory.ContainerChisel;
import info.jbcs.minecraft.chisel.inventory.InventoryChiselSelection;
import info.jbcs.minecraft.chisel.item.ItemBallOMoss;
import info.jbcs.minecraft.chisel.item.ItemChisel;
import info.jbcs.minecraft.chisel.item.ItemCloudInABottle;
import info.jbcs.minecraft.chisel.item.ItemSmashingRock;
import info.jbcs.minecraft.chisel.world.GeneratorLimestone;
import info.jbcs.minecraft.chisel.world.GeneratorMarble;
import info.jbcs.minecraft.utilities.General;

import java.io.File;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLInterModComms;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent;
import cpw.mods.fml.common.event.FMLMissingMappingsEvent.MissingMapping;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.IGuiHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.GameRegistry.Type;

@Mod(modid = Chisel.MOD_ID, name = Chisel.MOD_NAME, version = "@MOD_VERSION@", guiFactory = "info.jbcs.minecraft.chisel.client.gui.GuiFactory"/*
                                                                                                                                               * ,
                                                                                                                                               * dependencies
                                                                                                                                               * =
                                                                                                                                               * "after:ForgeMicroblock;"
                                                                                                                                               */)
public class Chisel
{
    public static final String MOD_ID = "chisel";
    public static final String MOD_NAME = "Chisel";

    public static ItemChisel chisel;
    // public static ItemChisel needle;
    public static ItemCloudInABottle itemCloudInABottle;
    public static ItemBallOMoss itemBallOMoss;
    public static ItemSmashingRock smashingRock;

    public static CreativeTabs tabChisel;

    public static boolean multipartLoaded = false;

    public static final BlockCarvable.SoundType soundHolystoneFootstep = new BlockCarvable.SoundType("holystone", 1.0f, 1.0f);
    public static final BlockCarvable.SoundType soundTempleFootstep = new BlockCarvable.SoundType("dig.stone", MOD_ID + ":step.templeblock", 1.0f, 1.0f);
    public static final BlockCarvable.SoundType soundMetalFootstep = new BlockCarvable.SoundType("metal", 1.0f, 1.0f);

    public static int RenderEldritchId;
    public static int RenderCTMId;
    public static int RenderCarpetId;

    @Instance(MOD_ID)
    public static Chisel instance;

    @SidedProxy(clientSide = "info.jbcs.minecraft.chisel.ClientProxy", serverSide = "info.jbcs.minecraft.chisel.CommonProxy")
    public static CommonProxy proxy;

    @EventHandler
    public void missingMapping(FMLMissingMappingsEvent event)
    {
        BlockNameConversion.init();

        for (MissingMapping m : event.get())
        {
            // This bug was introduced along with Chisel 1.5.2, and was fixed in 1.5.3.
            // Ice Stairs were called null.0-7 instead of other names, and Marble/Limestone stairs
            // did not exist.
            // This fixes the bug.
            if (m.name.startsWith("null.") && m.name.length() == 6 && m.type == Type.BLOCK)
            {
                m.warn();// (Action.WARN);
            }

            // Fix mapping of snakestoneSand, snakestoneStone, limestoneStairs, marbleStairs when
            // loading an old (1.5.4) save
            else if (m.type == Type.BLOCK)
            {
                final Block block = BlockNameConversion.findBlock(m.name);

                if (block != null)
                {
                    m.remap(block);
                    FMLLog.getLogger().info("Remapping block " + m.name + " to " + General.getName(block));
                }
                else
                    FMLLog.getLogger().warn("Block " + m.name + " could not get remapped.");
            }
            else if (m.type == Type.ITEM)
            {
                final Item item = BlockNameConversion.findItem(m.name);

                if (item != null)
                {
                    m.remap(item);
                    FMLLog.getLogger().info("Remapping item " + m.name + " to " + General.getName(item));

                }
                else
                    FMLLog.getLogger().warn("Item " + m.name + " could not get remapped.");
            }
        }
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        File configFile = event.getSuggestedConfigurationFile();
        Configurations.configExists = configFile.exists();
        Configurations.config = new Configuration(configFile);
        Configurations.config.load();
        Configurations.refreshConfig();

        tabChisel = new CreativeTabs("tabChisel")
        {
            @Override
            public Item getTabIconItem()
            {
                return chisel;
            }
        };

        chisel = (ItemChisel) new ItemChisel(Carving.chisel).setTextureName("chisel:chisel").setCreativeTab(CreativeTabs.tabTools);
        GameRegistry.registerItem(chisel, "chisel");

        if (Configurations.featureEnabled("cloud"))
        {
            itemCloudInABottle = (ItemCloudInABottle) new ItemCloudInABottle().setTextureName("Chisel:cloudinabottle").setCreativeTab(CreativeTabs.tabTools);
            EntityRegistry.registerModEntity(EntityCloudInABottle.class, "CloudInABottle", 1, this, 40, 1, true);
            GameRegistry.registerItem(itemCloudInABottle, "cloudinabottle");
        }

        if (Configurations.featureEnabled("ballOfMoss"))
        {
            itemBallOMoss = (ItemBallOMoss) new ItemBallOMoss().setTextureName("Chisel:ballomoss").setCreativeTab(CreativeTabs.tabTools);
            EntityRegistry.registerModEntity(EntityBallOMoss.class, "BallOMoss", 2, this, 40, 1, true);
            GameRegistry.registerItem(itemBallOMoss, "ballomoss");
        }

        if (Configurations.featureEnabled("smashingRock"))
        {
            smashingRock = (ItemSmashingRock) new ItemSmashingRock().setTextureName("Chisel:smashingrock").setCreativeTab(CreativeTabs.tabTools);
            EntityRegistry.registerModEntity(EntitySmashingRock.class, "SmashingRock", 2, this, 40, 1, true);
            GameRegistry.registerItem(smashingRock, "smashingrock");
        }
        ChiselBlocks.load();
        proxy.preInit();
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        Crafting.init();

        NetworkRegistry.INSTANCE.registerGuiHandler(this, new IGuiHandler()
        {
            @Override
            public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
            {
                return new ContainerChisel(player.inventory, new InventoryChiselSelection(null));
            }

            @Override
            public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z)
            {
                return new GuiChisel(player.inventory, new InventoryChiselSelection(null));
            }
        });

        GameRegistry.registerWorldGenerator(new GeneratorMarble(ChiselBlocks.blockMarble, 32, Configurations.marbleAmount), 1000);
        GameRegistry.registerWorldGenerator(new GeneratorLimestone(ChiselBlocks.blockLimestone, 32, Configurations.limestoneAmount), 1000);

        proxy.init();
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new ChiselLeftClick());
        FMLCommonHandler.instance().bus().register(instance);

        FMLInterModComms.sendMessage("Waila", "register", "info.jbcs.minecraft.chisel.Waila.register");
    }

    @EventHandler
    public void postInit(FMLPostInitializationEvent event)
    {
        Compatibility.init(event);
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event)
    {
        if (event.modID.equals("chisel"))
        {
            Configurations.refreshConfig();
        }
    }

}
