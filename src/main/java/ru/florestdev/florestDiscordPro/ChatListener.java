package ru.florestdev.florestDiscordPro;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.regex.Pattern;

public class ChatListener implements Listener {

    private final FlorestDiscordPro plugin;
    private final Methods methods;

    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    public ChatListener(FlorestDiscordPro plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();

        // 1. Проверка фильтрации (игнор префикса из конфига)
        String ignorePrefix = plugin.getConfig().getString("restrictions.prefix_ignore_mc", "#");
        if (message.startsWith(ignorePrefix)) {
            return;
        }

        // 2. Формирование сообщения
        String channelId = plugin.getConfig().getString("discord_channel_id");
        String format = plugin.getConfig().getString("discord_message_format", "[MC] **{player}**: {message}");

        String formattedMessage = format
                .replace("{player}", player.getName())
                .replace("{message}", message);

        // 3. Обработка префикса LuckPerms
        if (plugin.getConfig().getBoolean("support_prefix")) {
            formattedMessage = formattedMessage.replace("{prefix}", getPlayerPrefix(player));
        } else {
            formattedMessage = formattedMessage.replace("{prefix}", "");
        }

        // 4. Отправка в Discord
        if (!plugin.getConfig().getBoolean("enable_webhook")) {
            methods.sendDiscordMessage(channelId, formattedMessage);
        } else {
            methods.sendWebhookMessage(event.getPlayer().getName(), formattedMessage);
        }
    }

    /**
     * Получение чистого префикса игрока без кодов форматирования
     */
    private String getPlayerPrefix(Player player) {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider == null) return "";

            LuckPerms api = provider.getProvider();
            User user = api.getUserManager().getUser(player.getUniqueId());
            if (user == null) return "";

            String prefix = user.getCachedData().getMetaData().getPrefix();
            return prefix != null ? removeMinecraftFormatting(prefix) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String removeMinecraftFormatting(String text) {
        if (text == null) return "";
        // Удаляем HEX и обычные цветовые коды
        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");
        return text;
    }
}