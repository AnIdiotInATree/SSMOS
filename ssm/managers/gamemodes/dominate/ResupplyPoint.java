package ssm.managers.gamemodes.dominate;

import org.bukkit.Location;

public class ResupplyPoint {
    private Location pointLocation;
    private int ticksElapsedSincePickup = 0;
    private boolean resupplyActive = false;

    public ResupplyPoint(Location pointLocation)
    {
        this.pointLocation = pointLocation;
    }

    public int getTicksElapsedSincePickup()
    {
        return ticksElapsedSincePickup;
    }
}
