package fathertoast.coopmod.test.client;

import fathertoast.coopmod.common.core.CoOpMod;
import fathertoast.coopmod.test.common.TestCommonRegister;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber( value = Dist.CLIENT, modid = CoOpMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD )
public class TestClientRegister {
    
    @SubscribeEvent
    static void buildCreativeContents( BuildCreativeModeTabContentsEvent event ) {
        if( event.getTabKey() == TestCommonRegister.TEST_TAB.getKey() ) {
            int[] modifiers = { 1, 2, 10, 20, 40, 60 };
            Item[] items = { Items.GOLDEN_HELMET, Items.GOLDEN_CHESTPLATE, Items.GOLDEN_LEGGINGS, Items.GOLDEN_BOOTS };
            for( int h : modifiers ) {
                for( Item item : items ) {
                    ItemStack stack = new ItemStack( item );
                    addModifier( stack, Attributes.MAX_HEALTH, h,
                            AttributeModifier.Operation.ADDITION );
                    event.accept( stack );
                }
            }
        }
    }
    
    /** Adds a custom attribute modifier to the item stack that only applies while in an appropriate equipment slot. */
    static void addModifier( ItemStack stack, Attribute attribute, double value, AttributeModifier.Operation operation ) {
        stack.addAttributeModifier( attribute,
                new AttributeModifier( CoOpMod.MOD_ID + ":" + stack.getItem().getDescriptionId() +
                        " item bonus", value, operation ),
                Mob.getEquipmentSlotForItem( stack ) );
    }
}