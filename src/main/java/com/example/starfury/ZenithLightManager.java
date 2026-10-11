package com.example.starfury;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Provides dynamic handheld lighting for the Zenith Sword.
 * When held in either hand, the sword acts as a real light source (level 15, like a torch)
 * lighting up surrounding blocks without applying Night Vision buff.
 */
@Mod.EventBusSubscriber(modid = StarfuryMod.MODID)
public class ZenithLightManager {
    private static final Map<UUID, BlockPos> ACTIVE_LIGHTS = new ConcurrentHashMap<>();

    public static boolean isHoldingZenith(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        ItemStack main = entity.getMainHandItem();
        ItemStack off = entity.getOffhandItem();
        return (main.getItem() instanceof ZenithSwordItem) || (off.getItem() instanceof ZenithSwordItem);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        Player player = event.player;
        Level level = player.level();
        UUID id = player.getUUID();

        if (player.isAlive() && isHoldingZenith(player)) {
            BlockPos desired = getDesiredLightPos(level, player);
            BlockPos current = ACTIVE_LIGHTS.get(id);

            if (desired != null) {
                if (current != null && !current.equals(desired)) {
                    removeLight(level, current);
                }
                if (!desired.equals(current) || !level.getBlockState(desired).is(Blocks.LIGHT)) {
                    placeLight(level, desired);
                    ACTIVE_LIGHTS.put(id, desired);
                }
            } else if (current != null) {
                removeLight(level, current);
                ACTIVE_LIGHTS.remove(id);
            }
        } else {
            BlockPos current = ACTIVE_LIGHTS.remove(id);
            if (current != null) {
                removeLight(level, current);
            }
        }
    }

    private static BlockPos getDesiredLightPos(Level level, Player player) {
        BlockPos feet = player.blockPosition();
        if (canPlaceLight(level, feet)) {
            return feet;
        }
        BlockPos head = feet.above();
        if (canPlaceLight(level, head)) {
            return head;
        }
        return null;
    }

    private static boolean canPlaceLight(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(Blocks.LIGHT) || state.is(Blocks.WATER);
    }

    private static void placeLight(Level level, BlockPos pos) {
        boolean isWater = level.getFluidState(pos).is(Fluids.WATER) || level.getBlockState(pos).is(Blocks.WATER);
        BlockState lightState = Blocks.LIGHT.defaultBlockState()
                .setValue(LightBlock.LEVEL, 15)
                .setValue(LightBlock.WATERLOGGED, isWater);
        level.setBlock(pos, lightState, 3);
    }

    public static void removeLight(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.LIGHT)) {
            boolean waterlogged = state.hasProperty(LightBlock.WATERLOGGED) && state.getValue(LightBlock.WATERLOGGED);
            level.setBlock(pos, waterlogged ? Blocks.WATER.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        }
    }

    public static void cleanup(Player player) {
        if (player == null || player.level().isClientSide) {
            return;
        }
        BlockPos pos = ACTIVE_LIGHTS.remove(player.getUUID());
        if (pos != null) {
            removeLight(player.level(), pos);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        cleanup(event.getEntity());
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        cleanup(event.getEntity());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            cleanup(player);
        }
    }
}
