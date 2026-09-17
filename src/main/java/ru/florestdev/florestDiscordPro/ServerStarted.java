package ru.florestdev.florestDiscordPro;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.Plugin;
import ru.florestdev.florestTelegramPRO.FlorestTelegramPRO;

public class ServerStarted implements Listener {

    public static FlorestDiscordPro discordPro;

    public ServerStarted(FlorestDiscordPro discordPro) {
        this.discordPro = discordPro;
    }

    @EventHandler
    public void onServerStarted(ServerLoadEvent event) {
        Server server = Bukkit.getServer();
        Plugin plugin = server.getPluginManager().getPlugin("FlorestTelegramPRO");
        if (plugin instanceof FlorestTelegramPRO) {
            discordPro.getLogger().info("Initialized the DS-Telegram friendship.");
            discordPro.updateDiscordToTg(new DiscordToTG(discordPro, (FlorestTelegramPRO) plugin));
        }
    }
}