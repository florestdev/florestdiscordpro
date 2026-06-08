package ru.florestdev.florestDiscordPro;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.user.UserTypingEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DiscordListener extends ListenerAdapter {

    private final FlorestDiscordPro plugin;

    public DiscordListener(FlorestDiscordPro plugin) {
        this.plugin = plugin;
    }
    Runtime runtime = Runtime.getRuntime();
    private final Map<String, Long> typingCooldown = new ConcurrentHashMap<>();
    private final long COOLDOWN_MS = 10000; // 10 секунд

    // Обработка новых сообщений (вместо handleMessage)
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent event) {
        // Игнорируем сообщения от ботов (и от самого себя)
        if (event.getAuthor().isBot()) return;

        // Проверяем, тот ли это канал
        if (!event.getChannel().getId().equals(plugin.getConfig().getString("discord_channel_id"))) return;

        String messageText = event.getMessage().getContentRaw();
        String userTag = event.getAuthor().getName();

        // 1. Если это команда для игрового сервера (начинается с /)
        if (messageText.startsWith("/")) {
            handleDiscordCommand(event, messageText);
            return;
        }

        // 2. Если это обычное сообщение — пересылаем в Minecraft
        String ignorePrefix = plugin.getConfig().getString("restrictions.prefix_ignore_discord", "!");
        if (messageText.startsWith(ignorePrefix)) return;

        String format = plugin.getConfig().getString("minecraft_discord_format", "§9[Discord] §f{discord_name}: {discord_message}");
        String formatted = format
                .replace("{discord_name}", userTag)
                .replace("{discord_message}", messageText);

        // Отправляем в чат Minecraft (синхронно)
        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.broadcastMessage(formatted)
        );
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        // Игнорируем ботов
        if (event.getUser() == null || event.getUser().isBot()) return;

        // Проверяем канал
        if (!event.getChannel().getId().equals(plugin.getConfig().getString("discord_channel_id")))
            return;

        String messageId = event.getMessageId();
        String reactorName = event.getUser().getName();

        // Определяем emoji
        String emoji;
        if (event.getReaction().getEmoji().getType().name().equals("UNICODE")) {
            emoji = event.getReaction().getEmoji().asUnicode().getName();
        } else {
            emoji = ":" + event.getReaction().getEmoji().asCustom().getName() + ":";
        }

        String format = plugin.getConfig().getString("minecraft_discord_reaction_received");

        event.retrieveMessage().queue(message -> {
            String formatted = format
                    .replace("{discord_name}", reactorName)
                    .replace("{reaction}", emoji)
                    .replace("{author}", message.getAuthor().getName())
                    .replace("{message}", message.getContentRaw());

            Bukkit.getScheduler().runTask(plugin, () ->
                    Bukkit.broadcastMessage(formatted)
            );
        });

    }

    @Override
    public void onUserTyping(UserTypingEvent event) {
        if (event.getUser().isBot()) return;

        String requiredChannel = plugin.getConfig().getString("discord_channel_id");
        if (!event.getChannel().getId().equals(requiredChannel)) return;

        // Антиспам
        String userId = event.getUser().getId();
        if (typingCooldown.getOrDefault(userId, 0L) + 10000 > System.currentTimeMillis()) return;
        typingCooldown.put(userId, System.currentTimeMillis());

        String discordName = event.getUser().getName();

        Bukkit.getScheduler().runTask(plugin, () -> {
            String format = plugin.getConfig().getString("minecraft_discord_typing_format");
            String message = format.replace("{discord_name}", discordName);
            String colored = ChatColor.translateAlternateColorCodes('&', message);
            Bukkit.broadcastMessage(colored);
        });
    }

    // Обработка редактирования (вместо handleEdited)
    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;

        String newText = event.getMessage().getContentRaw();
        String userTag = event.getAuthor().getName();

        String format = "§9[Discord] §7(ред.) §f{discord_name}: {discord_message}";
        String formatted = format
                .replace("{discord_name}", userTag)
                .replace("{discord_message}", newText);

        Bukkit.getScheduler().runTask(plugin, () ->
                Bukkit.broadcastMessage(formatted)
        );
    }

    private void handleDiscordCommand(MessageReceivedEvent event, String command) {
        Member member = event.getMember();
        if (member == null) return;

        // Обработка встроенной команды /players
        if (command.equalsIgnoreCase("/players")) {
            // 1. Получаем список имен
            String playersList = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.joining(", "));

            // 2. Достаем строки из конфига (с дефолтными значениями, если конфиг пуст)
            String format = plugin.getConfig().getString("players-format", "📊 **Online Players ({online}/{max}):**\n`{list}`");
            String noPlayers = plugin.getConfig().getString("no-players", "Nobody is online");

            // 3. Формируем финальный список
            String finalList = playersList.isEmpty() ? noPlayers : playersList;

            // 4. Заменяем плейсхолдеры в формате
            String message = format
                    .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
                    .replace("{max}", String.valueOf(Bukkit.getMaxPlayers()))
                    .replace("{list}", finalList);

            // 5. Отправляем в Discord
            event.getChannel().sendMessage(message).queue();
            return;
        }

        if (command.equalsIgnoreCase("/tps")) {
            if (plugin.essentials == null) {
                plugin.getMethods().sendDiscordMessage(event.getChannel().getId(), "Server haven't got the EssentialsX plugin for this feature. Please install!");
                return;
            } else {
                double currentTps = plugin.essentials.getTimer().getAverageTPS();
                long maxMemory = runtime.maxMemory() / 1024 / 1024;
                long freeMemory = runtime.freeMemory() / 1024 / 1024;

                // Вычисляем реально используемую память
                long usedMemory = maxMemory - freeMemory;
                plugin.getMethods().sendDiscordMessage(plugin.getConfig().getString("discord_channel_id"), plugin.getConfig().getString("tps_message").replace("{tps}", String.valueOf(currentTps)).replace("{ram_usage}", String.valueOf(usedMemory)).replace("{ram_maximum}", String.valueOf(maxMemory)));
                return;
            }
        }

        // Проверка: включены ли команды вообще
        if (!plugin.getConfig().getBoolean("commands.enabled", true)) return;

        // ПРОВЕРКА РОЛИ (то, что вы спрашивали):
        if (plugin.getConfig().getBoolean("commands.require_role", false)) {
            String adminRoleId = plugin.getConfig().getString("commands.admin_role_id");

            boolean hasRole = member != null && member.getRoles().stream()
                    .anyMatch(role -> role.getId().equals(adminRoleId));

            if (!hasRole && !plugin.getConfig().getString("commands.plus_admin").contains(member.getId()) && !member.hasPermission(Permission.ADMINISTRATOR)) {
                String noPermMsg = plugin.getConfig().getString("commands.no_permission", "{user}, нет прав!")
                        .replace("{user}", event.getAuthor().getAsMention());
                event.getChannel().sendMessage(noPermMsg).queue();
                return; // Прекращаем выполнение
            }
        }

        // Проверка черного списка команд из конфига
        String baseCommand = command.split(" ")[0].toLowerCase();
        List<String> blacklist = plugin.getConfig().getStringList("commands.blacklist");

        if (blacklist.contains(baseCommand) || blacklist.contains("all")) {
            event.getChannel().sendMessage("🚫 Эта команда запрещена в конфиге.").queue();
            return;
        }

        // Выполнение команды в консоли сервера
        String cleanCommand = command.substring(1); // убираем /
        Bukkit.getScheduler().runTask(plugin, () -> {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cleanCommand);
            event.getChannel().sendMessage("✅ Команда `" + cleanCommand + "` отправлена в консоль.").queue();
        });
    }
}