package info.jbcs.minecraft.chisel.item;

import info.jbcs.minecraft.chisel.Chisel;
import info.jbcs.minecraft.chisel.PacketHandler;
import info.jbcs.minecraft.chisel.api.ChiselMode;
import info.jbcs.minecraft.chisel.api.IChiselMode;
import info.jbcs.minecraft.chisel.carving.CarvableHelper;
import info.jbcs.minecraft.chisel.carving.Carving;
import info.jbcs.minecraft.chisel.carving.CarvingVariation;
import info.jbcs.minecraft.chisel.client.GeneralChiselClient;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;

import org.apache.commons.lang3.ArrayUtils;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

public class ItemChisel extends ItemTool implements IChiselMode
{
    public static Carving carving = Carving.chisel;

    public ItemChisel(Carving c)
    {
        super(1, ToolMaterial.IRON, CarvableHelper.getChiselBlockSet());

        setMaxStackSize(1);
        setMaxDamage(500);
        setUnlocalizedName("chisel");

        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack itemstack, World world, EntityPlayer entityplayer)
    {
        entityplayer.openGui(Chisel.instance, 0, world, 0, 0, 0);

        return itemstack;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase breaker)
    {
        return true; // prevent damaging
    }
    
    @Override
    public boolean hitEntity(ItemStack stack, EntityLivingBase player, EntityLivingBase hit)
    {
        return true; // prevent damaging
    }

    @SubscribeEvent
    public void onLeftClickBlock(PlayerInteractEvent event)
    {
        ItemStack held = event.entityPlayer.getCurrentEquippedItem();
        int slot = event.entityPlayer.inventory.currentItem;
        if (event.action == Action.LEFT_CLICK_BLOCK && held != null && held.getItem() == this)
        {
            int x = event.x, y = event.y, z = event.z;
            Block block = event.world.getBlock(x, y, z);
            int metadata = event.world.getBlockMetadata(x, y, z);
            CarvingVariation[] variations = carving.getVariations(block, metadata);

            if (variations != null)
            {
                ItemStack target = getTarget(held);

                if (target != null)
                {
                    for (CarvingVariation v : variations)
                    {
                        if (v.block == Block.getBlockFromItem(target.getItem()) && v.meta == target.getItemDamage())
                        {
                            setVariation(event.entityPlayer, event.world, x, y, z, v);
                        }
                    }
                }
                else
                {
                    for (int i = 0; i < variations.length; i++)
                    {
                        CarvingVariation v = variations[i];
                        if (v.block == block && v.meta == metadata)
                        {
                            variations = ArrayUtils.remove(variations, i--);
                        }
                    }

                    int index = event.world.rand.nextInt(variations.length);
                    CarvingVariation newVar = variations[index];
                    setVariation(event.entityPlayer, event.world, x, y, z, newVar);
                    event.entityPlayer.inventory.currentItem = slot;
                }
            }
        }
    }

    /**
     * Assumes that the player is holding a chisel
     */
    private void setVariation(EntityPlayer player, World world, int x, int y, int z, CarvingVariation v)
    {
        PacketHandler.INSTANCE.sendTo(new PacketPlaySound(x, y, z, v), (EntityPlayerMP) player);
        world.setBlock(x, y, z, v.block);
        world.setBlockMetadataWithNotify(x, y, z, v.meta, 3);
        player.getCurrentEquippedItem().damageItem(1, player);
        if (player.getCurrentEquippedItem().stackSize <= 0)
        {
            player.destroyCurrentEquippedItem();
        }
    }

    public ItemStack getTarget(ItemStack chisel)
    {
        return chisel.hasTagCompound() ? ItemStack.loadItemStackFromNBT(chisel.stackTagCompound.getCompoundTag("chiselTarget")) : null;
    }

    @Override
    public ChiselMode getChiselMode(ItemStack itemStack)
    {
        // TODO
        return ChiselMode.SINGLE;
    }

    public static class PacketPlaySound implements IMessage
    {
        public PacketPlaySound()
        {}

        private int x, y, z;
        private int block;
        private byte meta;

        public PacketPlaySound(int x, int y, int z, CarvingVariation v)
        {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = Block.getIdFromBlock(v.block);
            this.meta = (byte) v.meta;
        }

        @Override
        public void toBytes(ByteBuf buf)
        {
            buf.writeInt(x);
            buf.writeInt(y);
            buf.writeInt(z);
            buf.writeInt(block);
            buf.writeByte(meta);
        }

        @Override
        public void fromBytes(ByteBuf buf)
        {
            x = buf.readInt();
            y = buf.readInt();
            z = buf.readInt();
            block = buf.readInt();
            meta = (byte) buf.readByte();
        }
    }

    public static class PlaySoundHandler implements IMessageHandler<PacketPlaySound, IMessage>
    {
        @Override
        public IMessage onMessage(PacketPlaySound message, MessageContext ctx)
        {
            String sound = ItemChisel.carving.getVariationSound(Block.getBlockById(message.block), message.meta);
            GeneralChiselClient.spawnChiselEffect(message.x, message.y, message.z, sound);
            return null;
        }
    }
}
