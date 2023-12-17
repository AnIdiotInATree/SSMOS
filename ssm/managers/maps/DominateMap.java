package ssm.managers.maps;

import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.material.Wool;
import ssm.managers.gamemodes.dominate.CapturePoint;
import ssm.managers.gamemodes.dominate.EmeraldPoint;
import ssm.managers.gamemodes.dominate.ResupplyPoint;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DominateMap extends GameMap {
    private ArrayList<Location> redSpawnpoints = new ArrayList<>();
    private ArrayList<Location> blueSpawnpoints = new ArrayList<>();
    private ArrayList<EmeraldPoint> emeraldPoints = new ArrayList<>();
    private ArrayList<ResupplyPoint> resupplyPoints = new ArrayList<>();
    private ArrayList<CapturePoint> capturePoints = new ArrayList<>();



    public DominateMap(File file) {
        super(file);
    }

    @Override
    public void createWorld() {
        redSpawnpoints.clear();
        blueSpawnpoints.clear();
        emeraldPoints.clear();
        resupplyPoints.clear();
        capturePoints.clear();
        super.createWorld();
    }

    @Override
    public boolean parseBlock(Block parsed) {
        super.parseBlock(parsed);
        if(isRedSpawnpoint(parsed)) {
            redSpawnpoints.add(getCenteredLocation(parsed.getLocation()));
            return true;
        }
        if(isBlueSpawnpoint(parsed)) {
            blueSpawnpoints.add(getCenteredLocation(parsed.getLocation()));
            return true;
        }
        if(isCapturePoint(parsed))
        {
            return true;
        }
        if(isEmeraldPoint(parsed))
        {
            emeraldPoints.add(new EmeraldPoint(parsed.getLocation()));
            return true;
        }
        if(isResupplyPoint(parsed))
        {
            resupplyPoints.add(new ResupplyPoint(parsed.getLocation()));
            return true;
        }
        return false;
    }

    public boolean isRedSpawnpoint(Block check)
    {
        if (check.getType() != Material.STAINED_CLAY) {
            return false;
        }
        byte data = check.getData();
        if (data != (byte) 14) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.GOLD_PLATE);
    }

    public boolean isBlueSpawnpoint(Block check)
    {
        if (check.getType() != Material.STAINED_CLAY) {
            return false;
        }
        byte data = check.getData();
        if (data != (byte) 11) {
            return false;
        }
        Block plate = check.getRelative(0, 1, 0);
        return (plate.getType() == Material.GOLD_PLATE);
    }

    public boolean isEmeraldPoint(Block check)
    {
        if (check.getType() != Material.EMERALD_BLOCK) {
            return false;
        }

        return true;
    }

    public boolean isResupplyPoint(Block check)
    {
        if (check.getType() != Material.GOLD_BLOCK) {
            return false;
        }

        return true;
    }

    public boolean isCapturePoint(Block check)
    {
        if (check.getType() != Material.GLASS) {
            return false;
        }

        Sign name = null;

        if(check.getRelative(-1, 0, 0).getType() == Material.WALL_SIGN)
        {
            name = (Sign) check.getRelative(-1,0,0).getState();
        }
        else if(check.getRelative(1, 0, 0).getType() == Material.WALL_SIGN)
        {
            name = (Sign) check.getRelative(1,0,0).getState();
        }
        else if(check.getRelative(0, 0, -1).getType() == Material.WALL_SIGN)
        {
            name = (Sign) check.getRelative(0,0,-1).getState();
        }
        else if(check.getRelative(0, 0, 1).getType() == Material.WALL_SIGN)
        {
            name = (Sign) check.getRelative(0,0,1).getState();
        }

        if(name == null)
        {
            return false;
        }

        if(capturePoints.size() < 5)
        {
            capturePoints.add(new CapturePoint(check.getLocation(), name.getLine(0)));
            Block lol = name.getBlock();
            lol.setType(Material.STONE);
            lol.setType(Material.AIR);
            return true;
        }
        else
        {
            System.out.println(ChatColor.RED + "Too many capture points. Failed at " + check.getX() + " " + check.getY() + " " + check.getZ());
        }
        return false;
    }

    public List<Location> getRedSpawnpoints()
    {
        return redSpawnpoints;
    }

    public List<Location> getBlueSpawnpoints()
    {
        return blueSpawnpoints;
    }
    public List<CapturePoint> getCapturePoints()
    {
        return capturePoints;
    }

    public List<EmeraldPoint> getEmeraldPoints()
    {
        return emeraldPoints;
    }

    public List<ResupplyPoint> getResupplyPoints()
    {
        return resupplyPoints;
    }

}
