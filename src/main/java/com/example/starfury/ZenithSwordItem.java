package com.example.starfury;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * Zenith (Terraria).
 * Left click: normal sword hit, 25 damage.
 * Hold right click: launches a spectral blade every {@link #LAUNCH_INTERVAL} ticks (5 per second). Each blade loops
 * out up to 20 blocks toward where the player is looking and back in 1 second, hitting everything on its path for
 * {@link ZenithSwordEntity#DAMAGE}. No cooldown: keeps going until right click is released.
 */
public class ZenithSwordItem extends SwordItem {
    /** Ticks between two blades (4 ticks = 5 blades per second). */
    public static final int LAUNCH_INTERVAL = 4;
    /** Every this many ticks of holding, a big sonic boom (sound + vapor cone + speed lines) goes off. */
    public static final int BIG_BOOM_INTERVAL = 40;
    /** Effectively "forever": the ability runs until the player lets go of right click. */
    private static final int USE_DURATION = 72000;

    public ZenithSwordItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    /** Called every tick while right click is held (both sides; gameplay only runs on the server). */
    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        int used = this.getUseDuration(stack) - remainingUseTicks;
        if (used % LAUNCH_INTERVAL != 0) {
            return;
        }

        boolean bigBoom = used % BIG_BOOM_INTERVAL == 0;
        boolean firstBoom = used == 0;
        ZenithSwordEntity.launch(serverLevel, user, used / LAUNCH_INTERVAL, bigBoom, firstBoom);

        float pitch = 1.3F + serverLevel.random.nextFloat() * 0.5F;
        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.55F, pitch);
        serverLevel.playSound(null, user.getX(), user.getY(), user.getZ(),
                SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 0.45F, pitch + 0.2F);
        if (used % (LAUNCH_INTERVAL * 2) == 0) {
            user.swing(user.getUsedItemHand(), true); // arm keeps swinging while the storm is going
        }
    }
}
