package ssm.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ssm.managers.GameManager;
import ssm.managers.gamestate.GameState;
import ssm.managers.smashserver.SmashServer;

public class CommandStop implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String commandLabel, String[] args) {
        if(!commandSender.isOp()) {
            return true;
        }
        if (!(commandSender instanceof Player)) {
            Bukkit.shutdown();
            return true;
        }
        Player player = (Player) commandSender;
        SmashServer server = GameManager.getPlayerServer(player);
        if(server == null) {
            Bukkit.shutdown();
            return true;
        }
        server.stopGame();
        return true;
    }

}
