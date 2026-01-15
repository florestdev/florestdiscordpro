package ru.florestdev.florestDiscordPro;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.plugin.java.JavaPlugin;
public class DiscordManager {

    private final JavaPlugin plugin;
    private JDA jda;
    private final String token;

    public DiscordManager(JavaPlugin plugin, String token) {
        this.plugin = plugin;
        this.token = token;
    }

    public void start() {
        try {
            JDABuilder builder = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT)
                    .setStatus(OnlineStatus.valueOf(plugin.getConfig().getString("bot_status.status_type", "ONLINE").toUpperCase()))
                    .setActivity(Activity.playing(plugin.getConfig().getString("bot_status.activity_text", "FlorestWorld").replace("{online}", String.valueOf(plugin.getServer().getOnlinePlayers().size()))))

                    // ВОТ ТУТ ГЛАВНОЕ ИЗМЕНЕНИЕ:
                    .addEventListeners(new DiscordListener((FlorestDiscordPro) plugin));

            jda = builder.build();
            jda.awaitReady(); // Рекомендую добавить, чтобы бот успел прогрузиться
            plugin.getLogger().info("FlorestDiscordPRO: Бот успешно авторизован!");

        } catch (Exception e) {
            plugin.getLogger().severe("Ошибка при запуске Discord бота: " + e.getMessage());
        }
    }

    public void stop() {
        if (jda != null) {
            jda.shutdownNow(); // Мгновенно выключаем бота при выключении сервера
        }
    }

    public JDA getJda() {
        return jda;
    }
}