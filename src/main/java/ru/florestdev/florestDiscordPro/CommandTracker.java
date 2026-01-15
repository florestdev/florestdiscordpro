package ru.florestdev.florestDiscordPro;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class CommandTracker implements Listener {

    private final FlorestDiscordPro plugin;
    private final Methods methods;

    public CommandTracker(FlorestDiscordPro plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("CommandTracker (Discord) успешно зарегистрирован!");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        // Проверяем, включен ли трекинг вообще
        if (!plugin.getConfig().getBoolean("command_tracking.enabled", false)) {
            return;
        }

        String fullMessage = event.getMessage(); // Например: "/tp player1 100 200"
        String baseCommand = fullMessage.split(" ")[0].toLowerCase(); // Извлекаем только "/tp"

        List<String> whitelist = plugin.getConfig().getStringList("command_tracking.whitelist");
        List<String> blacklist = plugin.getConfig().getStringList("command_tracking.blacklist");

        boolean shouldLog = false;

        // Логика фильтрации
        if (whitelist.contains("all")) {
            // Если разрешены все, проверяем, не в черном ли списке конкретная команда
            if (!blacklist.contains(baseCommand)) {
                shouldLog = true;
            }
        } else {
            // Если "all" нет, логируем только то, что в белом списке
            if (whitelist.contains(baseCommand)) {
                shouldLog = true;
            }
        }

        if (shouldLog) {
            String channelId = plugin.getConfig().getString("discord_channel_id");
            String format = plugin.getConfig().getString("command_tracking.format", "⚠️ `{user}` использовал команду: `{command}`");

            String message = format
                    .replace("{user}", event.getPlayer().getName())
                    .replace("{command}", fullMessage);

            methods.sendDiscordMessage(channelId, message);
        }
    }
}