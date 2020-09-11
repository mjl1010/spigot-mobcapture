package net.johnbrooks.mh.managers;

import net.johnbrooks.mh.Language;
import net.johnbrooks.mh.Main;
import net.johnbrooks.mh.Settings;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public class EconomyManager {

    public static boolean chargePlayer(Player player) {
        switch (Settings.costMode) {
            case ITEM:
                return canChargeItem(player) && chargeItem(player);
            case VAULT:
                return canChargeVault(player) && chargeVault(player);
            case ALL:
                return (canChargeVault(player) && canChargeItem(player)) && (chargeVault(player) && chargeItem(player));
            default:
                return false;
        }
    }

    private static boolean canChargeVault(Player player) {
        return Main.economy.getBalance(player) >= Settings.costVault;
    }

    private static boolean chargeVault(Player player) {
        if (!canChargeVault(player)) return false;
        if (Settings.costVault > 0) {
            player.sendMessage(Language.getKey("chargeVault").replaceAll("%costVault%", String.valueOf(Settings.costVault)));
            Main.economy.withdrawPlayer(player, Settings.costVault);
        }
        return true;
    }

    private static boolean canChargeItem(Player player) {
        Optional<ItemStack> itemStackOptional = Arrays.stream(player.getInventory().getContents())
                .filter(itemStack -> itemStack != null && itemStack.getType().equals(Settings.costMaterial) && itemStack.getAmount() >= Settings.costAmount)
                .findAny();

        return itemStackOptional.isPresent();
    }

    private static boolean chargeItem(Player player) {
        Optional<ItemStack> itemStackOptional = Arrays.stream(player.getInventory().getContents())
                .filter(itemStack -> itemStack != null && itemStack.getType().equals(Settings.costMaterial) && itemStack.getAmount() >= Settings.costAmount)
                .findAny();

        if (!itemStackOptional.isPresent()) return false;

        ItemStack itemStack = itemStackOptional.get();
        itemStack.setAmount(itemStack.getAmount() - Settings.costAmount);
        String capitalizedMaterial = Settings.costMaterial.name().substring(0, 1) + Settings.costMaterial.name().substring(1).toLowerCase().replace("_", " ");

        if (Settings.costAmount > 0)
            player.sendMessage(Language.getKey("chargeItem").replaceAll("%item%", capitalizedMaterial).replaceAll("%costItem%", String.valueOf(Settings.costAmount)));

        return true;
    }
}