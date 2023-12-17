package ssm.managers.maps;

import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import ssm.Main;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public abstract class SmashMap implements Listener {

    protected String name = "N/A";
    protected File map_directory;
    protected World world = null;
    protected int permanent_chunk_load_radius = -1;
    protected int parse_chunk_radius = 0;
    protected List<Chunk> permanent_chunks = new ArrayList<Chunk>();
    private HashMap<Location, BlockFace> itemFrameRotations = new HashMap<>();

    public SmashMap(File original_directory) {
        Bukkit.getServer().getPluginManager().registerEvents(this, Main.getInstance());
        File copy_directory = new File("maps/_Copies/" + UUID.randomUUID());
        if (!copy_directory.exists()) {
            try {
                FileUtils.copyDirectory(original_directory, copy_directory);
                File uid = new File(copy_directory.getPath() + "/uid.dat");
                File session = new File(copy_directory.getPath() + "/session.dat");
                if (uid.exists()) {
                    uid.delete();
                }
                if (session.exists()) {
                    session.delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        map_directory = copy_directory;
    }

    public void createWorld() {
        World existing_world = Bukkit.getWorld(map_directory.getPath());
        if (existing_world != null) {
            return;
        }
        WorldCreator worldCreator = new WorldCreator(map_directory.getPath());
        world = worldCreator.createWorld();
        world.setAutoSave(false);
        Block center = world.getSpawnLocation().getBlock();
        for (int x = -permanent_chunk_load_radius; x <= permanent_chunk_load_radius; x++) {
            for (int z = -permanent_chunk_load_radius; z <= permanent_chunk_load_radius; z++) {
                permanent_chunks.add(world.getChunkAt(center.getChunk().getX() + x, center.getChunk().getZ() + z));
            }
        }
        for (int x = -parse_chunk_radius; x <= parse_chunk_radius; x++) {
            for (int z = -parse_chunk_radius; z <= parse_chunk_radius; z++) {
                // Use getChunkAt to temporarily load the chunk only
                // This seems to keep chunks loaded for about 30 seconds but not 100% sure
                world.getChunkAt(center.getChunk().getX() + x, center.getChunk().getZ() + z);
            }
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof ItemFrame) {
                ItemFrame frame = (ItemFrame) entity;
                Block parsed = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
                itemFrameRotations.put(parsed.getLocation(), frame.getFacing());
                if (parseBlock(parsed)) {
                    frame.remove();
                    parsed.getRelative(0, 1, 0).setType(Material.AIR);
                    parsed.setType(Material.AIR);
                }
            }

            if(entity instanceof ArmorStand)
            {
                ArmorStand armorStand = (ArmorStand) entity;
                Block parsed = armorStand.getLocation().getBlock().getRelative(0, -1, 0);
                if(parseBlock(parsed))
                {
                    armorStand.remove();
                    if(parsed.getType() == Material.GLASS)
                    {
                        parsed.setType(Material.AIR);
                    }
                    else
                    {
                        parsed.setType(Material.IRON_BLOCK);
                    }
                }
            }
        }
    }

    public Location getCenteredLocation(Location location)
    {
        if(itemFrameRotations.get(location) != null)
        {
            Location centeredLocation = location.clone().add(0.5,0,0.5);
            BlockFace facing = itemFrameRotations.get(location);

            if(facing == BlockFace.NORTH)
            {
                centeredLocation.setYaw(180f);
            }
            if(facing == BlockFace.EAST)
            {
                centeredLocation.setYaw(-90f);
            }
            if(facing == BlockFace.SOUTH)
            {
                centeredLocation.setYaw(0f);
            }
            if(facing == BlockFace.WEST)
            {
                centeredLocation.setYaw(90f);
            }

            return centeredLocation;
        }
        System.out.println(ChatColor.RED + "Failed to get direction");
        return location.add(0.5, 0, 0.5);
    }

    public void deleteWorld() {
        unloadWorld();
        try {
            FileUtils.deleteDirectory(map_directory);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unloadWorld() {
        if (world == null) {
            return;
        }
        for (Player player : world.getPlayers()) {
            player.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
        }
        Bukkit.unloadWorld(world, false);
    }

    public String getName() {
        return name;
    }

    public File getMapDirectory() {
        return map_directory;
    }

    public World getWorld() {
        return world;
    }

    public List<Location> getRespawnPoints() {
        if (world == null) {
            return null;
        }
        return List.of(world.getSpawnLocation());
    }

    public boolean isOutOfBounds(Entity entity) {
        return (entity.getLocation().getY() <= 0);
    }

    public boolean parseBlock(Block parsed) {
        return false;
    }

    @EventHandler
    public void chunkUnload(ChunkUnloadEvent e) {
        if(permanent_chunks.contains(e.getChunk())) {
            e.setCancelled(true);
        }
    }

}
