package org.github.anarchael.requestplugin.utils;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.github.anarchael.requestplugin.Main;

public class ConfigUtil {

    private File file;
    private FileConfiguration config;

    public ConfigUtil(Main plugin, String path) {
        this(plugin.getDataFolder().getAbsolutePath() + "/" + path);
    }

    public ConfigUtil(String path) {
        this.file = new File(path);
        this.config = YamlConfiguration.loadConfiguration(file);
    }

    public boolean save() {
        try {
            this.config.save(this.file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public File getFile() {
        return file;
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
