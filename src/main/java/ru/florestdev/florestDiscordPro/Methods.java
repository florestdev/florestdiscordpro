package ru.florestdev.florestDiscordPro;

import com.google.gson.JsonObject;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.WebhookClient;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.skinsrestorer.api.SkinsRestorerProvider;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public class Methods {

    private final FlorestDiscordPro plugin;

    public Methods(FlorestDiscordPro plugin) {
        this.plugin = plugin;
    }

    /**
     * Отправка сообщения в указанный канал Discord
     */
    public void sendDiscordMessage(String channelId, String message) {
        if (message == null || message.isEmpty()) return;

        // Проверяем, инициализирован ли бот
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) {
            return;
        }

        try {
            TextChannel channel = plugin.getDiscordManager().getJda().getTextChannelById(channelId);
            if (channel != null) {
                channel.sendMessage(message).queue(
                        success -> {}, // Можно добавить лог успеха, если надо
                        error -> plugin.getLogger().warning("Не удалось отправить сообщение в Discord: " + error.getMessage())
                );
            } else {
                plugin.getLogger().warning("Канал с ID " + channelId + " не найден!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при отправке сообщения: " + e.getMessage());
        }
    }

    /**
     * Обновление статуса (Activity) бота (замена setChatDescription из телеги)
     */
    public void updateBotStatus(int online, int max) {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) return;

        String activityText = plugin.getConfig().getString("bot_status.activity_text", "Online: {online}/{max}")
                .replace("{online}", String.valueOf(online))
                .replace("{max}", String.valueOf(max));

        plugin.getDiscordManager().getJda().getPresence().setActivity(Activity.playing(activityText));
    }

    /**
     * Бан пользователя (по ID)
     */
    public void banDiscordUser(String userId, String reason) {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) return;

        String channelId = plugin.getConfig().getString("discord_channel_id");
        TextChannel channel = plugin.getDiscordManager().getJda().getTextChannelById(channelId);

        if (channel != null) {
            channel.getGuild().ban(net.dv8tion.jda.api.entities.User.fromId(userId), 0, TimeUnit.DAYS)
                    .reason(reason)
                    .queue(
                            success -> plugin.getLogger().info("Пользователь " + userId + " забанен в Discord."),
                            error -> plugin.getLogger().warning("Не удалось забанить")
                    );
        }
    }

    public void sendWebhookMessage(String playerName, String message) {
        String channelId = plugin.getConfig().getString("discord_channel_id");
        var jda = ((FlorestDiscordPro) plugin).getDiscordManager().getJda();
        var channel = jda.getTextChannelById(channelId);

        if (channel == null) return;


        String webhookUrl = plugin.getConfig().getString("webhook_url");
        String skinName = playerName; // По умолчанию

        // Пытаемся получить API SkinsRestorer
        try {
            if (Bukkit.getPluginManager().isPluginEnabled("SkinsRestorer")) {
                var skinsRestorer = SkinsRestorerProvider.get();
                var skinStorage = skinsRestorer.getSkinStorage();

                // Получаем скин игрока (если он установлен)
                var skin = skinStorage.findSkinData(playerName);
                if (skin.isPresent()) {
                    // Если у игрока стоит кастомный скин, берем его имя для minotar
                    if (skin.get().getIdentifier().toString().startsWith("http://") || skin.get().getIdentifier().toString().startsWith("https://")) {
                        // ...
                    } else {
                        skinName = skin.get().getIdentifier().toString();
                    }

                }
            }
        } catch (Exception ignored) {
            // Если что-то пошло не так, просто оставим playerName
        }

        // Формируем JSON вручную (или через Gson, который есть в Bukkit)
        JsonObject json = new JsonObject();
        json.addProperty("content", message);
        json.addProperty("username", playerName);
        json.addProperty("avatar_url", "https://minotar.net/helm/" + skinName + "/100.png");

            // Отправляем обычным HttpClient (он у тебя уже был в коде)
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Разбан пользователя
     */
    public void unbanDiscordUser(String userId) {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) return;

        String channelId = plugin.getConfig().getString("discord_channel_id");
        TextChannel channel = plugin.getDiscordManager().getJda().getTextChannelById(channelId);

        if (channel != null) {
            channel.getGuild().unban(net.dv8tion.jda.api.entities.User.fromId(userId)).queue(
                    success -> plugin.getLogger().info("Пользователь " + userId + " разбанен."),
                    error -> plugin.getLogger().warning("Не удалось разбанить: " + error.getMessage())
            );
        }
    }

    // Мьюты в Discord обычно делаются через роли или Timeout (JDA поддерживает timeout)
    public void muteDiscordUser(String userId, int minutes) {
        if (plugin.getDiscordManager() == null || plugin.getDiscordManager().getJda() == null) return;

        String channelId = plugin.getConfig().getString("discord_channel_id");
        TextChannel channel = plugin.getDiscordManager().getJda().getTextChannelById(channelId);

        if (channel != null) {
            channel.getGuild().timeoutFor(net.dv8tion.jda.api.entities.User.fromId(userId), java.time.Duration.ofMinutes(minutes)).queue();
        }
    }

}
