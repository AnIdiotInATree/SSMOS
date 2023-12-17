package ssm.managers.gamemodes.dominate;

import net.minecraft.server.v1_8_R3.PacketPlayInChat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import ssm.events.PlayerLostLifeEvent;
import ssm.events.SmashDamageEvent;
import ssm.managers.DamageManager;
import ssm.managers.GameManager;
import ssm.managers.TeamManager;
import ssm.managers.TeamManager.TeamColor;
import ssm.managers.gamemodes.SmashGamemode;
import ssm.managers.gamestate.GameState;
import ssm.managers.maps.DominateMap;
import ssm.managers.maps.GameMap;
import ssm.managers.smashscoreboard.SmashScoreboard;
import ssm.managers.smashteam.SmashTeam;
import ssm.utilities.ServerMessageType;
import ssm.utilities.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DominateGamemode extends SmashGamemode {

    private List<DominateTeam> teams = new ArrayList<>();
    private DominateTeam[] team_deaths = new DominateTeam[2];
    private HashMap<Player, Player> preferred_teammate = new HashMap<Player, Player>();

    protected String maps_folder_name = "Dominate";

    private DominateMap current_map;
    private int redSpawnpointIndex = 0;
    private int blueSpawnpointIndex = 0;

    public DominateGamemode() {
        super();
        redSpawnpointIndex = 0;
        blueSpawnpointIndex = 0;
        this.name = "SSM Dominate";
        this.short_name = "DOM";
        this.description = new String[] {
                "Capture Beacons for Points",
                "+300 Points for Emerald Powerups",
                "+50 Points for Kills",
                "First team to 15000 Points wins"
        };
        this.players_to_start = 2;
        this.max_players = 10;
    }

    public void updateAllowedMaps() {
        try {
            allowed_maps.clear();
            File maps_folder = new File("maps");
            if (!maps_folder.exists()) {
                if (!maps_folder.mkdir()) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Failed to make Main Maps Folder");
                }
            }
            File gamemode_maps_folder = new File("maps/" + maps_folder_name);
            if (!gamemode_maps_folder.exists()) {
                if (!gamemode_maps_folder.mkdir()) {
                    Bukkit.broadcastMessage(ChatColor.RED + "Failed to make Maps Folder: " + maps_folder_name);
                }
            }
            File[] files = gamemode_maps_folder.listFiles();
            if(files == null) {
                return;
            }
            for (File file : files) {
                if (!file.isDirectory()) {
                    continue;
                }
                File region_directory = new File(file.getPath() + "/region");
                if (!region_directory.exists()) {
                    continue;
                }
                allowed_maps.add(new DominateMap(file));
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    // Even distribution of players across the teams
    @Override
    public void setPlayerLives(HashMap<Player, Integer> lives) {
        // Create teams
        for(SmashTeam team : teams) {
            TeamManager.removeTeam(team);
        }
        teams.clear();

        teams.add(TeamManager.createDominateTeam("Red", ChatColor.RED));
        teams.add(TeamManager.createDominateTeam("Blue", ChatColor.BLUE));

        // alternate adding players to team :3
        int playerNumber = 0;
        for(Player player : server.players) {
            if (server.isSpectator(player)) {
                continue;
            }
            lives.put(player, 1337);
            SmashTeam team = teams.get(playerNumber%2);
            team.addPlayer(player);
            playerNumber++;
        }

        current_map = (DominateMap) server.getGameMap();
        server.getScoreboard().buildScoreboard();
    }

    @Override
    public Location getRandomRespawnPoint(Player player) { // terrible awful
        SmashTeam playerTeam = TeamManager.getPlayerTeam(player);
        if(playerTeam.getName().equals("Red"))
        {
            int spawnIndex = redSpawnpointIndex;
            redSpawnpointIndex++;
            if(spawnIndex > (current_map.getRedSpawnpoints().size()-1))
            {
                spawnIndex = 0;
                redSpawnpointIndex = 0;
            }
            return current_map.getRedSpawnpoints().get(spawnIndex);
        }
        else if(playerTeam.getName().equals("Blue"))
        {
            int spawnIndex = blueSpawnpointIndex;
            blueSpawnpointIndex++;
            if(spawnIndex > (current_map.getBlueSpawnpoints().size()-1))
            {
                spawnIndex = 0;
                blueSpawnpointIndex = 0;
            }
            return current_map.getBlueSpawnpoints().get(spawnIndex);
        }
        return server.getGameMap().getWorld().getSpawnLocation();
    }

    public List<String> getLivesScoreboard() {
        List<String> scoreboard_string = new ArrayList<String>();

        for(CapturePoint point : current_map.getCapturePoints())
        {
            scoreboard_string.add(point.getName());
        }
        scoreboard_string.add("   ");
        scoreboard_string.add(String.valueOf(teams.get(1).getPoints()));
        scoreboard_string.add(ChatColor.AQUA + "Blue Team");
        scoreboard_string.add("  ");
        scoreboard_string.add(teams.get(0).getPoints() + " ");
        scoreboard_string.add(ChatColor.RED + "Red Team");
        scoreboard_string.add(" ");
        scoreboard_string.add("First to 15000");
        return scoreboard_string;
    }

    @Override
    public boolean isGameEnded(HashMap<Player, Integer> lives) {
        int teams_left = 0;
        for(SmashTeam team : teams) {
            if(team.hasAliveMembers()) {
                teams_left++;
            }
        }
        return (teams_left <= 1);
    }

    @Override
    public String getFirstPlaceString() {
        for(SmashTeam team : teams) {
            if(team.hasAliveMembers()) {
                return team.getColor() + team.getName();
            }
        }
        return null;
    }

    @Override
    public String getSecondPlaceString() {
        int found_alive = 0;
        for(SmashTeam team : teams) {
            if(team.hasAliveMembers()) {
                found_alive++;
            }
            if(found_alive == 2) {
                return team.getColor() + team.getName();
            }
        }
        if(team_deaths[0] != null) {
            return team_deaths[0].getColor() + team_deaths[0].getName();
        }
        return null;
    }

    @Override
    public String getThirdPlaceString() {
        int found_alive = 0;
        for(SmashTeam team : teams) {
            if(team.hasAliveMembers()) {
                found_alive++;
            }
            if(found_alive == 3) {
                return team.getColor() + team.getName();
            }
        }
        if(team_deaths[1] != null) {
            return team_deaths[1].getColor() + team_deaths[1].getName();
        }
        return null;
    }

    @EventHandler
    public void onPlayerLostLife(PlayerLostLifeEvent e) {
        if(server == null || !server.equals(GameManager.getPlayerServer(e.getPlayer()))) {
            return;
        }
        DominateTeam team = (DominateTeam) TeamManager.getPlayerTeam(e.getPlayer());
        if(team == null) {
            return;
        }
        if(!team.hasAliveMembers()) {
            team_deaths[1] = team_deaths[0];
            team_deaths[0] = team;
        }

        SmashDamageEvent record = DamageManager.getLastDamageEvent(e.getPlayer());
        if(record == null)
        {
            return;
        }
        LivingEntity damager = record.getDamager();
        if(damager.getType() != EntityType.PLAYER)
        {
            return;
        }

        if(teams.get(0).equals(team))
        {
            teams.get(1).addPoints(50);
        }
        if(teams.get(1).equals(team))
        {
            teams.get(0).addPoints(50);
        }
    }
}