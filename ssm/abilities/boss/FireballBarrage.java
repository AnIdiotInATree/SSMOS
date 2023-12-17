package ssm.abilities.boss;

import net.minecraft.server.v1_8_R3.EntityLargeFireball;
import net.minecraft.server.v1_8_R3.EntitySmallFireball;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLargeFireball;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftSmallFireball;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import ssm.abilities.Ability;
import ssm.events.SmashDamageEvent;
import ssm.managers.ownerevents.OwnerDealSmashDamageEvent;
import ssm.managers.ownerevents.OwnerRightClickEvent;
import ssm.utilities.Utils;
import ssm.utilities.VelocityUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class FireballBarrage extends Ability implements OwnerRightClickEvent, OwnerDealSmashDamageEvent {

    protected double velocity = 0.2;
    protected double velocity_radius = 8;
    protected double damage = 8;
    private int task = -1;
    private ArrayList<LargeFireball> fireballs = new ArrayList<>();
    private ArrayList<LargeFireball> fireballsForDeletion = new ArrayList<>();

    public FireballBarrage() {
        super();
        this.name = "Fireball Barrage";
        this.cooldownTime = 6;
        this.description = new String[]{
                ChatColor.RESET + "Release a powerful ball of magma which explodes",
                ChatColor.RESET + "on impact, dealing damage and knockback.",
                ChatColor.RESET + "",
                ChatColor.RESET + "You receive strong knockback when you shoot it.",
                ChatColor.RESET + "Use this knockback to get back onto the map!",
        };
    }

    public void onOwnerRightClick(PlayerInteractEvent e) {
        checkAndActivate();
    }

    public void activate() {
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int ticks = 0;
            @Override
            public void run() {

                if(ticks % 20 == 0 && fireballs.size() < 5)
                {
                    LargeFireball ball = owner.launchProjectile(LargeFireball.class);
                    ball.setShooter(owner);
                    ball.setIsIncendiary(false);
                    ball.setYield(0);
                    ball.setBounce(false);
                    ball.teleport(owner.getEyeLocation().add(owner.getLocation().getDirection().multiply(1)));

                    fireballs.add(ball);

                    Vector dir = owner.getLocation().getDirection().multiply(velocity);
                    EntityLargeFireball eFireball = ((CraftLargeFireball) ball).getHandle();
                    eFireball.dirX = dir.getX();
                    eFireball.dirY = dir.getY();
                    eFireball.dirZ = dir.getZ();

                    ball.setMetadata("Barrage Fireball", new FixedMetadataValue(plugin, 1));
                    owner.getWorld().playSound(owner.getLocation(), Sound.GHAST_FIREBALL, 2f, 0.5f);
                }

                for(LargeFireball fireball : fireballs)
                {
                    if(!(fireball.isValid()))
                    {
                        fireballsForDeletion.add(fireball);
                        continue;
                    }

                    if(ticks % 10 == 0)
                    {
                        for(int i = 0; i < 3; i++)
                        {
                            Fireball smallFireball = owner.getWorld().spawn(fireball.getLocation(), SmallFireball.class);
                            smallFireball.setShooter(owner);
                            smallFireball.setIsIncendiary(false);
                            smallFireball.setBounce(false);

                            EntityLargeFireball eFireball = ((CraftLargeFireball) fireball).getHandle();
                            EntitySmallFireball eSmallFireball = ((CraftSmallFireball) smallFireball).getHandle();

                            double xOffset = eSmallFireball.dirX + -0.5 + 0.5*i;
                            double zOffset = eSmallFireball.dirZ + -0.5 + 0.5*i;
                            eSmallFireball.dirX = xOffset*1.5;
                            eSmallFireball.dirY = eFireball.dirY*1.5;
                            eSmallFireball.dirZ = zOffset*1.5;

                        }
                    }
                }

                for(LargeFireball fireball : fireballsForDeletion)
                {
                    fireballs.remove(fireball);
                    if(fireballs.size() == 0)
                    {
                        Bukkit.getScheduler().cancelTask(task);
                        return;
                    }
                }

                ticks++;
            }
        }, 0L, 0L);
    }

    @EventHandler
    public void Collide(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        List<MetadataValue> data = projectile.getMetadata("Magma Blast");
        if (data.size() <= 0) {
            return;
        }
        if (projectile.getShooter() == null) {
            return;
        }
        if (!projectile.getShooter().equals(owner)) {
            return;
        }
        projectile.remove();
        HashMap<LivingEntity, Double> hit_entities = Utils.getInRadius(projectile.getLocation().subtract(0, 1, 0), velocity_radius);
        for (LivingEntity livingEntity : hit_entities.keySet()) {
            if(!(livingEntity instanceof Player)) {
                continue;
            }
            if(livingEntity.equals(owner)) {
                continue;
            }
            Player player = (Player) livingEntity;
            double range = hit_entities.get(livingEntity);
            if (range > 0.8) {
                range = 1;
            }
            SmashDamageEvent smashDamageEvent = new SmashDamageEvent(player, owner, range * damage);
            smashDamageEvent.multiplyKnockback(0);
            smashDamageEvent.setIgnoreDamageDelay(true);
            smashDamageEvent.setReason(name);
            smashDamageEvent.callEvent();
            Vector difference = player.getEyeLocation().toVector().subtract(projectile.getLocation().add(0, -0.5, 0).toVector());
            difference.normalize();
            VelocityUtil.setVelocity(player, difference, 1 + 2 * range, false, 0, 0.2 + 0.4 * range, 1.2, true);
        }
        Utils.playParticle(EnumParticle.LAVA, projectile.getLocation(),
                0.1f, 0.1f, 0.1f, 0.1f, 50, 96, projectile.getWorld().getPlayers());
    }

    // Change direct hit to not have a sound
    @Override
    public void onOwnerDealSmashDamageEvent(SmashDamageEvent e) {
        if(e.getProjectile() == null) {
            return;
        }
        Projectile projectile = e.getProjectile();
        List<MetadataValue> data = projectile.getMetadata("Magma Blast");
        if (data.size() <= 0) {
            return;
        }
        if (projectile.getShooter() == null) {
            return;
        }
        if (!projectile.getShooter().equals(owner)) {
            return;
        }
        e.setDamageCause(EntityDamageEvent.DamageCause.CUSTOM);
        e.setReason(name);
    }

}