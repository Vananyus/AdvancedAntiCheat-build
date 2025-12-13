package com.yourname.anticheat;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class AntiCheatCore extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> violations = new HashMap<>();
    private final Map<UUID, List<Long>> hitTimestamps = new HashMap<>();
    private final Map<UUID, Boolean> isPunishing = new HashMap<>();

    private static final int MAX_VIOLATIONS = 20;
    private static final int MAX_ALLOWED_CPS = 16;
    private static final double MAX_FLIGHT_Y = 0.5;

    @Override
    public void onEnable() {
        getLogger().info("AntiCheat включен (Spigot 1.16.5)");
        getServer().getPluginManager().registerEvents(this, this);
    }

    // ===== ДВИЖЕНИЕ =====
    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        Location from = e.getFrom();
        Location to = e.getTo();
        UUID id = p.getUniqueId();

        if (isPunishing.getOrDefault(id, false)) {
            e.setCancelled(true);
            return;
        }

        if (p.getGameMode() == GameMode.CREATIVE || p.isFlying()) return;

        double dist = from.distance(to);
        double dy = to.getY() - from.getY();

        double maxSpeed = p.isSprinting() ? 0.52 : 0.4;
        if (dist > maxSpeed) addVL(p, "Speed");
        else reduceVL(p);

        if (!p.isOnGround() && dy > MAX_FLIGHT_Y) {
            addVL(p, "Flight");
        }

        if (p.getLocation().getBlock().isLiquid() && dy == 0 && !p.isOnGround()) {
            addVL(p, "Jesus");
        }
    }

    // ===== NOFALL =====
    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player) {
            Player p = (Player) e.getEntity();
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL &&
                p.getFallDistance() > 3 && e.getDamage() == 0) {
                addVL(p, "NoFall");
            }
        }
    }

    // ===== CPS =====
    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        Player p = (Player) e.getDamager();

        long now = System.currentTimeMillis();
        List<Long> list = hitTimestamps.getOrDefault(p.getUniqueId(), new ArrayList<>());
        list.removeIf(t -> now - t > 1000);
        list.add(now);
        hitTimestamps.put(p.getUniqueId(), list);

        if (list.size() > MAX_ALLOWED_CPS) {
            addVL(p, "AutoClicker (" + list.size() + " CPS)");
        }
    }

    // ===== VL =====
    private void addVL(Player p, String check) {
        int vl = violations.getOrDefault(p.getUniqueId(), 0) + 1;
        violations.put(p.getUniqueId(), vl);
        p.sendMessage(ChatColor.RED + "[AntiCheat] " + check + " VL=" + vl);

        if (vl >= MAX_VIOLATIONS) punish(p);
    }

    private void reduceVL(Player p) {
        violations.put(
            p.getUniqueId(),
            Math.max(0, violations.getOrDefault(p.getUniqueId(), 0) - 1)
        );
    }

    // ===== НАКАЗАНИЕ =====
    private void punish(Player p) {
        isPunishing.put(p.getUniqueId(), true);

        new BukkitRunnable() {
            @Override
            public void run() {
                getServer().dispatchCommand(
                    getServer().getConsoleSender(),
                    "ban " + p.getName() + " Забанен античитом"
                );
            }
        }.runTaskLater(this, 10L);
    }
}
