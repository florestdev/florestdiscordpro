package ru.florestdev.florestDiscordPro;

import ru.florestdev.florestTelegramPRO.FlorestTelegramPRO;
import ru.florestdev.florestTelegramPRO.Methods;

public class DiscordToTG {
    public static FlorestDiscordPro discordPro;
    public static FlorestTelegramPRO telegramPro;
    public static String bot_token;
    public static String group_id;
    public static int theme;
    public static Methods methods;

    public DiscordToTG(FlorestDiscordPro florestDiscordPro, FlorestTelegramPRO telegramPro) {
        this.discordPro = florestDiscordPro;
        this.telegramPro = telegramPro;

        this.bot_token = telegramPro.getConfig().getString("telegram_bot_token");
        this.group_id = telegramPro.getConfig().getString("telegram_chat_id");

        if (telegramPro.getConfig().getBoolean("support_themes")) {
            this.theme = telegramPro.getConfig().getInt("follow_theme");
        }

        this.methods = new Methods(telegramPro);

    }

    public void sendMessage(String username, String message) {
        methods.sendTelegramMessage(bot_token, group_id, discordPro.getConfig().getString("discord_tg_message").replace("{discord_name}", username).replace("{discord_message}", message));
    }

}
