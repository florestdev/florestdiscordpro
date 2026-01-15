package ru.florestdev.florestDiscordPro;

import com.earth2me.essentials.Essentials;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class FlorestDiscordPro extends JavaPlugin {

    private Methods methods;
    private DiscordManager discordManager;

    public static Essentials essentials;

    @Override
    public void onEnable() {
        // 1. Сначала сохраняем/грузим конфиг
        saveDefaultConfig();

        // 2. Инициализация менеджеров
        this.methods = new Methods(this);
        String token = getConfig().getString("discord_bot_token");

        if (token == null || token.isEmpty() || token.equals("your-token-here")) {
            getLogger().severe("Токен Discord не указан в конфиге! Плагин выключается.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Запускаем Дискорд бота и сохраняем в поле класса
        this.discordManager = new DiscordManager(this, token);
        this.discordManager.start();

        // 3. Регистрация событий и команд
        getServer().getPluginManager().registerEvents(new ChatListener(this, methods), this);

        PlayerTracker tracker = new PlayerTracker(this, methods);
        tracker.register();

        if (getConfig().getBoolean("command_tracking.enabled")) {
            new CommandTracker(this, methods).register();
        }

        // Регистрация новой команды /florestdiscord (из твоего нового plugin.yml)
        PluginCommand mainCommand = getCommand("florestdiscord");
        if (mainCommand != null) {
            mainCommand.setExecutor(new CommandHandler(this, methods));
        }

        if (getConfig().getBoolean("enable_advancements")) {
            getServer().getPluginManager().registerEvents(new AchievementManager(this, methods), this);
        }

        if (getConfig().getBoolean("enable_tps_tracking")) {
            TPSListener tpsListener = new TPSListener(this);
            tpsListener.startTask();
        }

        // 4. Проверка LuckPerms
        if (getConfig().getBoolean("support_prefix")) {
            if (getServer().getPluginManager().getPlugin("LuckPerms") == null) {
                getLogger().warning("LuckPerms не найден! Функция префиксов будет работать некорректно.");
            }
        }

        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");

        // 5. Сообщение о запуске (теперь через методы Discord)
        String channelId = getConfig().getString("discord_channel_id");
        String startMsg = getConfig().getString("hello_message");
        methods.sendDiscordMessage(channelId, startMsg);

        getLogger().info("FlorestDiscordPRO успешно запущен и готов к работе!");
    }

    @Override
    public void onDisable() {
        if (discordManager != null && discordManager.getJda() != null) {
            String channelId = getConfig().getString("discord_channel_id");
            String byeMsg = getConfig().getString("goodbye_message", "🛑 Сервер выключен!")
                    .replace("{was_players}", String.valueOf(getServer().getOnlinePlayers().size()));

            try {
                var channel = discordManager.getJda().getTextChannelById(channelId);
                if (channel != null) {
                    // КЛЮЧЕВОЙ МОМЕНТ: .complete() вместо .queue()
                    // Это заставит текущий поток ждать завершения отправки
                    channel.sendMessage(byeMsg).complete();
                }
            } catch (Exception e) {
                getLogger().warning("Не удалось отправить сообщение при выключении: " + e.getMessage());
            }

            // Останавливаем бота аккуратно
            discordManager.stop();
        }

        getLogger().info("FlorestDiscordPRO выключен. Пока-пока!");
    }

    // Геттеры, чтобы другие классы могли достучаться до менеджеров
    public DiscordManager getDiscordManager() {
        return discordManager;
    }

    public Methods getMethods() {
        return methods;
    }
}