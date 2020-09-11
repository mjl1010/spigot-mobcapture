package net.johnbrooks.mh.items;

import net.johnbrooks.mh.NBTManager;
import net.johnbrooks.mh.Settings;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

public class CaptureEgg {
    public static void captureLivingEntity(LivingEntity livingEntity) {
        if (Settings.catchEffect) {
            livingEntity.getLocation().getWorld().playSound(livingEntity.getLocation(), Sound.ENTITY_GHAST_SHOOT, SoundCategory.MASTER, 1f, 1f);
            livingEntity.getLocation().getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, livingEntity.getLocation(), 4);
        }
        ItemStack eggItemStack = CaptureEgg.get(livingEntity);
        Item drop = livingEntity.getLocation().getWorld().dropItem(livingEntity.getLocation(), eggItemStack);
        drop.setItemStack(eggItemStack);
        drop.setVelocity(new Vector(0, 0.3f, 0));
    }

    public static LivingEntity useSpawnItem(ItemStack spawnItem, Location target) {
        if (target == null) return null;
        if (Settings.catchEffect) {
            target.getWorld().playSound(target, Sound.ENTITY_GHAST_SHOOT, SoundCategory.MASTER, 1f, 1f);
            target.getWorld().spawnParticle(Particle.EXPLOSION_NORMAL, target, 4);
        }
        return NBTManager.spawnEntityFromNBTData(spawnItem, target);
    }

    private static ItemStack get(LivingEntity livingEntity) {
        return convertEntityToEntitySpawnEgg(livingEntity);
    }

    private static ItemStack convertEntityToEntitySpawnEgg(LivingEntity livingEntity) {
        Material material = Material.getMaterial(livingEntity.getType().name().toUpperCase() + "_SPAWN_EGG");
        if (material == null) {
            switch (livingEntity.getType()) {
                case MUSHROOM_COW:
                    material = Material.MOOSHROOM_SPAWN_EGG;
                    break;
                default:
                    material = Material.GHAST_SPAWN_EGG;
                    break;
            }
        }
        ItemStack spawnEgg = new ItemStack(material, 1);
        return NBTManager.castEntityDataToItemStackNBT(spawnEgg, livingEntity);
    }
}