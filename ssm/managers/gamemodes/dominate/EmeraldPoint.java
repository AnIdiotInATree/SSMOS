package ssm.managers.gamemodes.dominate;

import org.bukkit.Location;

public class EmeraldPoint {
    private Location pointLocation;
    private int ticksElapsedSincePickup = 0;
    private boolean emeraldActive = false;

    public EmeraldPoint(Location pointLocation)
    {
        this.pointLocation = pointLocation;
    }

    public int getTicksElapsedSincePickup()
    {
        return ticksElapsedSincePickup;
    }
}
