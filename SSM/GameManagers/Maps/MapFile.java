package SSM.GameManagers.Maps;

import SSM.Commands.CommandLoadWorld;
import SSM.GameManagers.GameManager;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.material.Wool;
import org.bukkit.util.Vector;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class MapFile {

    protected String name = "N/A";
    protected String created_by = "N/A";
    protected File map_directory = null;
    private List<Location> respawn_points = new ArrayList<Location>();
    protected List<Player> voted = new ArrayList<Player>();
    public World copy_world = null;
    private Vector boundary_min = null;
    private Vector boundary_max = null;

    public MapFile(File file) {
        map_directory = file;
        name = file.getName().replace("_", " ");
        name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        File name_file = new File(map_directory.getPath() + "/map_name.txt");
        File created_by_file = new File(map_directory.getPath() + "/created_by.txt");
        try {
            if (name_file.exists() && name_file.length() != 0) {
                name = Files.readString(name_file.toPath());
            }
            else {
                if(!name_file.exists()) {
                    name_file.createNewFile();
                }
                Bukkit.broadcastMessage(ChatColor.YELLOW +
                        "Name File for: " + name + " was empty.");
            }
            if(created_by_file.exists() && created_by_file.length() != 0) {
                created_by = Files.readString(created_by_file.toPath());
            }
            else {
                if(!created_by_file.exists()) {
                    created_by_file.createNewFile();
                }
                Bukkit.broadcastMessage(ChatColor.YELLOW +
                        "Created By File for: " + name + " was empty.");
            }
        } catch (Exception e) {
            Bukkit.broadcastMessage(ChatColor.RED + "Failed to read map files");
        }
    }

    public void createWorld() {
        respawn_points.clear();
        try {
            File copy_directory = new File("maps/_Copies/" + map_directory.getName());
            World world = Bukkit.getWorld(copy_directory.getPath());
            if (world != null) {
                Bukkit.unloadWorld(world, false);
            }
            if (copy_directory.exists() && copy_directory.isDirectory()) {
                FileUtils.deleteDirectory(copy_directory);
            }
            FileUtils.copyDirectory(map_directory, copy_directory);
            ArrayList<String> ignore = new ArrayList<String>(Arrays.asList("uid.dat", "session.dat"));
            File uid = new File(copy_directory.getPath() + "/uid.dat");
            File session = new File(copy_directory.getPath() + "/session.dat");
            if (uid.exists()) {
                uid.delete();
            }
            if (session.exists()) {
                session.delete();
            }
            copy_world = CommandLoadWorld.loadWorld(copy_directory.getPath());
        } catch (Exception e) {
            Bukkit.broadcastMessage(ChatColor.RED + "Failed to load world.");
        }
        // Parse map for objects
        int size = 100;
        for (int x = -size; x <= size; x++) {
            for (int y = -size; y <= size; y++) {
                for (int z = -size; z <= size; z++) {
                    Block parsed = copy_world.getBlockAt(x, y, z);
                    if (isRespawnPoint(parsed)) {
                        respawn_points.add(parsed.getLocation());
                        parsed.getRelative(0, 1, 0).setType(Material.AIR);
                        parsed.setType(Material.AIR);
                    }
                    if (isBoundaryPoint(parsed)) {
                        addBoundaryPoint(parsed.getLocation());
                        parsed.getRelative(0, 1, 0).setType(Material.AIR);
                        parsed.setType(Material.AIR);
                    }
                    if (isCenterPoint(parsed)) {
                        copy_world.setSpawnLocation(parsed.getX(), parsed.getY(), parsed.getZ());
                        parsed.getRelative(0, 1, 0).setType(Material.AIR);
                        parsed.setType(Material.AIR);
                    }
                }
            }
        }
    }

    public void deleteWorld() {
        for (Player player : copy_world.getPlayers()) {
            player.teleport(GameManager.lobby_world.getSpawnLocation());
        }
        Bukkit.unloadWorld(copy_world, true);
    }

    public List<Location> getRespawnPoints() {
        return respawn_points;
    }

    public String getName() {
        return name;
    }

    public String getCreatedBy() {
        return created_by;
    }

    public World getCopyWorld() {
        return copy_world;
    }

    public List<Player> getVoted() {
        for (Player player : voted) {
            if (!player.isOnline()) {
                voted.remove(player);
            }
        }
        return voted;
    }

    public void clearVoted() {
        voted.clear();
    }

    public void addBoundaryPoint(Location location) {
        if(boundary_min == null) {
            boundary_min = location.toVector();
        }
        if(boundary_max == null) {
            boundary_max = location.toVector();
        }
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        boundary_min.setX(Math.min(x, boundary_min.getX()));
        boundary_min.setY(Math.min(y, boundary_min.getY()));
        boundary_min.setZ(Math.min(z, boundary_min.getZ()));
        boundary_max.setX(Math.max(x, boundary_max.getX()));
        boundary_max.setY(Math.max(y, boundary_max.getY()));
        boundary_max.setZ(Math.max(z, boundary_max.getZ()));
    }

    public boolean isOutOfBounds(Entity entity) {
        if(boundary_min == null || boundary_max == null) {
            return false;
        }
        if(entity == null) {
            return false;
        }
        if(copy_world == null || !entity.getWorld().equals(copy_world)) {
            return false;
        }
        Vector vector = entity.getLocation().toVector();
        if(vector.getX() < boundary_min.getX() || vector.getX() > boundary_max.getX()) {
            return true;
        }
        if(vector.getY() < boundary_min.getY() || vector.getY() > boundary_max.getY()) {
            return true;
        }
        if(vector.getZ() < boundary_min.getZ() || vector.getZ() > boundary_max.getZ()) {
            return true;
        }
        return false;
    }

    public boolean isRespawnPoint(Block check) {
        if (check.getType() != Material.WOOL) {
            return false;
        }
        Wool wool = (Wool) check.getState().getData();
        if (wool.getColor() != DyeColor.GREEN) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.GOLD_PLATE);
    }

    public boolean isBoundaryPoint(Block check) {
        if (check.getType() != Material.WOOL) {
            return false;
        }
        Wool wool = (Wool) check.getState().getData();
        if (wool.getColor() != DyeColor.RED) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.GOLD_PLATE);
    }

    public boolean isCenterPoint(Block check) {
        if (check.getType() != Material.WOOL) {
            return false;
        }
        Wool wool = (Wool) check.getState().getData();
        if (wool.getColor() != DyeColor.WHITE) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.GOLD_PLATE);
    }

    public String toString() {
        return ChatColor.GREEN + "Map - " + ChatColor.RESET + ChatColor.BOLD + getName() +
                ChatColor.GRAY + " created by " + ChatColor.RESET + ChatColor.BOLD + getCreatedBy();
    }
}
