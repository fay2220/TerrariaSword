package com.example.starfury;

import net.minecraft.world.item.Item;
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
}
