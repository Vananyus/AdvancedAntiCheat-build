package com.example.anticheat;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AdvancedAntiCheatPaper extends JavaPlugin {

    private Map<UUID, Boolean> isPunishing = new HashMap<>();

    @Override
    public void onEnable() {
        getLogger().info("AdvancedAntiCheat enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("AdvancedAntiCheat disabled!");
    }

    // Метод для наказания игрока
    public void startPunishment(Player target) {
        UUID id = target.getUniqueId();
        if (isPunishing.getOrDefault(id, false)) return;
        isPunishing.put(id, true);

        final double startY = target.getLocation().getY();
        final double targetY = startY + 3.0;

        // Частицы и подъём
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline() || target.isDead()) {
                    isPunishing.remove(id);
                    cancel();
                    return;
                }

                target.getWorld().spawnParticle(
                        Particle.REDSTONE,
                        target.getLocation().add(0,1,0),
                        30,
                        0.6,1.0,0.6,
                        0.02,
                        new Particle.DustOptions(Color.RED, 1f)
                );

                double curY = target.getLocation().getY();
                if (curY < targetY) {
                    double remaining = targetY - curY;
                    double vy = Math.min(0.18, Math.max(0.08, remaining * 0.25));
                    Vector v = target.getVelocity();
                    v.setY(Math.max(v.getY(), vy));
                    target.setVelocity(v);
                } else {
                    Location loc = target.getLocation();
                    loc.setY(targetY);
                    target.teleport(loc);
                    isPunishing.remove(id);
                    cancel();
                }
            }
        }.runTaskTimer(this, 0L, 2L);

        // Бан через 6 секунд
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!target.isOnline()) {
                    isPunishing.remove(id);
                    return;
                }
                String banCommand = "ban " + target.getName() + " AntCheat: Вы были забанены за читы";
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), banCommand);
                try {
                    target.kickPlayer("AntCheat: Вы были забанены за читы");
                } catch (Exception ignored){}
                isPunishing.remove(id);
                getLogger().info("[AAC] Banned " + target.getName());
            }
        }.runTaskLater(this, 20L * 6);
    }
}
