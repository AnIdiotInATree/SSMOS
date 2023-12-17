package ssm.abilities.boss;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import ssm.abilities.Ability;
import ssm.managers.DisguiseManager;
import ssm.managers.disguises.CreeperDisguise;
import ssm.managers.disguises.Disguise;
import ssm.managers.disguises.GhastDisguise;
import ssm.managers.ownerevents.OwnerRightClickEvent;
import ssm.projectiles.SulphurProjectile;

public class GhastMouth extends Ability implements OwnerRightClickEvent {

    public GhastMouth() {
        super();
        this.name = "Ghast Mouth";
        this.cooldownTime = 1;
        this.description = new String[] {
                ChatColor.RESET + "Throw a small bomb of sulphur.",
                ChatColor.RESET + "Explodes on contact with players,",
                ChatColor.RESET + "dealing some damage and knockback.",

        };
    }

    public void onOwnerRightClick(PlayerInteractEvent e) {
        checkAndActivate();
    }

    public void activate() {
        Disguise disguise = DisguiseManager.disguises.get(owner);
        if(!(disguise instanceof GhastDisguise)) {
            return;
        }
        GhastDisguise creeperDisguise = (GhastDisguise) disguise;
        creeperDisguise.setAttacking((byte) 1);
    }

}