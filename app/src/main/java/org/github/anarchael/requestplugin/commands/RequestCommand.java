package org.github.anarchael.requestplugin.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import org.github.anarchael.requestplugin.Main;
import org.github.anarchael.requestplugin.utils.ClassManipulationUtils;
import org.github.anarchael.requestplugin.utils.ConfigUtil;
import org.github.anarchael.requestplugin.utils.RewardManager;

import net.md_5.bungee.api.ChatColor;

public class RequestCommand implements Listener, CommandExecutor {

    private Main mainPlugin;
    ConfigUtil requestConfig;
    private String requestMenuName = ChatColor.translateAlternateColorCodes('&', "&6&bRequêtes");
    private String deleteMenuName = ChatColor.translateAlternateColorCodes('&', "&4Supprimer la requête ?");
    private String addRewardsMenuName = ChatColor.translateAlternateColorCodes('&', "&6Choisissez les récompenses.");
    private String takeRequestMenuName = ChatColor.translateAlternateColorCodes('&', "&6Prendre cette requête ?");
    private String manageRequestMenuName = ChatColor.translateAlternateColorCodes('&', "&6Gestion de la requête.");
    private String playerRequestMenuName = ChatColor.translateAlternateColorCodes('&', "&2Vos requêtes");

    private int pendingAmount = 0;
    private String pendingRequest = null;

    public RequestCommand(Main plugin) {
        mainPlugin = plugin;
        requestConfig = new ConfigUtil(plugin, "requests.yml");
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        int clickedSlot = event.getSlot();
        Player player = (Player) event.getWhoClicked();
        clickRequestMenu(event, player);
        clickDeleteMenu(event, player, clickedSlot);
        clickRewardMenu(event, player, clickedSlot);
        clickTakenRequestMenu(event, player, clickedSlot);
        clickManageRequestMenu(event, player, clickedSlot);

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                openManageRequestMenu(player);
            } else {
                if (args[0].equalsIgnoreCase("add")) {
                    Material itemMaterial = Material.matchMaterial(args[1]);
                    if (itemMaterial == null)
                        itemMaterial = Material.BARRIER;
                    int amount = Integer.parseInt(args[2]);

                    pendingAmount = amount;
                    openAddRewardsMenu(player, new ItemStack(itemMaterial));
                }
                if (args[0].equalsIgnoreCase("del")) {
                    deleteQuest(player);
                }
                if (args[0].equalsIgnoreCase("list")) {
                    openQuestInventory(player);
                }
            }
        }
        return true;
    }

    private ItemStack getPlayerHead(String uuidString) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(UUID.fromString(uuidString));
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Requête de&6 " + player.getName()));
        meta.setMaxStackSize(1);

        ItemStack questItem = requestConfig.getConfig().getItemStack("requests." +
                uuidString + ".item");
        List<String> lore = new ArrayList<>();
        lore.add("Objet recherché: " + questItem.getType().name());
        lore.add("x" + requestConfig.getConfig().get("requests." + uuidString +
                ".amount"));
        lore.add("Récompense(s) : ");
        List<ItemStack> rewards = RewardManager.getRewards(uuidString);
        for (ItemStack reward : rewards) {
            lore.add("     -" + reward.getType().name());
        }
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
        openAddRewardsMenu(player, requestedItem);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Requête soumise avec succès."));
    }

    private void deleteQuest(Player player) {
        String uuidString = player.getUniqueId().toString();
        if (!(requestConfig.getConfig().contains("requests." + uuidString))) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&cVous n'avez pas encore soumis de requête. Faîtes &4/request add <material> <amount> &cpour en soumettre une."));
        } else {
            requestConfig.getConfig().set("requests." + uuidString, null);
            requestConfig.save();
            RewardManager.deleteReward(uuidString);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&2Requête supprimée avec succès."));
        }
    }

    // Methods

    private void takeRequest(Player player, String requestUuidString) {
        String playerUuidString = player.getUniqueId().toString();
        ConfigUtil playerTakenQuests = new ConfigUtil(mainPlugin, "players/" + playerUuidString + ".yml");
        List<String> requestList = ClassManipulationUtils.castList(String.class,
                playerTakenQuests.getConfig().getList("requests"));
        boolean isUnique = true;
        for (String s : requestList) {
            if (requestUuidString.equals(s)) {
                isUnique = false;
            }
        }
        if (isUnique) {
            requestList.add(requestUuidString);
            playerTakenQuests.getConfig().set("requests", requestList);
            playerTakenQuests.save();
        } else {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    "&4Vous ne pouvez pas prendre deux fois la même requête"));
        }
    }

    private List<ItemStack> getRewards(Inventory inventory) {
        List<ItemStack> rewards = new ArrayList<>();
        rewards.add(inventory.getItem(0));
        rewards.add(inventory.getItem(1));
        rewards.add(inventory.getItem(2));
        return rewards;
    }

    // Inventories

    private void openAddRewardsMenu(Player player, ItemStack requestedItem) {
        Inventory addRewardsMenu = Bukkit.createInventory(player, 9 * 2, addRewardsMenuName);

        ItemStack placeHolder = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta placeHolderMeta = placeHolder.getItemMeta();
        placeHolderMeta.setMaxStackSize(1);
        placeHolder.setItemMeta(placeHolderMeta);

        ItemStack confirmButton = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta confirmButtonMeta = confirmButton.getItemMeta();

        confirmButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Valider et soumettre"));
        confirmButton.setItemMeta(confirmButtonMeta);

        ItemStack xpButton = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta xpButtonMeta = xpButton.getItemMeta();
        xpButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "Ajouter des niveaux"));
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&4FONCTIONNALITÉ NON IMPLÉMENTÉE"));
        xpButtonMeta.setLore(lore);
        xpButton.setItemMeta(xpButtonMeta);

        for (int i = 3; i < addRewardsMenu.getSize(); i++) {
            switch (i) {
                case 4:
                    addRewardsMenu.setItem(i, requestedItem);
                    break;
                case 7:
                    addRewardsMenu.setItem(i, xpButton);
                    break;
                case 13:
                    addRewardsMenu.setItem(i, confirmButton);
                    break;
                default:
                    addRewardsMenu.setItem(i, placeHolder);
                    break;
            }
        }

        player.openInventory(addRewardsMenu);
    }

    private void openQuestInventory(Player player) {
        Set<String> keys = requestConfig.getConfig().getConfigurationSection("requests").getKeys(false);
        Inventory inventory = Bukkit.createInventory(player, 9 * 3, requestMenuName);

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

    private void openTakeRequestMenu(Player player) {
        Inventory takeRequestMenu = Bukkit.createInventory(player, 9 * 1, takeRequestMenuName);

        ItemStack yesButton = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta yesButtonMeta = yesButton.getItemMeta();

        yesButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Supprimer"));

        yesButton.setItemMeta(yesButtonMeta);

        ItemStack noButton = new ItemStack(Material.RED_CONCRETE);
        ItemMeta noButtonMeta = noButton.getItemMeta();
        noButtonMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Ne pas supprimer"));

        noButton.setItemMeta(noButtonMeta);

        takeRequestMenu.setItem(3, yesButton);
        takeRequestMenu.setItem(5, noButton);
        player.openInventory(takeRequestMenu);
    }

    private void openManageRequestMenu(Player player) {
        Inventory manageRequestMenu = Bukkit.createInventory(player, 9 * 3, manageRequestMenuName);

        ItemStack requestList = new ItemStack(Material.OAK_SIGN);
        ItemMeta requestListMeta = requestList.getItemMeta();
        requestListMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Liste de requêtes"));
        List<String> lores = new ArrayList<>();
        lores.add(ChatColor.translateAlternateColorCodes('&', "&2------------------------------------------"));
        lores.add(ChatColor.translateAlternateColorCodes('&', "&aCliquez pour accéder à la liste de requête"));
        lores.add(ChatColor.translateAlternateColorCodes('&', "&2------------------------------------------"));
        requestListMeta.setLore(lores);
        requestList.setItemMeta(requestListMeta);

        ItemStack playerRequestList = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta playerRequestListMeta = playerRequestList.getItemMeta();
        playerRequestListMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Vos requêtes"));
        lores = new ArrayList<>();
        lores.add(ChatColor.translateAlternateColorCodes('&',
                "&2------------------------------------------------------"));
        lores.add(ChatColor.translateAlternateColorCodes('&',
                "&aCliquez pour accéder aux requêtes que vous avez prises"));
        lores.add(ChatColor.translateAlternateColorCodes('&',
                "&2------------------------------------------------------"));
        playerRequestListMeta.setLore(lores);
        playerRequestList.setItemMeta(playerRequestListMeta);

        manageRequestMenu.setItem(10, requestList);
        manageRequestMenu.setItem(16, playerRequestList);

        player.openInventory(manageRequestMenu);
    }

    private void openPlayerRequestsMenu(Player player) {
        Inventory playerRequestMenu = Bukkit.createInventory(player, 9 * 3, playerRequestMenuName);
        UUID playerUuid = player.getUniqueId();
        ConfigUtil playerConfig = new ConfigUtil(mainPlugin, "players/" + playerUuid.toString() + ".yml");

        List<String> playerRequestList = ClassManipulationUtils.castList(String.class,
                playerConfig.getConfig().getList("requests"));

        for (String uuid : playerRequestList) {
            playerRequestMenu.addItem(getPlayerHead(uuid));
            
        }

        player.openInventory(playerRequestMenu);
    }

    // Manage click inventories

    public void clickRequestMenu(InventoryClickEvent event, Player player) {
        if (event.getView().getTitle().equals(requestMenuName)) {
            SkullMeta playerHeadClicked = (SkullMeta) event.getCurrentItem().getItemMeta();
            if (player.getUniqueId().equals(playerHeadClicked.getOwnerProfile().getUniqueId())) {
                openDeleteMenu(player);
            } else {
                pendingRequest = playerHeadClicked.getOwnerProfile().getUniqueId().toString();
                openTakeRequestMenu(player);
            }
            event.setCancelled(true);
        }
    }

    public void clickDeleteMenu(InventoryClickEvent event, Player player, int slot) {
        if (event.getView().getTitle().equals(deleteMenuName)) {
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

    public void clickRewardMenu(InventoryClickEvent event, Player player, int slot) {
        if (event.getView().getTitle().equals(addRewardsMenuName)
                && event.getClickedInventory().equals(event.getView().getTopInventory())
                && slot != 0 && slot != 1 && slot != 2) {
            if (slot == 13) {
                List<ItemStack> rewards = getRewards(event.getInventory());
                int counter = 0;
                for (ItemStack item : rewards) {
                    if (item == null) {
                        counter++;
                    }
                }
                if (counter == 2) {
                    RewardManager.addReward(player.getUniqueId().toString(), rewards.getFirst());
                    registerQuest(player, event.getView().getTopInventory().getItem(4), pendingAmount);
                    player.closeInventory();
                }
                if (counter < 3 && counter != 2) {
                    RewardManager.addRewards(player.getUniqueId().toString(), rewards);
                    registerQuest(player, event.getView().getTopInventory().getItem(4), pendingAmount);
                    player.closeInventory();
                }
                if (counter >= 3) {
                    player.sendMessage("Vous êtes obligé de mettre un objet en récompense");
                }
            }
            event.setCancelled(true);
        }
    }

    public void clickTakenRequestMenu(InventoryClickEvent event, Player player, int slot) {
        if (event.getView().getTitle().equals(takeRequestMenuName)) {
            if (slot == 3) {
                takeRequest(player, pendingRequest);
                player.closeInventory();
            }
            if (slot == 5) {
                pendingRequest = null;
                openQuestInventory(player);
            }
            event.setCancelled(true);
        }
    }

    public void clickManageRequestMenu(InventoryClickEvent event, Player player, int slot) {
        if (event.getView().getTitle().equals(manageRequestMenuName)) {
            switch (slot) {
                case 10:
                    openQuestInventory(player);
                    break;
                case 16:
                    openPlayerRequestsMenu(player);
                    break;
                default:
                    break;
            }
            event.setCancelled(true);
        }
    }
}
