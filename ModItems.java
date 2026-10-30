package com.zangetsumod;

import com.zangetsumod.item.ZangetsuItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Function;

public final class ModItems {
    public static Item A_CASA_PA;
    public static Item TP_0_0;
    public static Item ZANGETSU;

    private ModItems() {}

    private static <T extends Item> T register(String name, Function<Item.Properties, T> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, ZangetsuMod.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }

    public static void register() {
        A_CASA_PA = register("a_casa_pa", Item::new,
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        TP_0_0 = register("tp_0_0", Item::new,
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
        ZANGETSU = register("zangetsu", ZangetsuItem::new,
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).sword(ToolMaterial.NETHERITE, 6.0f, -2.6f));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.COMBAT).register(e -> {
            e.accept(ZANGETSU);
            e.accept(A_CASA_PA);
            e.accept(TP_0_0);
        });
    }
}
