package ru.florestdev.florestDiscordPro;

import net.dv8tion.jda.api.entities.Activity;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

public class PlayerTracker implements Listener {

    private final FlorestDiscordPro plugin;
    private final Methods methods;

    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    public PlayerTracker(FlorestDiscordPro plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        plugin.getLogger().info("PlayerTracker (Discord) успешно зарегистрирован!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join_leave.enabled", true)) return;

        Player player = event.getPlayer();
        String channelId = plugin.getConfig().getString("discord_channel_id");

        String message = plugin.getConfig().getString("join_leave.human_joined", "📥 **{user}** зашел на сервер.")
                .replace("{user}", player.getName());

        TwoFactorDatabase.TwoFactorData data = plugin.getTwoFactorDatabase().getByUsername(player.getName());

        if (data != null && data.enabled) {
            if (data.isBlocked(3, 600000)) {
                player.kickPlayer("§cСлишком много попыток. Подождите 10 минут.");
                return;
            }

            String code = String.format("%06d", new Random().nextInt(1000000));
            long expires = System.currentTimeMillis() + 120000;
            plugin.getTwoFactorDatabase().updateCode(player.getName(), code, expires);

            methods.sendDiscordMessageUser(data.telegramId, plugin.getConfig().getString("2fa_message").replace("{username}", player.getName()).replace("{ip}", player.getAddress().getAddress().getHostAddress()).replace("{code}", code));
            plugin.getTwoFactorHandler().freezePlayer(player);
        } else {
            return;
        }

        // Добавляем префикс
        if (plugin.getConfig().getBoolean("support_prefix")) {
            message = message.replace("{prefix}", getPlayerPrefix(player));
        } else {
            message = message.replace("{prefix}", "");
        }

        methods.sendDiscordMessage(channelId, message);
        updateChannelDescription();
        plugin.getDiscordManager().getJda().getPresence().setActivity(Activity.playing(plugin.getConfig().getString("bot_status.activity_text", "FlorestWorld").replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (plugin.getTwoFactorHandler().isFrozen(event.getPlayer())) {
            plugin.getTwoFactorHandler().unfreezePlayer(event.getPlayer());
        }
        if (!plugin.getConfig().getBoolean("join_leave.enabled", true)) return;

        Player player = event.getPlayer();
        String channelId = plugin.getConfig().getString("discord_channel_id");

        String message = plugin.getConfig().getString("join_leave.human_quited", "📤 **{user}** покинул нас.")
                .replace("{user}", player.getName());

        if (plugin.getConfig().getBoolean("support_prefix")) {
            message = message.replace("{prefix}", getPlayerPrefix(player));
        } else {
            message = message.replace("{prefix}", "");
        }

        methods.sendDiscordMessage(channelId, message);
        plugin.getDiscordManager().getJda().getPresence().setActivity(Activity.playing(plugin.getConfig().getString("bot_status.activity_text", "FlorestWorld").replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size() - 1))));

        // Вместо Thread.sleep(3000), который вешает сервер, используем задачу Bukkit
        Bukkit.getScheduler().runTaskLater(plugin, this::updateChannelDescription, 40L); // задержка 2 секунды (40 тиков)
    }

    private void updateChannelDescription() {
        if (!plugin.getConfig().getBoolean("desc_editing_bool")) return;

        String channelId = plugin.getConfig().getString("discord_channel_id");
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String formattedDate = formatter.format(new Date());

        String desc = plugin.getConfig().getString("on_online_desc", "Онлайн: {players_online}/{players_max}")
                .replace("{players_online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("{players_max}", String.valueOf(Bukkit.getMaxPlayers()))
                .replace("{time}", formattedDate);
    }

    private String getPlayerPrefix(Player player) {
        try {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider == null) return "";
            User user = provider.getProvider().getUserManager().getUser(player.getUniqueId());
            if (user == null) return "";
            String prefix = user.getCachedData().getMetaData().getPrefix();
            return prefix != null ? removeMinecraftFormatting(prefix) : "";
        } catch (Exception e) {
            return "";
        }
    }

    public static String removeMinecraftFormatting(String text) {
        if (text == null) return "";
        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");
        return text;
    }
}