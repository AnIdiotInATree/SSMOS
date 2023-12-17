package ssm.managers.gamemodes.dominate;

import org.bukkit.ChatColor;
import ssm.managers.smashteam.SmashTeam;

public class DominateTeam extends SmashTeam {
    private int points = 0;
    public DominateTeam(String team_name, ChatColor team_color) {
        super(team_name, team_color);
    }

    public void addPoints(int num)
    {
        int newPoints = points + num;
        if(newPoints > 15000)
        {
            newPoints = 15000;
        }
        points = newPoints;
    }

    public int getPoints()
    {
        return points;
    }
}
