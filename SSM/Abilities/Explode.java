package SSM.Abilities;

import SSM.Events.SmashDamageEvent;
import SSM.GameManagers.DisguiseManager;
import SSM.GameManagers.Disguises.CreeperDisguise;
import SSM.GameManagers.Disguises.Disguise;
import SSM.GameManagers.OwnerEvents.OwnerRightClickEvent;
import SSM.GameManagers.OwnerEvents.OwnerTakeSmashDamageEvent;
import SSM.GameManagers.OwnerEvents.OwnerToggleSneakEvent;
import SSM.Projectiles.IronHookProjectile;
import SSM.Utilities.DamageUtil;
import SSM.Utilities.ServerMessageType;
import SSM.Utilities.Utils;
import SSM.Utilities.VelocityUtil;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

public class Explode extends Ability implements OwnerRightClickEvent, OwnerToggleSneakEvent, OwnerTakeSmashDamageEvent {

    /*
     - - Explode
      - - Radius Normal
        - '8'
      - - Radius Smash
        - '24'
      - - Damage Normal
        - '20'
      - - Damage Smash
        - '30'
      - - Spawn Removal Radius
        - '14'
      - - Knockback Magnitude
        - '2.5'
      - - Block Destroy Radius
        - '12'
      - - Block Regeneration Time
        - '20'
     */

    private int explode_task = -1;
    private long start_time_ms = 0;

    public Explode() {
        super();
        this.name = "Explode";
        this.cooldownTime = 8;
        this.useMessage = "You are charging";
        this.description = new String[] {
                ChatColor.RESET + "You freeze in location and charge up",
                ChatColor.RESET + "for 1.5 seconds. Then you explode!",
                ChatColor.RESET + "You are sent flying in the direction",
                ChatColor.RESET + "you are looking, while opponents take",
                ChatColor.RESET + "large damage and knockback.",

        };
    }

    public void onOwnerRightClick(PlayerInteractEvent e) {
        checkAndActivate();
    }

    public void activate() {
        disguiseLarge();
        start_time_ms = System.currentTimeMillis();
        explode_task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - start_time_ms;
                VelocityUtil.setVelocity(owner, new Vector(0, 0, 0));
                float volume = (float) 0.5f + elapsed / 1000f;
                owner.getWorld().playSound(owner.getLocation(), Sound.CREEPER_HISS, volume, volume);
                disguiseLarge();
                owner.setExp((float) Math.min(0.999f, elapsed / 1500.0));
                if(elapsed < 1500) {
                    return;
                }
                owner.setExp(0);
                disguiseNormal();
                Utils.playParticle(EnumParticle.EXPLOSION_HUGE, owner.getLocation(),
                        0, 0, 0, 0, 1, 96, owner.getWorld().getPlayers());
                owner.getWorld().playSound(owner.getLocation(), Sound.EXPLODE, 2f, 1f);
                double maxRange = 8;
                double damage = 20;
                for(LivingEntity living : Utils.getInRadius(owner.getLocation(), maxRange).keySet()) {
                    if(living.equals(owner)) {
                        continue;
                    }
                    double dist = owner.getLocation().distance(living.getLocation());
                    if(!DamageUtil.canDamage(owner, living, damage)) {
                        continue;
                    }
                    double scale = 0.1 + 0.9 * ((maxRange - dist) / maxRange);
                    SmashDamageEvent smashDamageEvent = new SmashDamageEvent(living, owner, damage * scale);
                    smashDamageEvent.setDamage(smashDamageEvent.getDamage() * 0.75);
                    smashDamageEvent.multiplyKnockback(2.5);
                    smashDamageEvent.setIgnoreDamageDelay(true);
                    smashDamageEvent.setReason(name);
                    smashDamageEvent.callEvent();
                }
                VelocityUtil.setVelocity(owner, 1.8, 0.2, 1.4, true);
                Utils.sendAttributeMessage("You used", name, owner, ServerMessageType.SKILL);
                Bukkit.getScheduler().cancelTask(explode_task);
            }
        }, 0L, 0L);
    }

    @Override
    public void onOwnerTakeSmashDamageEvent(SmashDamageEvent e) {
        if(!isExploding()) {
            return;
        }
        e.setDamage(e.getDamage() * 0.75);
    }

    public void disguiseNormal() {
        Disguise disguise = DisguiseManager.disguises.get(owner);
        if(disguise == null || !(disguise instanceof CreeperDisguise)) {
            return;
        }
        CreeperDisguise creeperDisguise = (CreeperDisguise) disguise;
        creeperDisguise.setFuseState((byte) -1);
    }

    public void disguiseLarge() {
        Disguise disguise = DisguiseManager.disguises.get(owner);
        if(disguise == null || !(disguise instanceof CreeperDisguise)) {
            return;
        }
        CreeperDisguise creeperDisguise = (CreeperDisguise) disguise;
        creeperDisguise.setFuseState((byte) 1);
    }

    @Override
    public void onOwnerToggleSneak(PlayerToggleSneakEvent e) {
        if(!isExploding()) {
            return;
        }
        Bukkit.getScheduler().cancelTask(explode_task);
        disguiseNormal();
        Utils.sendAttributeMessage("You cancelled", name, owner, ServerMessageType.SKILL);
    }

    private boolean isExploding() {
        boolean is_running = Bukkit.getScheduler().isQueued(explode_task);
        is_running = is_running || Bukkit.getScheduler().isCurrentlyRunning(explode_task);
        return is_running;
    }

}