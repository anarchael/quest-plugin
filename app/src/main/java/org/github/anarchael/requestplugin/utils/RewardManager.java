package org.github.anarchael.requestplugin.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.github.anarchael.requestplugin.Main;

public final class RewardManager {

    private static Main plugin = (Main) Bukkit.getPluginManager().getPlugin("Re-Quest");
    private static ConfigUtil rewardsConfig = new ConfigUtil(plugin, "rewards.yml");

    private RewardManager() {
    }

    public static void addReward(String uuidString, ItemStack item) {
        rewardsConfig.getConfig().set(uuidString + ".item-reward-1", item);
        rewardsConfig.save();
    }

    public static void addRewards(String uuidString, Collection<?> rawItems) {
        List<ItemStack> items = ClassManipulationUtils.castList(ItemStack.class, rawItems);
        int counter = 0;
        for (ItemStack item : items) {
            counter++;
            rewardsConfig.getConfig().set(uuidString + ".item-reward-" + counter, item);
        }
        rewardsConfig.save();
    }

    public static ItemStack getReward(String uuidString) {
        return rewardsConfig.getConfig().getItemStack(uuidString + ".item-reward");
    }

    public static List<ItemStack> getRewards(String uuidString) {
        List<ItemStack> rewards = new ArrayList<>();
        Set<String> rewardsPaths = rewardsConfig.getConfig().getConfigurationSection(uuidString).getKeys(false);
        for (String path : rewardsPaths) {
            rewards.add(rewardsConfig.getConfig().getItemStack(uuidString + "." + path));
        }
        Bukkit.getLogger().warning(rewards.toString());
        return rewards;
    }

    public static void deleteReward(String uuidString) {
        rewardsConfig.getConfig().set(uuidString, null);
        rewardsConfig.save();
    }

    public static void sendRewardToInbox(Player player, String uuidString) {
        ItemStack reward = rewardsConfig.getConfig().getItemStack(uuidString + ".item-reward");
        String playerUuidString = player.getUniqueId().toString();
        ConfigUtil playerConfig = new ConfigUtil(plugin, "players/" + player.getUniqueId().toString() + ".yml");
        List<ItemStack> inboxItems = ClassManipulationUtils.castList(ItemStack.class,
                playerConfig.getConfig().getList(playerUuidString + ".inbox"));
        inboxItems.add(reward);
        playerConfig.getConfig().set(playerUuidString + ".inbox", inboxItems);
    }
}