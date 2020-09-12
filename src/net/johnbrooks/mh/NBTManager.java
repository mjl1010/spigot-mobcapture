package net.johnbrooks.mh;

import net.minecraft.server.v1_16_R2.*;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_16_R2.inventory.CraftItemStack;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentWrapper;
import org.bukkit.entity.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.Lootable;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class NBTManager {
    public enum ListType {
        END, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, BYTE_ARRAY, STRING, LIST, COMPOUND, INT_ARRAY;
    }

    public static boolean isSpawnEgg(ItemStack itemStack) {
        if (itemStack.getType().name().contains("SPAWN_EGG")) {
            net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
            if (nmsStack.hasTag()) {
                NBTTagCompound compound = nmsStack.getTag();
                NBTTagCompound entityDetails = compound.getCompound("tag");
                if (entityDetails != null) {
                    String entityType = entityDetails.getString("entity type");
                    return entityType != null;
                }
            }
        }

        return false;
    }

    public static LivingEntity spawnEntityFromNBTData(ItemStack spawnItem, Location target) {

        if (spawnItem != null) {
            net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(spawnItem);
            if (nmsStack.hasTag()) {
                NBTTagCompound compound = nmsStack.getTag();
                NBTTagCompound entityDetails = compound.getCompound("tag");

                String entityType = entityDetails.getString("entity type");

                try {
                    LivingEntity livingEntity = (LivingEntity) target.getWorld().spawnEntity(target.clone().add(0, 0.5f, 0), EntityType.valueOf(entityType));
                    applyNBTDataToEntity(livingEntity, entityDetails);

                    return livingEntity;
                } catch (Exception ex) {
                    Main.logger.warning("Spawn entity type not found.");
                    String nombre=null;
                    if (spawnItem.getType() == Material.MOOSHROOM_SPAWN_EGG) {
                        nombre = "MUSHROOM_COW";
                    } else {
                        String[] S = spawnItem.getType().name().split("_");
                        nombre = S[0];
                        for (int x = 1; x < S.length - 2; x++) nombre += "_" + S[x];
                    }
                    return (LivingEntity) target.getWorld().spawnEntity(target.clone().add(0, 0.5f, 0), EntityType.valueOf(nombre));
                }
            } else {
                Main.logger.warning("Spawn Item does not have any NBT Tags.");
                String nombre = null;
                if (spawnItem.getType() == Material.MOOSHROOM_SPAWN_EGG) {
                    nombre = "MUSHROOM_COW";
                } else {
                    String[] S = spawnItem.getType().name().split("_");
                    nombre = S[0];
                    for (int x = 1; x < S.length - 2; x++) nombre += "_" + S[x];
                }
                return (LivingEntity) target.getWorld().spawnEntity(target.clone().add(0, 0.5f, 0), EntityType.valueOf(nombre));
            }
        } else
            Main.logger.warning("NULL spawn item passed to #spawnEntityFromNBTData().");

        return null;
    }

    private static void applyNBTDataToEntity(LivingEntity livingEntity, NBTTagCompound entityDetails) {
        if (entityDetails.hasKey("custom name"))
            livingEntity.setCustomName(entityDetails.getString("custom name"));
        livingEntity.setAI(entityDetails.getBoolean("ai"));
        livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(entityDetails.getDouble("max health"));
        livingEntity.setHealth(entityDetails.getDouble("health"));
        livingEntity.setGlowing(entityDetails.getBoolean("glowing"));

        NBTTagList potionEffectList = entityDetails.getList("potion effects", ListType.COMPOUND.ordinal());
        for (int i = 0; i < potionEffectList.size(); i++) {
            NBTTagCompound potionEffectCompound = potionEffectList.getCompound(i);

            int duration = potionEffectCompound.getInt("duration");
            int amplifier = potionEffectCompound.getInt("amplifier");
//            int colorRed = -1;
//            int colorGreen = -1;
//            int colorBlue = -1;
//            if (potionEffectCompound.hasKey("color red")) {
//                colorRed = potionEffectCompound.getInt("color red");
//                colorGreen = potionEffectCompound.getInt("color green");
//                colorBlue = potionEffectCompound.getInt("color blue");
//            }
            String type = potionEffectCompound.getString("type");
            boolean hasParticles = potionEffectCompound.getBoolean("particles");
            boolean isAmbient = potionEffectCompound.getBoolean("ambient");

            PotionEffectType potionEffectType = PotionEffectType.getByName(type);
//            Color color = null;
//            if (colorRed != -1 && colorBlue != -1 && colorGreen != -1)
//                color = Color.fromRGB(colorRed, colorGreen, colorBlue);

            PotionEffect potionEffect;
//            if (color != null)
//                potionEffect = new PotionEffect(potionEffectType, duration, amplifier, isAmbient, hasParticles, true);
//            else
            potionEffect = new PotionEffect(potionEffectType, duration, amplifier, isAmbient, hasParticles);
            livingEntity.addPotionEffect(potionEffect);
        }

        // EntityEquipment
        ItemStack itemArmor = null;
        NBTTagCompound tagArmor = null;

        // Equipment boots
        if (entityDetails.hasKey("equipment boots")) {
            itemArmor = new ItemStack(Material.valueOf(entityDetails.getString("equipment boots material")));
            tagArmor = entityDetails.getCompound("equipment boots");
            if (tagArmor.equals(new NBTTagCompound())) {
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemArmor);
                nmsStack.setTag(tagArmor);
                itemArmor = CraftItemStack.asBukkitCopy(nmsStack);
            }
            livingEntity.getEquipment().setBoots(itemArmor);
            livingEntity.getEquipment().setBootsDropChance(entityDetails.getFloat(("equipment boots dropchance")));
        }

        // Equipment chestplate
        if (entityDetails.hasKey("equipment chestplate")) {
            itemArmor = new ItemStack(Material.valueOf(entityDetails.getString("equipment chestplate material")));
            tagArmor = entityDetails.getCompound("equipment chestplate");
            if (tagArmor.equals(new NBTTagCompound())) {
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemArmor);
                nmsStack.setTag(tagArmor);
                itemArmor = CraftItemStack.asBukkitCopy(nmsStack);
            }
            livingEntity.getEquipment().setChestplate(itemArmor);
            livingEntity.getEquipment().setChestplateDropChance(entityDetails.getFloat(("equipment chestplate dropchance")));
        }

        // Equipment helmet
        if (entityDetails.hasKey("equipment helmet")) {
            itemArmor = new ItemStack(Material.valueOf(entityDetails.getString("equipment helmet material")));
            tagArmor = entityDetails.getCompound("equipment helmet");
            if (tagArmor.equals(new NBTTagCompound())) {
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemArmor);
                nmsStack.setTag(tagArmor);
                itemArmor = CraftItemStack.asBukkitCopy(nmsStack);
            }
            livingEntity.getEquipment().setHelmet(itemArmor);
            livingEntity.getEquipment().setHelmetDropChance(entityDetails.getFloat(("equipment helmet dropchance")));
        }

        // Equipment leggings
        if (entityDetails.hasKey("equipment leggings")) {
            itemArmor = new ItemStack(Material.valueOf(entityDetails.getString("equipment leggings material")));
            tagArmor = entityDetails.getCompound("equipment leggings");
            if (tagArmor.equals(new NBTTagCompound())) {
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemArmor);
                nmsStack.setTag(tagArmor);
                itemArmor = CraftItemStack.asBukkitCopy(nmsStack);
            }
            livingEntity.getEquipment().setLeggings(itemArmor);
            livingEntity.getEquipment().setLeggingsDropChance(entityDetails.getFloat(("equipment leggings dropchance")));
        }

        // Equipment mainHand
        if (entityDetails.hasKey("equipment mainhand")) {
            itemArmor = new ItemStack(Material.valueOf(entityDetails.getString("equipment mainhand material")));
            tagArmor = entityDetails.getCompound("equipment mainhand");
            if (tagArmor.equals(new NBTTagCompound())) {
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemArmor);
                nmsStack.setTag(tagArmor);
                itemArmor = CraftItemStack.asBukkitCopy(nmsStack);
            }
            livingEntity.getEquipment().setItemInMainHand(itemArmor);
            livingEntity.getEquipment().setItemInMainHandDropChance(entityDetails.getFloat(("equipment mainhand dropchance")));
        }

        // Equipment offHand
        if (entityDetails.hasKey("equipment offhand")) {
            itemArmor = new ItemStack(Material.valueOf(entityDetails.getString("equipment offhand material")));
            tagArmor = entityDetails.getCompound("equipment offhand");
            if (tagArmor.equals(new NBTTagCompound())) {
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemArmor);
                nmsStack.setTag(tagArmor);
                itemArmor = CraftItemStack.asBukkitCopy(nmsStack);
            }
            livingEntity.getEquipment().setItemInOffHand(itemArmor);
            livingEntity.getEquipment().setItemInOffHandDropChance(entityDetails.getFloat(("equipment offhand dropchance")));
        }

        // General entity data
        if (livingEntity instanceof Ageable) {
            Ageable ageable = (Ageable) livingEntity;
            ageable.setAge(entityDetails.getInt("age"));
        }

        if (livingEntity instanceof Tameable) {
            Tameable tameable = (Tameable) livingEntity;
            tameable.setTamed(entityDetails.getBoolean("tamed"));
            if (tameable.isTamed()) {
                try {
                    UUID ownerUUID = UUID.fromString(entityDetails.getString("owner"));
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(ownerUUID);
                    tameable.setOwner(offlinePlayer);
                } catch (IllegalArgumentException ignored) {

                }
            }
        }

        if (livingEntity instanceof Raider) {
            Raider raider = (Raider) livingEntity;
            raider.setPatrolLeader(entityDetails.getBoolean("patrol"));
        }

        if (livingEntity instanceof Sittable) {
            Sittable sittable = (Sittable) livingEntity;
            sittable.setSitting(entityDetails.getBoolean("sitting"));
        }

        if (livingEntity instanceof Steerable) {
            Steerable abstractEntity = (Steerable) livingEntity;
            abstractEntity.setBoostTicks(entityDetails.getInt("boost"));
            abstractEntity.setCurrentBoostTicks(entityDetails.getInt("currentBoost"));
            abstractEntity.setSaddle(entityDetails.getBoolean("saddle"));
        }

        if (livingEntity instanceof Lootable) {
            Lootable lootable = (Lootable) livingEntity;
            lootable.setSeed(entityDetails.getLong("seed"));
        }

        if (livingEntity instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) livingEntity;

            //1) Declare storage for item stack material, slot, and NBT tags
            NBTTagList tagList = entityDetails.getList("inventory nbt tags", ListType.COMPOUND.ordinal());
            NBTTagList materialList = entityDetails.getList("inventory materials", ListType.STRING.ordinal());
            NBTTagList enchList = entityDetails.getList("inventory ench", ListType.STRING.ordinal());

            for (int i = 0; i < materialList.size(); i++) {
                //2) Load NBT tag data and material type
                String[] materialElements = materialList.getString(i).split("\\.");
                String materialName = materialElements[0];
                int slot = Integer.parseInt(materialElements[1]);
                int ammount = materialElements.length >= 3 ? Integer.parseInt(materialElements[2]) : 1;
                NBTTagCompound tag = tagList.getCompound(i);

                //3) Create item stack and attach NBT tag to it.
                ItemStack itemStack = new ItemStack(Material.valueOf(materialName), ammount);
                if (tag.equals(new NBTTagCompound())) {
                    net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
                    nmsStack.setTag(tag);
                    itemStack = CraftItemStack.asBukkitCopy(nmsStack);
                }

                // 3.1) Item enchants
                for (int e = 0; e < enchList.size(); e++) {
                    String[] enchant = enchList.getString(e).split("\\.");
                    if (Integer.parseInt(enchant[0]) == i) {
                        System.out.println(enchant[2]);
                        itemStack.addUnsafeEnchantment(new EnchantmentWrapper(enchant[2]), Integer.parseInt(enchant[1]));
                    }
                }

                //4) Place in inventory at correct slot number.
                inventoryHolder.getInventory().setItem(slot, itemStack);
            }
        }

        // Abstract entities data
        if (livingEntity instanceof AbstractHorse) {
            AbstractHorse abstractHorse = (AbstractHorse) livingEntity;
            abstractHorse.setJumpStrength(entityDetails.getDouble("jump strength"));
            abstractHorse.setTamed(entityDetails.getBoolean("tamed"));
            abstractHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(entityDetails.getDouble("speed"));
            abstractHorse.setDomestication(entityDetails.getInt("domestication"));
            abstractHorse.setMaxDomestication(entityDetails.getInt("max domestication"));

            if (livingEntity instanceof ChestedHorse) ((ChestedHorse) livingEntity).setCarryingChest(entityDetails.getBoolean("chest equipped"));

            //TODO: INVENTORY CONTENTS

//            NBTTagList saddle = entityDetails.getList("saddle", ListType.COMPOUND.ordinal());
//            NBTTagCompound recipe = saddle.getCompound(0);
//
//            String[] ingredient = recipe.getString(0).split("\\.");
//            NBTTagCompound tags = tagList.getCompound(0);
//            ItemStack itemStack = new ItemStack(Material.valueOf(ingredient[0]), Integer.parseInt(ingredient[1]));
//            net.minecraft.server.v1_16_R2.ItemStack nmsIngredientStack = CraftItemStack.asNMSCopy(itemStack);
//            nmsIngredientStack.setTag(tags);
//            itemStack = CraftItemStack.asBukkitCopy(nmsIngredientStack);
//
//            entityDetails.set("saddle", itemStackTags2);
//
//            if (livingEntity instanceof HorseInventory) {
//                NBTTagList itemStackTags3 = new NBTTagList(); // Store the tags
//                net.minecraft.server.v1_16_R2.ItemStack nmsStack3 = CraftItemStack.asNMSCopy(((HorseInventory)livingEntity).getArmor());
//                NBTTagCompound itemStackCompound3 = (nmsStack3.hasTag()) ? nmsStack3.getTag() : new NBTTagCompound();
//                itemStackTags3.add(itemStackCompound3);
//                entityDetails.set("armor", itemStackTags3);
//            } else if (livingEntity instanceof LlamaInventory) {
//                NBTTagList itemStackTags3 = new NBTTagList(); // Store the tags
//                net.minecraft.server.v1_16_R2.ItemStack nmsStack3 = CraftItemStack.asNMSCopy(((LlamaInventory)livingEntity).getDecor());
//                NBTTagCompound itemStackCompound3 = (nmsStack3.hasTag()) ? nmsStack3.getTag() : new NBTTagCompound();
//                itemStackTags3.add(itemStackCompound3);
//                entityDetails.set("decor", itemStackTags3);
//            }

        } else if (livingEntity instanceof AbstractVillager) {
            //1) Get basic villager data.
            AbstractVillager villager = (AbstractVillager) livingEntity;

            if (livingEntity instanceof Villager) {
                Villager villager2 = (Villager) livingEntity;
                villager2.setProfession(Villager.Profession.valueOf(entityDetails.getString("profession")));
                villager2.setVillagerType(Villager.Type.valueOf(entityDetails.getString("type")));
                villager2.setVillagerExperience(entityDetails.getInt("exp"));
                villager2.setVillagerLevel(entityDetails.getInt("level"));
            }

            //2) Grab the recipe list.
            NBTTagList recipeList = entityDetails.getList("recipes", ListType.COMPOUND.ordinal());

            //3) Prepare an ArrayList for recipe list that will be stored in Villager.
            List<org.bukkit.inventory.MerchantRecipe> merchantRecipeList = new ArrayList<>();
            for (int i = 0; i < recipeList.size(); i++) {
                //4) Parse the recipe list.
                NBTTagCompound recipeCompound = recipeList.getCompound(i);
                int uses = recipeCompound.getInt("uses");
                int maxUses = recipeCompound.getInt("max uses");
                boolean experienceReward = recipeCompound.getBoolean("experience reward");
                String[] resultString = recipeCompound.getString("result").split("\\.");
                NBTTagCompound resultTags = recipeCompound.getCompound("result tags");
                NBTTagList materialsAndAmount = recipeCompound.getList("materials", ListType.STRING.ordinal());
                NBTTagList tagList = recipeCompound.getList("tags", ListType.COMPOUND.ordinal());

                //5) Set the resulted item stack to its proper NBT tags.
                ItemStack resultItemStack = new ItemStack(Material.valueOf(resultString[0]), Integer.parseInt(resultString[1]));
                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(resultItemStack);
                nmsStack.setTag(resultTags);
                resultItemStack = CraftItemStack.asBukkitCopy(nmsStack);

                //6) Register the recipes.
                org.bukkit.inventory.MerchantRecipe merchantRecipe = new org.bukkit.inventory.MerchantRecipe(resultItemStack, maxUses);
                merchantRecipe.setUses(uses);
                merchantRecipe.setExperienceReward(experienceReward);
                List<ItemStack> ingredients = new ArrayList<>();
                for (int j = 0; j < materialsAndAmount.size(); j++) {
                    String[] ingredient = materialsAndAmount.getString(j).split("\\.");
                    NBTTagCompound tags = tagList.getCompound(j);
                    ItemStack itemStack = new ItemStack(Material.valueOf(ingredient[0]), Integer.parseInt(ingredient[1]));
                    net.minecraft.server.v1_16_R2.ItemStack nmsIngredientStack = CraftItemStack.asNMSCopy(itemStack);
                    nmsIngredientStack.setTag(tags);
                    itemStack = CraftItemStack.asBukkitCopy(nmsIngredientStack);
                    ingredients.add(itemStack);
                }
                merchantRecipe.setIngredients(ingredients);
                merchantRecipeList.add(merchantRecipe);
            }

            //4) Input the recipe list into the villager
            villager.setRecipes(merchantRecipeList);
        } else if (livingEntity instanceof PiglinAbstract) {
            PiglinAbstract piglin = (PiglinAbstract) livingEntity;
            if (entityDetails.getBoolean("convertion")) piglin.setConversionTime(entityDetails.getInt("convertionTime"));
            piglin.setImmuneToZombification(entityDetails.getBoolean("immune"));
        }


        // Individual entity data
        if (livingEntity instanceof Wolf) {
            Wolf wolf = (Wolf) livingEntity;
            wolf.setAngry(entityDetails.getBoolean("angry"));
            if (entityDetails.getBoolean("tamed"))
                wolf.setCollarColor(DyeColor.valueOf(entityDetails.getString("color")));
        } else if (livingEntity instanceof Sheep) {
            Sheep sheep = (Sheep) livingEntity;
            sheep.setSheared(entityDetails.getBoolean("sheared"));
            sheep.setColor(DyeColor.valueOf(entityDetails.getString("color")));
        } else if (livingEntity instanceof Cat) {
            Cat cat = (Cat) livingEntity;
            cat.setCatType(Cat.Type.valueOf(entityDetails.getString("cat type")));
        } else if (livingEntity instanceof Rabbit) {
            Rabbit rabbit = (Rabbit) livingEntity;
            rabbit.setRabbitType(Rabbit.Type.valueOf(entityDetails.getString("rabbit type")));
        } else if (livingEntity instanceof Fox) {
            Fox fox = (Fox) livingEntity;
            fox.setFoxType(Fox.Type.valueOf(entityDetails.getString("fox type")));
        } else if (livingEntity instanceof Creeper) {
            ((Creeper) livingEntity).setPowered(entityDetails.getBoolean("charged"));
        } else if (livingEntity instanceof Slime) {
            ((Slime) livingEntity).setSize(entityDetails.getInt("size"));
        } else if (livingEntity instanceof ZombieVillager) {
            ZombieVillager zombieVillager = (ZombieVillager) livingEntity;
            zombieVillager.setVillagerProfession(Villager.Profession.valueOf(entityDetails.getString("profession")));
        } else if (livingEntity instanceof Parrot) {
            ((Parrot) livingEntity).setVariant(Parrot.Variant.valueOf(entityDetails.getString("variant")));
        } else if (livingEntity instanceof Panda) {
            Panda panda = (Panda) livingEntity;
            panda.setHiddenGene(Panda.Gene.valueOf(entityDetails.getString("hidden gene")));
            panda.setMainGene(Panda.Gene.valueOf(entityDetails.getString("main gene")));
        } else if (livingEntity instanceof Bee) {
            Bee bee = (Bee) livingEntity;
            bee.setHasNectar(entityDetails.getBoolean("nectar"));
            bee.setHasStung(entityDetails.getBoolean("stung"));
            bee.setAnger(entityDetails.getInt("anger"));
        } else if (livingEntity instanceof Piglin) {
            Piglin piglin = (Piglin) livingEntity;
            piglin.setIsAbleToHunt(entityDetails.getBoolean("hunt"));
        } else if (livingEntity instanceof Strider) {
            Strider strider = (Strider) livingEntity;
            strider.setShivering(entityDetails.getBoolean("shivering"));
        }

        // horse types
        if (livingEntity instanceof Llama) {
            Llama abstractHorse = (Llama) livingEntity;
            Llama.Color color = Llama.Color.valueOf(entityDetails.getString("color"));
            int strength = entityDetails.getInt("strength");
            abstractHorse.setColor(color);
            abstractHorse.setStrength(strength);
        } else if (livingEntity instanceof Horse) {
            Horse horse = (Horse) livingEntity;
            Horse.Color color = Horse.Color.valueOf(entityDetails.getString("color"));
            Horse.Style style = Horse.Style.valueOf(entityDetails.getString("style"));
            horse.setColor(color);
            horse.setStyle(style);
        }
    }

    public static ItemStack castEntityDataToItemStackNBT(ItemStack itemStack, LivingEntity livingEntity) {
        //2) Figure entity name
        String entityName;
        if (livingEntity.getCustomName() == null) {
            entityName = livingEntity.getType().name().replace("_", " ").toLowerCase();
            entityName = entityName.substring(0, 1).toUpperCase() + entityName.substring(1);
        } else
            entityName = livingEntity.getCustomName();

        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(Language.getKeyWithoutPrefix("capturedMob").replaceAll("%entity%", entityName));
        itemMeta.setLore(createItemLore(livingEntity));
        itemStack.setItemMeta(itemMeta);

        final net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
        final NBTTagCompound tagCompound = (nmsStack.hasTag() && nmsStack.getTag() != null) ? nmsStack.getTag() : new NBTTagCompound();

        //2) Gather capture data

        final NBTTagCompound entityDetails = generateNBTTagCompound(tagCompound, livingEntity);
        tagCompound.set("tag", entityDetails);

        //final NBTTagCompound display = tagCompound.hasKey("display") ? tagCompound.getCompound("display") : new NBTTagCompound();

        //TODO: Re-enable this when NBT Tags are finished in Spigot. SPIGOT BUG
        //display.set("Name", NBTTagString.a(CaptureEgg.TITLE_PREFIX + entityName));

        //display.set("Lore", createNMSItemLore(livingEntity));
        //tagCompound.set("display", display);

        //) Package and convert
        nmsStack.setTag(tagCompound);
        itemStack = CraftItemStack.asBukkitCopy(nmsStack);

        return itemStack;
    }

    private static List<String> createItemLore(final LivingEntity livingEntity) {
        final List<String> loreList = new ArrayList<>(2);
        loreList.add(Language.getKeyWithoutPrefix("mobType") + livingEntity.getType().name());
        loreList.add(Language.getKeyWithoutPrefix("mobHealth") + getLoreHealth(livingEntity));

        // Entity Age
        if (livingEntity instanceof Ageable) {
            final Ageable ageable = (Ageable) livingEntity;
            if (!ageable.isAdult())
                loreList.add(Language.getKeyWithoutPrefix("mobAge") + Language.getKeyWithoutPrefix("ageBaby"));
        }

        // Entity Tamed
        if (livingEntity instanceof Tameable) {
            final Tameable tameable = (Tameable) livingEntity;
            loreList.add(Language.getKeyWithoutPrefix("mobTamed") + (tameable.isTamed() ? Language.getKeyWithoutPrefix("affirmative") : Language.getKeyWithoutPrefix("negative")));
        }

        // Parrot Color
        if (livingEntity instanceof Parrot) {
            final Parrot parrot = (Parrot) livingEntity;
            loreList.add(Language.getKeyWithoutPrefix("mobColor") + parrot.getVariant().name());
        }

        // Active potion effects
        livingEntity.getActivePotionEffects().forEach(potionEffect -> {
            final int duration = potionEffect.getDuration();
            final String typeName = potionEffect.getType().getName().replace("_", " ").toLowerCase();
            loreList.add(ChatColor.DARK_PURPLE + typeName.substring(0, 1).toUpperCase() + typeName.substring(1) +
                    " for " + (duration / 20) + " seconds");
        });

        return loreList;
    }

    private static NBTTagList createNMSItemLore(final LivingEntity livingEntity) {
        final NBTTagList loreList = new NBTTagList();

        // Entity Type
        loreList.add(NBTTagString.a(Language.getKeyWithoutPrefix("mobType") + livingEntity.getType().name()));

        // Entity Health
        loreList.add(NBTTagString.a(Language.getKeyWithoutPrefix("mobHealth") + getLoreHealth(livingEntity)));

        // Entity Age
        if (livingEntity instanceof Ageable) {
            final Ageable ageable = (Ageable) livingEntity;
            if (!ageable.isAdult())
                loreList.add(NBTTagString.a(Language.getKeyWithoutPrefix("mobAge") + Language.getKeyWithoutPrefix("ageBaby")));
        }

        // Entity Tamed
        if (livingEntity instanceof Tameable) {
            final Tameable tameable = (Tameable) livingEntity;
            loreList.add(NBTTagString.a(Language.getKeyWithoutPrefix("mobTamed") + (tameable.isTamed() ? Language.getKeyWithoutPrefix("affirmative") : Language.getKeyWithoutPrefix("negative"))));
        }

        // Parrot Color
        if (livingEntity instanceof Parrot) {
            final Parrot parrot = (Parrot) livingEntity;
            loreList.add(NBTTagString.a(Language.getKeyWithoutPrefix("mobColor") + parrot.getVariant().name()));
        }

        // Active potion effects
        livingEntity.getActivePotionEffects().forEach(potionEffect -> {
            final int duration = potionEffect.getDuration();
            final String typeName = potionEffect.getType().getName().replace("_", " ").toLowerCase();
            loreList.add(NBTTagString.a(
                    ChatColor.DARK_PURPLE + typeName.substring(0, 1).toUpperCase() + typeName.substring(1) +
                            " for " + (duration / 20) + " seconds"));
        });

        return loreList;
    }

    private static String getLoreHealth(final LivingEntity livingEntity) {
        String healthData;
        if (round(livingEntity.getHealth(), 1) == (int) livingEntity.getHealth()) {
            healthData = String.valueOf((int) livingEntity.getHealth());
        }
        else {
            healthData = String.valueOf(round(livingEntity.getHealth(), 1));
        }
        String maxHealthData;
        if (round(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue(), 1) == (int) livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()) {
            maxHealthData = String.valueOf((int) livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        }
        else {
            maxHealthData = String.valueOf(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue());
        }
        return healthData + "/" + maxHealthData;
    }

    private static NBTTagCompound generateNBTTagCompound(NBTTagCompound compound, LivingEntity livingEntity) {
        NBTTagCompound entityDetails = compound.getCompound("tag");
        // General entity data
        if (livingEntity.getCustomName() != null)
            entityDetails.set("custom name", NBTTagString.a(livingEntity.getCustomName()));
        entityDetails.set("entity type", NBTTagString.a(livingEntity.getType().name()));
        entityDetails.setBoolean("ai", livingEntity.hasAI());
        entityDetails.set("health", NBTTagDouble.a(livingEntity.getHealth()));
        entityDetails.set("max health", NBTTagDouble.a(livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue()));
        entityDetails.setBoolean("glowing", livingEntity.isGlowing());

        NBTTagList potionEffectList = new NBTTagList();
        for (PotionEffect potionEffect : livingEntity.getActivePotionEffects()) {
            NBTTagCompound potionEffectCompound = new NBTTagCompound();
            potionEffectCompound.setInt("duration", potionEffect.getDuration());
            potionEffectCompound.setInt("amplifier", potionEffect.getAmplifier());
            potionEffect.getType().getColor();
            potionEffectCompound.setInt("color red", potionEffect.getType().getColor().getRed());
            potionEffectCompound.setInt("color green", potionEffect.getType().getColor().getGreen());
            potionEffectCompound.setInt("color blue", potionEffect.getType().getColor().getBlue());
            potionEffectCompound.setString("type", potionEffect.getType().getName());
            potionEffectCompound.setBoolean("ambient", potionEffect.isAmbient());
            potionEffectCompound.setBoolean("particles", potionEffect.hasParticles());

            potionEffectList.add(potionEffectCompound);
        }
        entityDetails.set("potion effects", potionEffectList);

        // Equipment boots
        net.minecraft.server.v1_16_R2.ItemStack nmsBoots = CraftItemStack.asNMSCopy(livingEntity.getEquipment().getBoots());
        NBTTagCompound bootsCompound = (nmsBoots.hasTag()) ? nmsBoots.getTag() : new NBTTagCompound();
        entityDetails.set("equipment boots", bootsCompound);
        entityDetails.setString("equipment boots material", livingEntity.getEquipment().getBoots().getType().name());
        entityDetails.setFloat("equipment boots dropchance", livingEntity.getEquipment().getBootsDropChance());

        // Equipment chestplate
        net.minecraft.server.v1_16_R2.ItemStack nmsChestplate = CraftItemStack.asNMSCopy(livingEntity.getEquipment().getChestplate());
        NBTTagCompound chestplateCompound = (nmsChestplate.hasTag()) ? nmsChestplate.getTag() : new NBTTagCompound();
        entityDetails.set("equipment chestplate", chestplateCompound);
        entityDetails.setString("equipment chestplate material", livingEntity.getEquipment().getChestplate().getType().name());
        entityDetails.setFloat("equipment chestplate dropchance", livingEntity.getEquipment().getChestplateDropChance());

        // Equipment helmet
        net.minecraft.server.v1_16_R2.ItemStack nmshelmet = CraftItemStack.asNMSCopy(livingEntity.getEquipment().getHelmet());
        NBTTagCompound helmetCompound = (nmshelmet.hasTag()) ? nmshelmet.getTag() : new NBTTagCompound();
        entityDetails.set("equipment helmet", helmetCompound);
        entityDetails.setString("equipment helmet material", livingEntity.getEquipment().getHelmet().getType().name());
        entityDetails.setFloat("equipment helmet dropchance", livingEntity.getEquipment().getHelmetDropChance());

        // Equipment leggings
        net.minecraft.server.v1_16_R2.ItemStack nmsleggings = CraftItemStack.asNMSCopy(livingEntity.getEquipment().getLeggings());
        NBTTagCompound leggingsCompound = (nmsleggings.hasTag()) ? nmsleggings.getTag() : new NBTTagCompound();
        entityDetails.set("equipment leggings", leggingsCompound);
        entityDetails.setString("equipment leggings material", livingEntity.getEquipment().getLeggings().getType().name());
        entityDetails.setFloat("equipment leggings dropchance", livingEntity.getEquipment().getLeggingsDropChance());

        // Equipment mainHand
        net.minecraft.server.v1_16_R2.ItemStack nmsmainHand = CraftItemStack.asNMSCopy(livingEntity.getEquipment().getItemInMainHand());
        NBTTagCompound mainHandCompound = (nmsmainHand.hasTag()) ? nmsmainHand.getTag() : new NBTTagCompound();
        entityDetails.set("equipment mainhand", mainHandCompound);
        entityDetails.setString("equipment mainhand material", livingEntity.getEquipment().getItemInMainHand().getType().name());
        entityDetails.setFloat("equipment mainhand dropchance", livingEntity.getEquipment().getItemInMainHandDropChance());

        // Equipment offHand
        net.minecraft.server.v1_16_R2.ItemStack nmsoffHand = CraftItemStack.asNMSCopy(livingEntity.getEquipment().getItemInOffHand());
        NBTTagCompound offHandCompound = (nmsoffHand.hasTag()) ? nmsoffHand.getTag() : new NBTTagCompound();
        entityDetails.set("equipment offhand", offHandCompound);
        entityDetails.setString("equipment offhand material", livingEntity.getEquipment().getItemInOffHand().getType().name());
        entityDetails.setFloat("equipment offhand dropchance", livingEntity.getEquipment().getItemInOffHandDropChance());

        // General extended entities
        if (livingEntity instanceof Ageable) {
            Ageable ageable = (Ageable) livingEntity;
            entityDetails.setInt("age", ageable.getAge());
        }

        if (livingEntity instanceof InventoryHolder) {
            InventoryHolder inventoryHolder = (InventoryHolder) livingEntity;

            NBTTagList tagList = new NBTTagList();
            NBTTagList materialList = new NBTTagList();
            NBTTagList enchList = new NBTTagList();

            for (int i = 0; i < inventoryHolder.getInventory().getContents().length; i++) {
                ItemStack itemStack = inventoryHolder.getInventory().getContents()[i];
                if (itemStack != null) {
                    net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
                    materialList.add(NBTTagString.a(itemStack.getType().name() + "." + i + itemStack.getAmount()));
                    NBTTagCompound itemStackCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
                    tagList.add(itemStackCompound);

                    for (Map.Entry<Enchantment, Integer> e : itemStack.getEnchantments().entrySet()) {
                        enchList.add(NBTTagString.a(i + "." + e.getValue() + "." + e.getKey().getKey().getKey()));
                    }
                }
            }

            entityDetails.set("inventory materials", materialList);
            entityDetails.set("inventory nbt tags", tagList);
            entityDetails.set("inventory ench", enchList);
        }

        if (livingEntity instanceof Lootable) {
            Lootable lootTable = (Lootable) livingEntity;
            entityDetails.setLong("seed", lootTable.getSeed());
        }

        if (livingEntity instanceof Raider) {
            Raider raider = (Raider) livingEntity;
            entityDetails.setBoolean("patrol", raider.isPatrolLeader());
        }

        if (livingEntity instanceof Sittable) {
            Sittable sittable = (Sittable) livingEntity;
            entityDetails.setBoolean("sitting", sittable.isSitting());
        }

        if (livingEntity instanceof Tameable) {
            Tameable tameable = (Tameable) livingEntity;
            entityDetails.setBoolean("tamed", tameable.isTamed());
            if (tameable.isTamed() && tameable.getOwner() != null) {
                String ownerUUID = tameable.getOwner().getUniqueId().toString();
                entityDetails.setString("owner", ownerUUID);
            }
        }

        if (livingEntity instanceof Steerable) {
            Steerable abstractEntity = (Steerable) livingEntity;
            entityDetails.setInt("boost", abstractEntity.getBoostTicks());
            entityDetails.setInt("currentBoost", abstractEntity.getCurrentBoostTicks());
            entityDetails.setBoolean("saddle", abstractEntity.hasSaddle());
        }

        // Abstract entities
        if (livingEntity instanceof AbstractHorse) {
            if (livingEntity instanceof ChestedHorse) entityDetails.setBoolean("chest equipped", ((ChestedHorse) livingEntity).isCarryingChest());

            AbstractHorse abstractHorse = (AbstractHorse) livingEntity;
            double jumpStrength = abstractHorse.getJumpStrength();
            double speed = livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();

            entityDetails.setDouble("jump strength", jumpStrength);
            entityDetails.setDouble("speed", speed);
            entityDetails.setInt("domestication", abstractHorse.getDomestication());
            entityDetails.setInt("max domestication", abstractHorse.getMaxDomestication());

//            NBTTagList recipeList = new NBTTagList();
//            NBTTagList materialsAndAmount = new NBTTagList(); // Holds materials and amount separated by "."
//            NBTTagList itemStackTags = new NBTTagList(); // Store the tags
//            NBTTagList ench = new NBTTagList();
//
//            for (ItemStack itemStack : abstractHorse.getInventory()) {
//                // Store the materials and amounts
//                materialsAndAmount.add(NBTTagString.a(itemStack.getType().name() + "." + itemStack.getAmount()));
//
//                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
//                NBTTagCompound itemStackCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
//                itemStackTags.add(itemStackCompound);
//
//                for (Map.Entry<Enchantment, Integer> e : itemStack.getEnchantments().entrySet()) {
//                    NBTTagCompound enchant = new NBTTagCompound();
//                    enchant.setString("id", e.getKey().getKey().getKey());
//                    enchant.setInt("lvl", e.getValue());
//                    ench.add(enchant);
//                }
//            }
//
//            NBTTagCompound recipeCompound = new NBTTagCompound();
//            recipeCompound.set("materials", materialsAndAmount);
//            recipeCompound.set("tags", itemStackTags);
//            recipeCompound.set("ench", ench);
//            recipeList.add(recipeCompound);
//            entityDetails.set("recipes", recipeList);

//            NBTTagList itemStackTags2 = new NBTTagList(); // Store the tags
//            net.minecraft.server.v1_16_R2.ItemStack nmsStack2 = CraftItemStack.asNMSCopy(abstractHorse.getInventory().getSaddle());
//            NBTTagCompound itemStackCompound2 = (nmsStack2.hasTag()) ? nmsStack2.getTag() : new NBTTagCompound();
//            itemStackTags2.add(itemStackCompound2);
//            entityDetails.set("saddle", itemStackTags2);
//
//            if (livingEntity instanceof HorseInventory) {
//                NBTTagList itemStackTags3 = new NBTTagList(); // Store the tags
//                net.minecraft.server.v1_16_R2.ItemStack nmsStack3 = CraftItemStack.asNMSCopy(((HorseInventory)livingEntity).getArmor());
//                NBTTagCompound itemStackCompound3 = (nmsStack3.hasTag()) ? nmsStack3.getTag() : new NBTTagCompound();
//                itemStackTags3.add(itemStackCompound3);
//                entityDetails.set("armor", itemStackTags3);
//            } else if (livingEntity instanceof LlamaInventory) {
//                NBTTagList itemStackTags3 = new NBTTagList(); // Store the tags
//                net.minecraft.server.v1_16_R2.ItemStack nmsStack3 = CraftItemStack.asNMSCopy(((LlamaInventory)livingEntity).getDecor());
//                NBTTagCompound itemStackCompound3 = (nmsStack3.hasTag()) ? nmsStack3.getTag() : new NBTTagCompound();
//                itemStackTags3.add(itemStackCompound3);
//                entityDetails.set("decor", itemStackTags3);
//            }

        } else if (livingEntity instanceof AbstractVillager) {
            AbstractVillager villager = (AbstractVillager) livingEntity;

            if (livingEntity instanceof Villager) {
                Villager villager2 = (Villager) livingEntity;
                entityDetails.setString("profession", villager2.getProfession().name());
                entityDetails.setString("type", villager2.getVillagerType().name());
                entityDetails.setInt("exp", villager2.getVillagerExperience());
                entityDetails.setInt("level", villager2.getVillagerLevel());
            }

            NBTTagList recipeList = new NBTTagList();
            for (org.bukkit.inventory.MerchantRecipe recipe : villager.getRecipes()) {
                List<ItemStack> ingredients = recipe.getIngredients();
                // Store the materials and amounts
                NBTTagList materialsAndAmount = new NBTTagList(); // Holds materials and amount separated by "."
                // Store the tags
                NBTTagList itemStackTags = new NBTTagList();
                for (ItemStack itemStack : ingredients) {
                    materialsAndAmount.add(NBTTagString.a(itemStack.getType().name() + "." + itemStack.getAmount()));

                    net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(itemStack);
                    NBTTagCompound itemStackCompound = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();
                    itemStackTags.add(itemStackCompound);
                }
                int uses = recipe.getUses();
                int maxUses = recipe.getMaxUses();
                boolean experienceReward = recipe.hasExperienceReward();

                net.minecraft.server.v1_16_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(recipe.getResult());
                NBTTagCompound resultTags = (nmsStack.hasTag()) ? nmsStack.getTag() : new NBTTagCompound();

                NBTTagCompound recipeCompound = new NBTTagCompound();
                recipeCompound.setInt("uses", uses);
                recipeCompound.setInt("max uses", maxUses);
                recipeCompound.setBoolean("experience reward", experienceReward);
                recipeCompound.setString("result", recipe.getResult().getType().name() + "." + recipe.getResult().getAmount());
                recipeCompound.set("result tags", resultTags);
                recipeCompound.set("materials", materialsAndAmount);
                recipeCompound.set("tags", itemStackTags);

                recipeList.add(recipeCompound);
            }

            entityDetails.set("recipes", recipeList);
        } else if (livingEntity instanceof PiglinAbstract) {
            PiglinAbstract piglin = (PiglinAbstract) livingEntity;
            entityDetails.setBoolean("convertion", piglin.isConverting());
            if (piglin.isConverting()) entityDetails.setInt("convertionTime", piglin.getConversionTime());
            entityDetails.setBoolean("immune", piglin.isImmuneToZombification());
        }

        // Individual data from entity
        if (livingEntity instanceof Wolf) {
            Wolf wolf = (Wolf) livingEntity;
            entityDetails.setBoolean("angry", wolf.isAngry());
            if (wolf.isTamed())
                entityDetails.set("color", NBTTagString.a(wolf.getCollarColor().name()));
        } else if (livingEntity instanceof Fox) {
            Fox fox = (Fox) livingEntity;
            entityDetails.setString("fox type", fox.getFoxType().name());
        } else if (livingEntity instanceof Sheep) {
            Sheep sheep = (Sheep) livingEntity;
            boolean sheared = sheep.isSheared();
            String color = sheep.getColor().name();
            entityDetails.set("color", NBTTagString.a(color));
            entityDetails.setBoolean("sheared", sheared);
        } else if (livingEntity instanceof Cat) {
            Cat cat = (Cat) livingEntity;
            String catType = cat.getCatType().name();
            entityDetails.setString("cat type", catType);
        } else if (livingEntity instanceof Rabbit) {
            entityDetails.setString("rabbit type", ((Rabbit) livingEntity).getRabbitType().name());
        } else if (livingEntity instanceof Creeper) {
            Creeper creeper = (Creeper) livingEntity;
            entityDetails.setBoolean("charged", creeper.isPowered());
        } else if (livingEntity instanceof Slime) {
            Slime slime = (Slime) livingEntity;
            entityDetails.setInt("size", slime.getSize());
        } else if (livingEntity instanceof ZombieVillager) {
            ZombieVillager zombieVillager = (ZombieVillager) livingEntity;
            String profession = zombieVillager.getVillagerProfession().name();
            entityDetails.setString("profession", profession);
        } else if (livingEntity instanceof Parrot) {
            Parrot parrot = (Parrot) livingEntity;
            Parrot.Variant color = parrot.getVariant();
            entityDetails.setString("variant", color.name());
        } else if (livingEntity instanceof Panda) {
            Panda panda = (Panda) livingEntity;
            entityDetails.setString("hidden gene", panda.getHiddenGene().name());
            entityDetails.setString("main gene", panda.getMainGene().name());
        } else if (livingEntity instanceof Bee) {
            Bee bee = (Bee) livingEntity;
            entityDetails.setBoolean("nectar", bee.hasNectar());
            entityDetails.setBoolean("stung", bee.hasStung());
            entityDetails.setInt("anger", bee.getAnger());
        } else if (livingEntity instanceof Piglin) {
            Piglin piglin = (Piglin) livingEntity;
            entityDetails.setBoolean("hunt", piglin.isAbleToHunt());
        } else if (livingEntity instanceof Strider) {
            Strider strider = (Strider) livingEntity;
            entityDetails.setBoolean("shivering", strider.isShivering());
        }

        // horse types
        if (livingEntity instanceof Llama) {
            Llama abstractHorse = (Llama) livingEntity;
            String color = abstractHorse.getColor().name();
            int strength = abstractHorse.getStrength();
            entityDetails.setString("color", color);
            entityDetails.setInt("strength", strength);
        } else if (livingEntity instanceof Horse) {
            Horse abstractHorse = (Horse) livingEntity;
            String color = abstractHorse.getColor().name();
            String style = abstractHorse.getStyle().name();
            entityDetails.setString("color", color);
            entityDetails.setString("style", style);
        }

        return entityDetails;
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }
}
