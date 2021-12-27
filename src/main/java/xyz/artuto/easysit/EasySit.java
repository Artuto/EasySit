package xyz.artuto.easysit;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.Objects;

import static org.bukkit.persistence.PersistentDataType.BYTE;

public final class EasySit extends JavaPlugin implements Listener
{
    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if(event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;

        Block clickedBlock = event.getClickedBlock();
        if(clickedBlock == null || !Tag.STAIRS.isTagged(clickedBlock.getType()))
            return;

        Player player = event.getPlayer();
        if(player.isSneaking() || player.isInsideVehicle())
            return;

        if(!clickedBlock.getRelative(BlockFace.UP).isPassable())
            return;

        if(!checkStair(clickedBlock))
            return;

        if(event.hasItem() && event.getItem() != null)
        {
            ItemStack item = event.getItem();
            if(item.getType().isBlock() || item.getType() == Material.LAVA_BUCKET || item.getType() == Material.WATER_BUCKET)
                return;
        }

        spawnEntity(clickedBlock, player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityDismount(EntityDismountEvent event)
    {
        Entity dismounted = event.getDismounted();
        Entity rider = event.getEntity();

        if(rider.getType() != EntityType.PLAYER && dismounted.getType() != EntityType.ARMOR_STAND)
            return;

        if(dismounted.getPersistentDataContainer().getOrDefault(KEY, BYTE, (byte) 0) == (byte) 1)
        {
            dismounted.remove();
            rider.teleport(rider.getLocation().clone().add(0, 1, 0));
        }
    }

    private void spawnEntity(Block block, Player player)
    {
        Location location = centerLocation(block.getLocation());
        World world = location.getWorld();

        ArmorStand entity = world.spawn(location, ArmorStand.class, armorStand ->
        {
            armorStand.setMarker(true);
            armorStand.setVisible(false);
            armorStand.getPersistentDataContainer().set(KEY, BYTE, (byte) 1);
        });
        entity.addPassenger(player);
    }

    private boolean checkStair(Block clickedBlock)
    {
        if(clickedBlock.getBlockData() instanceof Stairs stair)
            return !stair.isWaterlogged() && stair.getHalf() == Bisected.Half.BOTTOM;

        return false;
    }

    private Location centerLocation(Location original)
    {
        Location location = original.clone();
        location.setY(original.getBlockY() + 0.5);
        location.add(0.5, 0, 0.5);
        return location;
    }

    private final NamespacedKey KEY = Objects.requireNonNull(NamespacedKey.fromString("chair", this));
}
