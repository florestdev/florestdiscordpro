package ru.florestdev.florestDiscordPro;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CommandHandler implements CommandExecutor {

    private final FlorestDiscordPro plugin;
    private final Methods methods;

    public CommandHandler(FlorestDiscordPro plugin, Methods methods) {
        this.plugin = plugin;
        this.methods = methods;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cИспользование: /fdp [reload/ban/unban/mute]");
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "reload":
                plugin.reloadConfig();
                sender.sendMessage("§a[FlorestDiscord] Конфигурация успешно перезагружена!");
                break;

            case "ban":
                if (args.length >= 2) {
                    String reason = (args.length > 2) ? args[2] : "Banned via Minecraft";
                    methods.banDiscordUser(args[1], reason);
                    sender.sendMessage("§aЗапрос на бан пользователя " + args[1] + " отправлен.");
                } else {
                    sender.sendMessage("§cИспользование: /fdp ban <DiscordID> [причина]");
                }
                break;

            case "unban":
                if (args.length == 2) {
                    methods.unbanDiscordUser(args[1]);
                    sender.sendMessage("§aЗапрос на разбан пользователя " + args[1] + " отправлен.");
                } else {
                    sender.sendMessage("§cИспользование: /fdp unban <DiscordID>");
                }
                break;

            case "mute":
                // В Discord mute часто делается через таймаут (на время)
                if (args.length >= 2) {
                    int minutes = (args.length > 2) ? Integer.parseInt(args[2]) : 60;
                    methods.muteDiscordUser(args[1], minutes);
                    sender.sendMessage("§aПользователь " + args[1] + " отправлен в таймаут на " + minutes + " мин.");
                } else {
                    sender.sendMessage("§cИспользование: /fdp mute <DiscordID> [минуты]");
                }
                break;

            default:
                sender.sendMessage("§cНеизвестная подкоманда. Доступно: reload, ban, unban, mute.");
                break;
        }

        return true;
    }
}