package org.github.anarchael.menuplugin;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.github.anarchael.menuplugin.commands.QuestCommand;

import net.md_5.bungee.api.ChatColor;

public class Main extends JavaPlugin implements Listener {

    public final String pluginName = ChatColor.translateAlternateColorCodes('&',
            String.format("&d[&4%s&d]&r", getName()));

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.setJoinMessage(
                String.format("%2$s Salut à toi, %1$s, et bienvenu-e sur ce serveur !", event.getPlayer().getName(),
                        pluginName));
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        System.out.println("Plugin démarré");
        getServer().getConsoleSender().sendMessage(String.format("Merci d'utiliser le plugin %s !", getName()));
        getCommand("quest").setExecutor(new QuestCommand(this));
    }
}
