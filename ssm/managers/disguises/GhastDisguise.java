package ssm.managers.disguises;

import net.minecraft.server.v1_8_R3.*;
import org.bukkit.DyeColor;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import ssm.utilities.Utils;

public class GhastDisguise extends Disguise {

    private byte attacking = -1;

    public GhastDisguise(Player owner) {
        super(owner);
        name = "Ghast";
        type = EntityType.GHAST;
    }

    protected EntityLiving newLiving() {
        return new EntityGhast(((CraftWorld) owner.getWorld()).getHandle());
    }
    public void setAttacking(byte attacking) {
        this.attacking = attacking;
        DataWatcher dw = living.getDataWatcher();
        dw.watch(16, attacking);
        PacketPlayOutEntityMetadata target_packet = new PacketPlayOutEntityMetadata(living.getId(), dw, true);
        Utils.sendPacketToAll(target_packet);
    }

    public byte getAttacking() {
        return attacking;
    }
}
