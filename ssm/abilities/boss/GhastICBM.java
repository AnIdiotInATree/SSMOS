package ssm.abilities.boss;

import net.minecraft.server.v1_8_R3.EntityLargeFireball;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftLargeFireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.Vector;
import ssm.abilities.Ability;
import ssm.events.SmashDamageEvent;
import ssm.managers.DamageManager;
import ssm.managers.GameManager;
import ssm.managers.KitManager;
import ssm.managers.ownerevents.OwnerDealSmashDamageEvent;
import ssm.managers.ownerevents.OwnerRightClickEvent;
import ssm.managers.smashserver.SmashServer;
import ssm.utilities.ServerMessageType;
import ssm.utilities.Utils;
import ssm.utilities.VelocityUtil;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.HashMap;
import java.util.List;

public class GhastICBM extends Ability implements OwnerRightClickEvent, OwnerDealSmashDamageEvent {

    private int task = -1;

    protected double velocity = 0.1;
    protected double velocity_radius = 8;
    protected double range = 48;
    protected double damage = 8;

    public GhastICBM() {
        super();
        this.name = "Ghast ICBM";
        this.cooldownTime = 3;
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
        LargeFireball ball = owner.launchProjectile(LargeFireball.class);
        ball.setShooter(owner);
        ball.setIsIncendiary(false);
        ball.setYield(2);
        ball.setBounce(false);
        ball.teleport(owner.getEyeLocation().add(owner.getLocation().getDirection().multiply(1)));
        Vector dir = owner.getLocation().getDirection().multiply(velocity);
        EntityLargeFireball eFireball = ((CraftLargeFireball) ball).getHandle();
        eFireball.dirX = dir.getX();
        eFireball.dirY = dir.getY();
        eFireball.dirZ = dir.getZ();
        ball.setMetadata("Ghast ICBM", new FixedMetadataValue(plugin, 1));
        owner.getWorld().playSound(owner.getLocation(), Sound.GHAST_SCREAM, 2f, 0.5f);

        SmashServer server = GameManager.getPlayerServer(owner);

        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            private int ticks = 0;

            private int originalStocks = -1;
            private int currentStocks = -1;
            private Location lastTargetLocation;
            private LivingEntity target = null;
            private Location locationAtTargeting;
            private boolean hasStartedTargeting = false;

            //homing :3
            @Override
            public void run() {

                // i dont care taht this is fucking stupid i have a fucking anerurysm rn
                if(target != null)
                {
                    currentStocks = server.getLives((Player) target);
                    if(currentStocks < originalStocks)
                    {
                        target = null;
                        originalStocks = -1;
                        currentStocks = -1;
                    }
                }

                if (KitManager.getPlayerKit(owner) == null) {
                    ball.remove();
                    Bukkit.getScheduler().cancelTask(task);
                    return;
                }

                if(!(ball.isValid()))
                {
                    ball.remove();
                    Bukkit.getScheduler().cancelTask(task);
                    return;
                }

                if(ticks > 140)
                {
                    ball.remove();
                    Bukkit.getScheduler().cancelTask(task);
                    return;
                }


                if((target == null) && ticks > 7) //get target if one doesnt exist
                {
                    HashMap<LivingEntity, Double> nearby_entities = Utils.getInRadius(ball.getLocation(), range);
                    double closest = -1;
                    for(LivingEntity entity : nearby_entities.keySet())
                    {
                        if(entity == owner)
                        {
                            continue;
                        }
                        if(!(entity instanceof Player))
                        {
                            continue;
                        }
                        if(nearby_entities.get(entity) < closest)
                        {
                            closest = nearby_entities.get(entity);
                            target = entity;
                            originalStocks = currentStocks = server.getLives((Player) target);
                            lastTargetLocation = target.getLocation();
                            locationAtTargeting = ball.getLocation().subtract(eFireball.dirX, eFireball.dirY, eFireball.dirZ);
                        }
                        else if(closest == -1)
                        {
                            closest = nearby_entities.get(entity);
                            target = entity;
                            originalStocks = currentStocks = server.getLives((Player) target);
                            lastTargetLocation = target.getLocation();
                            locationAtTargeting = ball.getLocation().subtract(eFireball.dirX, eFireball.dirY, eFireball.dirZ);
                        }
                    }

                    if(ticks > 100)
                    {
                        ball.remove();
                        Bukkit.getScheduler().cancelTask(task);
                        return;
                    }

                    if(target != null) {
                        Utils.playParticle(EnumParticle.REDSTONE, target.getLocation().add(0, 1, 0),
                                0.3f, 0.3f, 0.3f, 0, 2, 96, owner.getWorld().getPlayers());
                        target.getLocation().getWorld().playSound(target.getLocation(), Sound.NOTE_PLING, 2f, 0.5f);
                        calculateProjVelocity(ball, target);
                    }
                }
                else if(target != null)
                {
                    Location ballLocation = ball.getLocation();
                    Location targetLocation = target.getLocation();

                    if(!hasStartedTargeting)
                    {
                        ball.teleport(locationAtTargeting);
                        hasStartedTargeting = true;
                    }

                    if(ballLocation.distance(targetLocation) > range)
                    {
                        owner.getWorld().playSound(owner.getLocation(), Sound.NOTE_BASS, 2f, 0.5f);
                        target = null;
                        hasStartedTargeting = false;
                    }
                    else if(!(targetLocation.equals(lastTargetLocation)))
                    {
                        calculateProjVelocity(ball, target);

                        lastTargetLocation = targetLocation;
                    }

                }
                Utils.playParticle(EnumParticle.SMOKE_NORMAL,  ball.getLocation().add(0, 0.25, 0),
                        0.3f, 0.3f, 0.3f, 1, 3, 96, owner.getWorld().getPlayers());

                ticks++;
            }
        }, 0L, 0L);
    }

    public static void calculateProjVelocity(LargeFireball ball, LivingEntity target)
    {
        // listen. this might be stupid but this seemed like the only way to get it to not miss by like 1 or 2 blocks all the time ;^;

        Vector ballVelocity = new Vector();
        EntityLargeFireball eFireball = ((CraftLargeFireball) ball).getHandle();

        double xTargetCorrection = 0;
        double yTargetCorrection = 0;
        double zTargetCorrection = 0;

        if (ball.getLocation().getX() < target.getLocation().getX())
        {
            xTargetCorrection = 1;
        }
        else
        {
            xTargetCorrection = -1;
        }

        if (ball.getLocation().getY() > target.getLocation().getY())
        {
            yTargetCorrection = 1;
        }
        else
        {
            yTargetCorrection = -1;
        }

        if (ball.getLocation().getZ() < target.getLocation().getZ())
        {
            zTargetCorrection = 1;
        }
        else
        {
            zTargetCorrection = -1;
        }

        ballVelocity = (target.getLocation()).subtract(ball.getLocation()).add(xTargetCorrection, yTargetCorrection, zTargetCorrection)
                .toVector().normalize().multiply(0.15);

        eFireball.dirX = ballVelocity.getX();
        eFireball.dirY = ballVelocity.getY();
        eFireball.dirZ = ballVelocity.getZ();
    }

    @EventHandler
    public void Collide(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        List<MetadataValue> data = projectile.getMetadata("Ghast ICBM");
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
        Bukkit.getScheduler().cancelTask(task);
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