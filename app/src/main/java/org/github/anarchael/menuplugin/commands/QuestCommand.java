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

public class QuestCommand implements Listener, CommandExecutor {

    ConfigUtil questConfig;
    private String inventoryName = ChatColor.translateAlternateColorCodes('&', "&6&bQuêtes");
    private String deleteMenuName = ChatColor.translateAlternateColorCodes('&', "&4Supprimer la quête ?");

    public QuestCommand(Main plugin) {
        questConfig = new ConfigUtil(plugin, "quests.yml");
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
            if (questConfig.getConfig().contains("quests." + player.getUniqueId().toString())) {
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&cVous avez déjà une quête soumise. Faîtes &4/quest del &cpour la supprimer."));
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
                && questConfig.getConfig().contains("quests")) {
            openQuestInventory(player);
        }

        return true;
    }

    private ItemStack getPlayerHead(String uuidString) {
        Player player = Bukkit.getPlayer(UUID.fromString(uuidString));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Quête de &4" + player.getName()));
        meta.setMaxStackSize(1);

        ItemStack questItem = questConfig.getConfig().getItemStack("quests." + uuidString + ".item");
        List<String> lore = new ArrayList<>();
        lore.add("Objet recherché: " + questItem.getType().name());
        lore.add("x" + questConfig.getConfig().get("quests." + uuidString + ".amount"));
        meta.setLore(lore);
        meta.setOwnerProfile(player.getPlayerProfile());
        item.setItemMeta(meta);

        return item;
    }

    private void registerQuest(Player player, ItemStack requestedItem, int amount) {
        String uuidString = player.getUniqueId().toString();
        questConfig.getConfig().set("quests." + uuidString + ".item", requestedItem);
        questConfig.getConfig().set("quests." + uuidString + ".amount", amount);
        questConfig.save();
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Quête soumise avec succès."));
    }

    private void deleteQuest(Player player) {
        String uuidString = player.getUniqueId().toString();
        if (!(questConfig.getConfig().contains("quests." + uuidString))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cVous n'avez pas encore soumis de quête. Faîtes &4/quest add <material> <amount> &cpour en soumettre une."));
        } else {
            questConfig.getConfig().set("quests." + uuidString, null);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Quête supprimée avec succès."));
        }
    }

    private void openQuestInventory(Player player) {
        Set<String> keys = questConfig.getConfig().getConfigurationSection("quests").getKeys(false);
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
