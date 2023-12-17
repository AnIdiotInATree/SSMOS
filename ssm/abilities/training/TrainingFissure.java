package ssm.abilities.training;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.minecraft.server.v1_8_R3.EnumParticle;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import ssm.abilities.Ability;
import ssm.events.SmashDamageEvent;
import ssm.managers.BlockRestoreManager;
import ssm.managers.ownerevents.OwnerRightClickEvent;
import ssm.utilities.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class TrainingFissure extends Ability implements OwnerRightClickEvent {

    private int task = -1;
    protected HashMap<Block, Integer> blocks = new HashMap<Block, Integer>();
    protected List<Entity> already_hit = new ArrayList<Entity>();

    public TrainingFissure() {
        super();
        this.name = "Fissure";
        this.cooldownTime = 3;
        this.description = new String[]{
                ChatColor.RESET + "Smash the ground, creating a fissure",
                ChatColor.RESET + "which slowly rises in a line, dealing",
                ChatColor.RESET + "damage and knockback to anyone it hits!",
        };
    }

    public void onOwnerRightClick(PlayerInteractEvent e) {
        if (!check()) {
            return;
        }
        if (!Utils.entityIsOnGround(owner)) {
            Utils.sendAttributeMessage("You cannot use",
                    name + ChatColor.GRAY + " while airborne", owner, ServerMessageType.SKILL);
            return;
        }
        checkAndActivate();
    }

    public void activate() {
        Location location = owner.getLocation();
        Location locationClone = location.clone();

        FissureData data = new FissureData(this, owner,
                location.getDirection(), location.add(location.getDirection()).add(0, -0.4, 0));

        TextComponent infoText = new TextComponent(ServerMessageType.GAME + "Used fissure at §e" + locationClone.getX() + " " + locationClone.getY() + " " + locationClone.getZ()
                + " " + locationClone.getYaw() + " " + locationClone.getPitch());
        infoText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§fClick to teleport to exact fissure usage location").create()));
        infoText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + owner.getName()
                + " " + locationClone.getX() + " " + locationClone.getY() + " " + locationClone.getZ() + " " + locationClone.getYaw() + " " + locationClone.getPitch()));
        owner.spigot().sendMessage(infoText);

        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (data.update()) {
                    data.clear();
                    Bukkit.getScheduler().cancelTask(task);
                }
            }
        }, 0L, 0L);
    }

    public class FissureData {
        private TrainingFissure host;

        private Player player;

        private Vector vec;
        private Location loc;
        private Location startLoc;

        private int height = 0;
        private int handled = 0;
        private int successes = 0;

        private HashSet<Player> hit = new HashSet<Player>();

        private ArrayList<Block> path = new ArrayList<Block>();

        public FissureData(TrainingFissure host, Player player, Vector vec, Location loc) {
            this.host = host;

            vec.setY(0);
            vec.normalize();
            vec.multiply(0.1);

            this.player = player;
            this.vec = vec;
            this.loc = loc;
            this.startLoc = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());

            MakePath();
        }

        private void MakePath() {
            while (Utils.getXZDistance(loc, startLoc) < 14) {
                boolean failed = false;
                loc.add(vec);

                Block block = loc.getBlock();

                if (block.equals(startLoc.getBlock()))
                {
                    Utils.playParticle(EnumParticle.REDSTONE, loc, 0, 0, 0, 0, 3, 96, loc.getWorld().getPlayers());
                    Utils.sendServerMessageToPlayer("Failed because block = startLoc §e"
                            + block.getLocation().getX() + " " + block.getLocation().getY() + " " + block.getLocation().getZ(), owner, ServerMessageType.GAME);
                    failed = true;
                }


                if (path.contains(block))
                {
                    Utils.playParticle(EnumParticle.REDSTONE, loc, 0, 0, 0, 0, 3, 96, loc.getWorld().getPlayers());
                    failed = true;
                }

                if(failed)
                {
                    continue;
                }

                //Move up 1, cant go 2 up
                if (isSolid(block.getRelative(BlockFace.UP))) {
                    loc.add(0, 1, 0);
                    block = loc.getBlock();

                    if (isSolid(block.getRelative(BlockFace.UP))) {
                        BlockRestoreManager.ourInstance.add(block, Material.STAINED_GLASS.getId(), (byte) 14, 1500);
                        Utils.sendServerMessageToPlayer("Failed because next possible block is too high §e"
                                + block.getLocation().getX() + " " + block.getLocation().getY() + " " + block.getLocation().getZ(), owner, ServerMessageType.GAME);
                        Utils.playParticle(EnumParticle.VILLAGER_HAPPY, loc, 0, 0, 0, 0, 1, 96, loc.getWorld().getPlayers());
                        return;
                    }

                }

                //Move down 1, cant go 2 down
                else if (!isSolid(block)) {
                    loc.add(0, -1, 0);
                    block = loc.getBlock();

                    if (!isSolid(block)) {
                        BlockRestoreManager.ourInstance.add(block, Material.STAINED_GLASS.getId(), (byte) 14, 1500);
                        Utils.sendServerMessageToPlayer("Failed because next possible block is too low §e"
                                + block.getLocation().getX() + " " + block.getLocation().getY() + " " + block.getLocation().getZ(), owner, ServerMessageType.GAME);
                        Utils.playParticle(EnumParticle.VILLAGER_HAPPY, loc, 0, 0, 0, 0, 1, 96, loc.getWorld().getPlayers());
                        return;
                    }
                }

                double distance = block.getLocation().add(0.5, 0.5, 0.5).distance(loc);
                String distanceToThousandths = String.valueOf(distance).substring(0, 5);

                if (distance > 0.5)
                {
                    Utils.playParticle(EnumParticle.DRIP_LAVA, loc, 0, 0, 0, 0, 3, 96, loc.getWorld().getPlayers());
                    Utils.sendServerMessageToPlayer("Failed because selected block is too far §c(" + distanceToThousandths +
                            ") §7from location §e" + block.getLocation().getX() + " " + block.getLocation().getY() + " " + block.getLocation().getZ(), owner, ServerMessageType.GAME);
                    failed = true;
                }

                if(failed)
                {
                    continue;
                }

                if(successes % 3 == 0) //lags hardcore if i dont do this idk
                {
                    Utils.playParticle(EnumParticle.FIREWORKS_SPARK, loc.add(0,1,0), 0, 0, 0, 0, 1, 96, loc.getWorld().getPlayers());
                }
                path.add(block);
                successes++;

                //Effect
                //loc.getWorld().playEffect(block.getLocation(), Effect.STEP_SOUND, block.getTypeId());

                //Slow
                for (Player cur : block.getWorld().getPlayers())
                    if (!cur.equals(player))
                        if ((block.getLocation().add(0.5, 0.5, 0.5).distance(cur.getLocation())) < 1.5) {
                            //Condition
                            player.removePotionEffect(PotionEffectType.SLOW);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 80, 1));
                            player.setVelocity(new Vector(0, 0, 0));
                        }
            }
        }

        public boolean update() {
            if (handled >= path.size())
                return true;

            Block block = path.get(handled);

            //Cannot raise
            if (block.getTypeId() == 46)
                return false;

            Block up = block.getRelative(0, height + 1, 0);

            //Done Column
            if (!BlocksUtil.isAirOrFoliage(up)) {
                loc.getWorld().playEffect(up.getLocation(), Effect.STEP_SOUND, up.getTypeId());
                height = 0;
                handled++;
                return false;
            }

            //Boost Column
            if (block.getTypeId() == 1) BlockRestoreManager.ourInstance.add(block, Material.GLASS.getId(), (byte) 0, 1500);
            if (block.getTypeId() == 2) BlockRestoreManager.ourInstance.add(block, Material.GLASS.getId(), (byte) 0, 1500);
            if (block.getTypeId() == 98) BlockRestoreManager.ourInstance.add(block, Material.GLASS.getId(), (byte) 0, 1500);

            if (block.getType() == Material.SNOW) {
                BlockRestoreManager.ourInstance.add(block, Material.GLASS.getId(), (byte) 0, 3500 - (1000 * height));
                BlockRestoreManager.ourInstance.add(up, Material.GLASS.getId(), (byte) 0, 3500 - (1000 * height));
            } else {
                BlockRestoreManager.ourInstance.add(up, Material.GLASS.getId(), (byte) 0, 3500 - (1000 * height));
            }
            height++;

            //Effect
            up.getWorld().playEffect(up.getLocation(), Effect.STEP_SOUND, block.getTypeId());

            //Damage
            for (Player cur : up.getWorld().getPlayers())
                if (!cur.equals(player)) {
                    //Teleport
                    if (cur.getLocation().getBlock().equals(block)) {
                        cur.teleport(cur.getLocation().add(0, 1, 0));


                    }

                    int damage = 4 + handled;

                    if(!DamageUtil.canDamage(cur, player)) {
                        continue;
                    }

                    //Damage
                    if (!hit.contains(cur))
                        if ((up.getLocation().add(0.5, 0.5, 0.5).distance(cur.getLocation())) < 1.5) {
                            hit.add(cur);

                            SmashDamageEvent smashDamageEvent = new SmashDamageEvent(cur, owner, damage);
                            smashDamageEvent.multiplyKnockback(0);
                            smashDamageEvent.setReason(name);
                            smashDamageEvent.callEvent();
                            Utils.sendAttributeMessage(ChatColor.YELLOW + owner.getName() +
                                    ChatColor.GRAY + " hit you with", name, cur, ServerMessageType.GAME);

                            final Player fPlayer = cur;
                            final Location fLoc = up.getLocation().add(0.5, 0.5, 0.5);

                            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                                public void run() {
                                    // From + X = To
                                    // X = To - From
                                    Vector trajectory = fPlayer.getLocation().toVector().subtract(fLoc.toVector().clone());
                                    trajectory.setY(0);
                                    VelocityUtil.setVelocity(fPlayer, trajectory.normalize(),
                                            1 + 0.1 * handled, true, 0.6 + 0.05 * handled, 0, 10, true);
                                }
                            }, 4);
                        }
                }

            //Next Column
            if (height >= Math.min(2, handled / 3 + 1)) {
                height = 0;
                handled++;
            }

            return (handled >= path.size());
        }

        public void clear() {
            hit.clear();
            path.clear();
            host = null;
            player = null;
            loc = null;
            startLoc = null;
        }

        private boolean isSolid(Block block) {
            return block.getType().isSolid() || block.getType() == Material.SNOW;
        }

    }

}
