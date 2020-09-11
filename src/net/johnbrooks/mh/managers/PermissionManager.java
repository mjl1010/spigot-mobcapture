package net.johnbrooks.mh.managers;

import org.bukkit.entity.*;

public class PermissionManager {
    public final String NoCost = "MobCapture.NoCost";
    public final String CatchPrefix = "MobCapture.Catch.";
    public final String CatchPeaceful = "MobCapture.Catch.Peaceful";
    public final String CatchHostile = "MobCapture.Catch.Hostile";

    public boolean hasPermissionToCapture(Player player, LivingEntity livingEntity) {
        if (livingEntity instanceof Monster && player.hasPermission(CatchHostile)) {
            return true;
        } else if (livingEntity instanceof Mob && player.hasPermission(CatchPeaceful)) {
            return true;
        } else {
            return player.hasPermission(CatchPrefix + livingEntity.getType().name());
        }
    }
}