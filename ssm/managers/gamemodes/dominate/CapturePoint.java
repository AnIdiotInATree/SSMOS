package ssm.managers.gamemodes.dominate;

import org.bukkit.Location;

public class CapturePoint {
    private Location pointLocation;
    private int redBlocksCaptured = 0;
    private int blueBlocksCaptured = 0;
    private int blocksUncaptured = 25;
    private String controlledBy = "None";
    private String capturingTeam = "None";
    private String name = "No name";

    public CapturePoint(Location pointLocation, String name)
    {
        this.pointLocation = pointLocation;
        if(name != null)
        {
            this.name = name;
        }
    }

    public String getName()
    {
        return name;
    }

}
