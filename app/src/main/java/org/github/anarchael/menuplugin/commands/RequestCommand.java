package org.github.anarchael.menuplugin.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.github.anarchael.menuplugin.Main;
import org.github.anarchael.menuplugin.utils.ConfigUtil;

import net.md_5.bungee.api.ChatColor;

public class RequestCommand implements Listener, CommandExecutor {

    ConfigUtil requestConfig;
    private String inventoryName = ChatColor.translateAlternateColorCodes('&', "&6&bRequêtes");
    private String deleteMenuName = ChatColor.translateAlternateColorCodes('&', "&4Supprimer la requête ?");

    public RequestCommand(Main plugin) {
        requestConfig = new ConfigUtil(plugin, "requests.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals(inventoryName)) {
            Player player = (Player) event.getWhoClicked();
            SkullMeta playerHeadClicked = (SkullMeta) event.getCurrentItem().getItemMeta();
            if (player.getUniqueId().equals(playerHeadClicked.getOwnerProfile().getUniqueId())) {
                openDeleteMenu(player);
            }
            event.setCancelled(true);
        }
        if (event.getView().getTitle().equals(deleteMenuName)) {
            Player player = (Player) event.getWhoClicked();
            int slot = event.getSlot();

            if (slot == 3) {
                deleteQuest(player);
                player.closeInventory();
            }
            if (slot == 5) {
                openQuestInventory(player);
            }

            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args[0].equalsIgnoreCase("add") && sender instanceof Player player) {
            if (requestConfig.getConfig().contains("requests." + player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&cVous avez déjà une quête soumise. Faîtes &4/request del &cpour la supprimer."));
            } else {
                Material requestedItemMaterial = Material.matchMaterial(args[1]);
                if (requestedItemMaterial == null)
                    requestedItemMaterial = Material.BARRIER;
                ItemStack requestedItem = new ItemStack(requestedItemMaterial);
                int amount = Integer.parseInt(args[2]);

                registerQuest(player, requestedItem, amount);

            }
        }
        if (args[0].equalsIgnoreCase("del") && sender instanceof Player player) {
            deleteQuest(player);
        }
        if (args[0].equalsIgnoreCase("list") && sender instanceof Player player
                && requestConfig.getConfig().contains("requests")) {
            openQuestInventory(player);
        }

        return true;
    }

    private ItemStack getPlayerHead(String uuidString) {
        Player player = Bukkit.getPlayer(UUID.fromString(uuidString));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Requête de &6" + player.getName()));
        meta.setMaxStackSize(1);

        ItemStack questItem = requestConfig.getConfig().getItemStack("requests." + uuidString + ".item");
        List<String> lore = new ArrayList<>();
        lore.add("Objet recherché: " + questItem.getType().name());
        lore.add("x" + requestConfig.getConfig().get("requests." + uuidString + ".amount"));
        meta.setLore(lore);
        meta.setOwnerProfile(player.getPlayerProfile());
        item.setItemMeta(meta);

        return item;
    }

    private void registerQuest(Player player, ItemStack requestedItem, int amount) {
        String uuidString = player.getUniqueId().toString();
        requestConfig.getConfig().set("requests." + uuidString + ".item", requestedItem);
        requestConfig.getConfig().set("requests." + uuidString + ".amount", amount);
        requestConfig.save();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Requête soumise avec succès."));
    }

    private void deleteQuest(Player player) {
        String uuidString = player.getUniqueId().toString();
        if (!(requestConfig.getConfig().contains("requests." + uuidString))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cVous n'avez pas encore soumis de requête. Faîtes &4/request add <material> <amount> &cpour en soumettre une."));
        } else {
            requestConfig.getConfig().set("requests." + uuidString, null);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Requête supprimée avec succès."));
        }
    }

    private void openQuestInventory(Player player) {
        Set<String> keys = requestConfig.getConfig().getConfigurationSection("requests").getKeys(false);
        Inventory inventory = Bukkit.createInventory(player, 9 * 3, inventoryName);

        for (String s : keys) {
            inventory.addItem(getPlayerHead(s));
        }

        player.openInventory(inventory);
    }

    private void openDeleteMenu(Player player) {
        Inventory deleteMenu = Bukkit.createInventory(player, 9, deleteMenuName);

        ItemStack yesButton = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta yesButtonMeta = yesButton.getItemMeta();

        yesButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Supprimer"));

        yesButton.setItemMeta(yesButtonMeta);

        ItemStack noButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta noButtonMeta = noButton.getItemMeta();
        noButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Ne pas supprimer"));

        noButton.setItemMeta(noButtonMeta);

        deleteMenu.setItem(3, yesButton);
        deleteMenu.setItem(5, noButton);

        player.openInventory(deleteMenu);
    }

}
