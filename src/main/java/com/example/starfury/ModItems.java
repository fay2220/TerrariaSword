package com.example.starfury;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, StarfuryMod.MODID);

    // Total melee damage = 1 (player base) + 3 (Tiers.DIAMOND bonus) + 3 (modifier) = 7
    public static final RegistryObject<Item> STARFURY = ITEMS.register("starfury",
            () -> new StarfurySwordItem(Tiers.DIAMOND, 3, -2.4F, new Item.Properties()));

    // Only used as the visual (2D star) of the projectile; not in any creative tab.
    public static final RegistryObject<Item> FALLEN_STAR = ITEMS.register("fallen_star",
            () -> new Item(new Item.Properties()));

    // Total melee damage = 1 (player base) + 4 (Tiers.NETHERITE bonus) + 20 (modifier) = 25
    // Attack speed = 4.0 (player base) - 2.0 = 2.0 hits per second (a normal sword is 1.6).
    public static final RegistryObject<Item> ZENITH_SWORD = ITEMS.register("zenith_sword",
            () -> new ZenithSwordItem(Tiers.NETHERITE, 20, -2.0F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

    // Recoloured "spectral" copies of the Zenith, used only as the look of the flying blades.
    // Not in any creative tab. Order must match ZenithSwordEntity.VARIANT_COLORS (index 2..5).
    public static final RegistryObject<Item> ZENITH_PHANTOM_EMERALD = phantom("zenith_phantom_emerald");
    public static final RegistryObject<Item> ZENITH_PHANTOM_EMBER = phantom("zenith_phantom_ember");
    public static final RegistryObject<Item> ZENITH_PHANTOM_ROSE = phantom("zenith_phantom_rose");
    public static final RegistryObject<Item> ZENITH_PHANTOM_AZURE = phantom("zenith_phantom_azure");

    private static RegistryObject<Item> phantom(String id) {
        return ITEMS.register(id, () -> new Item(new Item.Properties().stacksTo(1)));
    }
}
