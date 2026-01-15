package ru.florestdev.florestDiscordPro;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class TPSListener {
    private final FlorestDiscordPro plugin;
    private final Methods methods;
    private final List<Long> tickTimes = new ArrayList<>();
    private BukkitTask tpsTask;
    private long lastMeasurementTime = System.currentTimeMillis();
    private volatile double currentTPS = 20.0;

    // Флаг, чтобы не спамить уведомлениями каждую секунду при лагах
    private long lastNotificationSent = 0;

    public TPSListener(FlorestDiscordPro plugin) {
        this.plugin = plugin;
        this.methods = plugin.getMethods();
    }

    public void startTask() {
        if (tpsTask != null && !tpsTask.isCancelled()) {
            return;
        }

        tpsTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            long currentTime = System.currentTimeMillis();

            // Добавляем текущее время тика
            tickTimes.add(currentTime);

            // Оставляем только тики за последнюю секунду
            tickTimes.removeIf(tick -> tick < currentTime - 1000);

            long interval = currentTime - lastMeasurementTime;

            if (interval >= 1000) {
                currentTPS = tickTimes.size();

                // Ограничиваем сверху, так как тиков не может быть больше 20
                if (currentTPS > 20.0) currentTPS = 20.0;

                int minimum = plugin.getConfig().getInt("tps_min", 15);

                // Проверяем просадку и КД на уведомления (например, раз в 5 минут)
                if (currentTPS < minimum && (currentTime - lastNotificationSent > 300000)) {
                    sendLaggWarning();
                    lastNotificationSent = currentTime;
                }

                lastMeasurementTime = currentTime;
            }
        }, 1L, 1L); // Запуск каждый ТИК для точности

        plugin.getLogger().log(Level.INFO, "Система отслеживания TPS (Discord) запущена.");
    }

    private void sendLaggWarning() {
        String channelId = plugin.getConfig().getString("discord_channel_id");
        String message = plugin.getConfig().getString("message_tps_lagg", "⚠️ На сервере упал TPS: {tps}")
                .replace("{tps}", String.format("%.2f", currentTPS));

        methods.sendDiscordMessage(channelId, message);
    }

    public double getCurrentTPS() {
        return currentTPS;
    }

    public void stopTask() {
        if (tpsTask != null) {
            tpsTask.cancel();
        }
    }
}