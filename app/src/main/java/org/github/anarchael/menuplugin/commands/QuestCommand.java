package org.github.anarchael.menuplugin.commands;

import java.util.ArrayList;
import java.util.Collection;
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
import org.github.anarchael.menuplugin.Main;
import org.github.anarchael.menuplugin.utils.ClassManipulationUtils;
import org.github.anarchael.menuplugin.utils.ConfigUtil;

import net.md_5.bungee.api.ChatColor;

public class QuestCommand implements CommandExecutor, Listener {

    ConfigUtil questConfig;
    private String questMenuName = ChatColor.translateAlternateColorCodes('&', "&6Quêtes - Quêtes disponibles");
    private String choiceMenuName = ChatColor.translateAlternateColorCodes('&', "&6Quêtes - Choisissez une option");
    private String playerQuestsMenuName = ChatColor.translateAlternateColorCodes('&', "&6Quêtes - Vos quêtes");
    private String deleteMenuName = ChatColor.translateAlternateColorCodes('&',
            "&6Quêtes - Renoncer à cette quête ?");
    private UUID pendingQuestDelete = null;

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getView().getTitle().equals(choiceMenuName)) {
            if (event.getSlot() == 1) {
                openQuestMenu(player);
            }
            if (event.getSlot() == 7) {
                openPlayerQuestsMenu(player);
            }
            event.setCancelled(true);
        }
        if (event.getView().getTitle().equals(questMenuName)) {
            Set<String> quests = questConfig.getConfig().getConfigurationSection("quests").getKeys(false);
            String currentItemName = event.getCurrentItem().getItemMeta().getDisplayName();

            for (String uuid : quests) {
                if (questConfig.getConfig().getString(String.format("quests.%s.name", uuid)).equals(currentItemName)) {
                    takeQuest(player, uuid);
                }
            }
            event.setCancelled(true);
        }
        if (event.getView().getTitle().equals(playerQuestsMenuName)) {
            int slot = event.getSlot();
            if (slot > 2 && slot < 6) {
                ItemStack item = event.getCurrentItem();
                Set<String> quests = questConfig.getConfig().getConfigurationSection("quests").getKeys(false);
                for (String s : quests) {
                    if (questConfig.getConfig().getString(String.format("quests.%s.name", s))
                            .equals(item.getItemMeta().getDisplayName())) {
                        pendingQuestDelete = UUID.fromString(s);
                        openDeleteMenu(player);
                        break;
                    }
                }
            }
            event.setCancelled(true);
        }
        if (event.getView().getTitle().equals(deleteMenuName)) {
            switch (event.getSlot()) {
                case 3:
                    deletePlayerQuest(pendingQuestDelete, player);
                    openPlayerQuestsMenu(player);
                    break;
                case 5:
                    openPlayerQuestsMenu(player);
                    break;
                default:
                    break;
            }
            event.setCancelled(true);
        }
    }

    public QuestCommand(Main plugin) {
        questConfig = new ConfigUtil(plugin, "quests.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&4Veuillez utiliser les mots-clés &6add&4, &6del&4, ou &6list&4."));
        } else {
            if (args[0].equalsIgnoreCase("add")) {
                Material itemMaterial = Material.matchMaterial(args[1]);
                if (itemMaterial == null)
                    itemMaterial = Material.BARRIER;
                int itemAmount = Integer.parseInt(args[2]);
                int lvlAmount = Integer.parseInt(args[3]);
                String questName = args[4];

                addQuest(sender, itemMaterial, itemAmount, lvlAmount, questName);
            }
            if (args[0].equalsIgnoreCase("del") || args[0].equalsIgnoreCase("delete")) {
                deleteQuest(args[1]);
            }
            if (args[0].equalsIgnoreCase("list")) {
                if (sender instanceof Player player) {
                    openChoiceMenu(player);
                } else {
                    Set<String> quests = questConfig.getConfig().getConfigurationSection("quests").getKeys(false);

                    for (String s : quests) {
                        Bukkit.getLogger().info(questConfig.getConfig().getString(String.format("quests.%s.name", s)));
                    }
                }
            }
        }
        return true;
    }

    public void addQuest(CommandSender sender, Material itemMaterial, int amount, int lvlReward, String questName) {
        if (lvlReward > 0 && lvlReward <= 50) {
            ItemStack questItem = new ItemStack(itemMaterial);
            ItemMeta questItemMeta = questItem.getItemMeta();
            String uuidString = UUID.randomUUID().toString();

            questItemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', questName));

            List<String> lore = new ArrayList<>();

            lore.add(ChatColor.translateAlternateColorCodes('&',
                    String.format("&dObjet recherché : &6%s", itemMaterial.name())));
            lore.add(ChatColor.translateAlternateColorCodes('&', String.format("&dQuantité demandée : &6%d", amount)));
            lore.add(ChatColor.translateAlternateColorCodes('&',
                    String.format("&dRécompense : &6%d &dniveaux d'expérience", lvlReward)));
            questItemMeta.setLore(lore);

            questItem.setItemMeta(questItemMeta);

            String questPrefix = String.format("quests.%s.", uuidString);
            // Save quest info into config
            questConfig.getConfig().set(questPrefix + "item", questItem);
            questConfig.getConfig().set(questPrefix + "name", questName);
            questConfig.getConfig().set(questPrefix + "amount", amount);
            questConfig.getConfig().set(questPrefix + "reward", lvlReward);
            questConfig.save();
        } else {
            if (sender instanceof Player player)
                player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                        "&cLa quête n'a pas pu être enregistrée, la récompense doit être comprise entre &61 &cet &650 &cniveaux d'expérience."));
        }
    }

    public boolean deleteQuest(String questName) {
        Set<String> quests = questConfig.getConfig().getConfigurationSection("quests").getKeys(false);
        for (String s : quests) {
            if (questConfig.getConfig().getString(String.format("quests.%s.name", s)).equals(questName)) {
                questConfig.getConfig().set("quests." + s, null);
                deletePlayerQuest(UUID.fromString(s));
                questConfig.save();
                return true;
            }
        }
        return false;
    }

    public void deletePlayerQuest(UUID questUuid, Player player) {
        List<String> playerQuests = ClassManipulationUtils.castList(String.class, questConfig.getConfig()
                .getList(String.format("players.%s.taken-quest", player.getUniqueId().toString())));
        playerQuests.remove(questUuid.toString());
        questConfig.getConfig().set(String.format("players.%s.taken-quest", player.getUniqueId().toString()),
                playerQuests);
        questConfig.save();
    }

    public void deletePlayerQuest(UUID questUuid) {
        Set<String> players = questConfig.getConfig().getConfigurationSection("players").getKeys(false);

        for (String uuid : players) {
            List<String> playerQuests = ClassManipulationUtils.castList(String.class, questConfig.getConfig()
                    .getList(String.format("players.%s.taken-quest", uuid)));
            if (playerQuests.contains(uuid)) {
                playerQuests.remove(questUuid.toString());
                questConfig.getConfig().set(String.format("players.%s.taken-quest", uuid), playerQuests);
                questConfig.save();
            }
        }
    }

    public boolean deleteQuest(UUID questUuid) {
        questConfig.getConfig().set("quests." + questUuid.toString(), null);
        questConfig.save();
        return false;
    }

    // Menu definitions

    public void openQuestMenu(Player player) {
        Set<String> quests = questConfig.getConfig().getConfigurationSection("quests").getKeys(false);

        Inventory questMenu = Bukkit.createInventory(player, 9 * 3,
                ChatColor.translateAlternateColorCodes('&', questMenuName));

        addItemsToInventory(quests, questMenu);

        player.openInventory(questMenu);
    }

    public void openChoiceMenu(Player player) {

        Inventory choiceMenu = Bukkit.createInventory(player, 9,
                ChatColor.translateAlternateColorCodes('&', choiceMenuName));

        ItemStack listButton = new ItemStack(Material.BIRCH_SIGN);
        ItemMeta listButtonMeta = listButton.getItemMeta();
        listButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Liste des quêtes"));
        listButton.setItemMeta(listButtonMeta);

        ItemStack playerQuestButton = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta playerQuestMeta = playerQuestButton.getItemMeta();
        playerQuestMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Vos quêtes en cours"));
        playerQuestButton.setItemMeta(playerQuestMeta);

        choiceMenu.setItem(1, listButton);
        choiceMenu.setItem(7, playerQuestButton);

        player.openInventory(choiceMenu);
    }

    public void openPlayerQuestsMenu(Player player) {
        Inventory playerQuestsMenu = Bukkit.createInventory(player, 9 * 1, playerQuestsMenuName);
        String playerUuidString = player.getUniqueId().toString();
        List<String> playerQuests = ClassManipulationUtils.castList(String.class,
                questConfig.getConfig().getList(String.format("players.%s.taken-quest", playerUuidString)));

        for (int i = 0; i < playerQuestsMenu.getSize(); i++) {
            if (i < 3 || i > 5) {
                ItemStack blockade = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
                ItemMeta blockadeMeta = blockade.getItemMeta();
                blockadeMeta.setDisplayName("");
                blockade.setItemMeta(blockadeMeta);

                playerQuestsMenu.setItem(i, blockade);
            }
        }

        addItemsToInventory(playerQuests, playerQuestsMenu);

        player.openInventory(playerQuestsMenu);
    }

    public void openDeleteMenu(Player player) {
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

    // Functions called in menu definitions

    public void takeQuest(Player player, String questUuidString) {
        String playerUuidString = player.getUniqueId().toString();
        int takenQuestAmount = questConfig.getConfig().getInt("players." + playerUuidString + ".taken");
        List<?> playerQuests = questConfig.getConfig().getList("players." + playerUuidString + ".taken-quest");
        List<String> takenQuest = ClassManipulationUtils.castList(String.class, playerQuests);
        boolean isUnique = true;
        for (String s : takenQuest) {
            if (s.equals(questUuidString)) {
                isUnique = false;
            }
        }

        if (takenQuestAmount < 3) {
            if (isUnique) {
                takenQuest.add(questUuidString);
                takenQuestAmount = takenQuest.size();
                questConfig.getConfig().set("players." + playerUuidString + ".taken", takenQuestAmount);
                questConfig.getConfig().set("players." + playerUuidString + ".taken-quest", takenQuest);
                questConfig.save();
            } else {
                player.sendMessage("Vous ne pouvez pas prendre deux fois la même quête !");
            }
        } else {
            player.sendMessage("Vous ne pouvez prendre que 3 quêtes simultanément !");
        }
    }

    public void addItemsToInventory(Collection<String> collection, Inventory inventory) {
        for (String s : collection) {
            ItemStack item = questConfig.getConfig().getItemStack(String.format("quests.%s.item", s));

            inventory.addItem(item);
        }
    }

}
