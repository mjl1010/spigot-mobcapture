package net.johnbrooks.mh;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class Language {
    private static Language instance = new Language();
    private FileConfiguration lang = null;

    public static String getKey(String key) {
        return getInstance().lang.getString("prefix").replaceAll("&", "§") + getInstance().lang.getString(key).replaceAll("&", "§");
    }

    public static String getKeyWithoutPrefix(String key) {
        return getInstance().lang.getString(key).replaceAll("&", "§");
    }

    public static Language getInstance() {
        if (instance.lang == null) {
            File file = new File(Main.plugin.getDataFolder(), "lang.yml");
            if (!file.exists()) {
                Main.plugin.saveResource("lang.yml", false);
                file = new File(Main.plugin.getDataFolder(), "lang.yml");
            }
            instance.lang = YamlConfiguration.loadConfiguration(file);
        }

        return instance;
    }
}