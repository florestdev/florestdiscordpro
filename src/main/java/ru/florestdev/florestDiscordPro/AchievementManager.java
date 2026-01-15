package ru.florestdev.florestDiscordPro;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import org.bukkit.Bukkit;
import org.bukkit.advancement.AdvancementDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class AchievementManager implements Listener {

    private final FlorestDiscordPro plugin;
    private final Methods methods;

    private static final Pattern HEX_GRADIENT_PATTERN = Pattern.compile("(?i)&#[0-9A-F]{6}");
    private static final Pattern MC_FORMAT_PATTERN = Pattern.compile("(?i)[§&][0-9A-FK-OR]");

    public AchievementManager(FlorestDiscordPro plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    @EventHandler
    public void onAdvancementDone(PlayerAdvancementDoneEvent event) {
        // Пропускаем "рецепты" и прочую невидимую фигню
        if (event.getAdvancement().getDisplay() == null) return;

        Player player = event.getPlayer();
        String advancementTitle = Optional.ofNullable(event.getAdvancement().getDisplay())
                .map(display -> display.getTitle())
                .orElse("Unknown Advancement");

        String channelId = plugin.getConfig().getString("discord_channel_id");
        String message = plugin.getConfig().getString("format_advancements", "🏆 {prefix}{user} получил достижение: {advancement}")
                .replace("{user}", player.getName())
                .replace("{advancement}", advancementTitle);

        // Обработка префикса через LuckPerms
        if (plugin.getConfig().getBoolean("support_prefix")) {
            message = message.replace("{prefix}", getPlayerPrefix(player));
        } else {
            message = message.replace("{prefix}", "");
        }

        // Отправляем в Discord через обновленный метод
        methods.sendDiscordMessage(channelId, message);
    }

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
        text = HEX_GRADIENT_PATTERN.matcher(text).replaceAll("");
        text = MC_FORMAT_PATTERN.matcher(text).replaceAll("");
        return text;
    }
}