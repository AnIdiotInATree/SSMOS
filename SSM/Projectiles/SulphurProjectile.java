package SSM.Projectiles;

import SSM.Events.SmashDamageEvent;
import SSM.Utilities.ServerMessageType;
import SSM.Utilities.Utils;
import SSM.Utilities.VelocityUtil;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class SulphurProjectile extends SmashProjectile {

    public SulphurProjectile(Player firer, String name) {
        super(firer, name);
        this.damage = 6.5;
        this.hitbox_mult = 0.5;
        this.knockback_mult = 2.5;
    }

    @Override
    public void launchProjectile() {
        super.launchProjectile();
        firer.getWorld().playSound(firer.getLocation(), Sound.CREEPER_DEATH, 2f, 1.5f);
    }

    @Override
    protected Entity createProjectileEntity() {
        ItemStack coal = new ItemStack(Material.COAL);
        return firer.getWorld().dropItem(firer.getEyeLocation(), coal);
    }

    @Override
    protected void doVelocity() {
        VelocityUtil.setVelocity(projectile, firer.getLocation().getDirection(),
                1.2, false, 0, 0.2, 10, false);
    }

    @Override
    protected void doEffect() {
        return;
    }

    @Override
    protected boolean onExpire() {
        explodeEffect();
        return true;
    }

    @Override
    protected boolean onHitLivingEntity(LivingEntity hit) {
        explodeEffect();
        SmashDamageEvent smashDamageEvent = new SmashDamageEvent(hit, firer, damage);
        smashDamageEvent.multiplyKnockback(knockback_mult);
        smashDamageEvent.setIgnoreDamageDelay(true);
        smashDamageEvent.setReason(name);
        smashDamageEvent.callEvent();
        return true;
    }

    @Override
    protected boolean onHitBlock(Block hit) {
        explodeEffect();
        return true;
    }

    @Override
    protected boolean onIdle() {
        explodeEffect();
        return true;
    }

    protected void explodeEffect() {
        Utils.playParticle(EnumParticle.EXPLOSION_LARGE, projectile.getLocation(),
                0, 0, 0, 0, 1, 96, projectile.getWorld().getPlayers());
        projectile.getWorld().playSound(projectile.getLocation(), Sound.EXPLODE, 1f, 1.5f);
    }

}
