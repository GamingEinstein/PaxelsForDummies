package net.gamingeinstein.paxelsfordummies.item;

import net.gamingeinstein.paxelsfordummies.registries.ModItems;
import net.gamingeinstein.paxelsfordummies.util.ModTags;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class PaxelItem extends DiggerItemWithoutDurability {
    private static final Set<ItemAbility> DEFAULT_PAXEL_ACTIONS = Util.make(Collections.newSetFromMap(new IdentityHashMap<>()), actions -> {
        actions.addAll(ItemAbilities.DEFAULT_PICKAXE_ACTIONS);
        actions.addAll(ItemAbilities.DEFAULT_SHOVEL_ACTIONS);
        actions.addAll(ItemAbilities.DEFAULT_AXE_ACTIONS);
    });

    public PaxelItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, ModTags.Blocks.MINEABLE_WITH_PAXEL,
			properties.durability((int)(tier.getUses() * 0.75f * 3)).attributes(createAttributes(tier, attackDamage, attackSpeed)));
    }

    // This is just both of the useOn methods in the Axe and Shovel classes
    // I took this from Mekanism's code since it was the simplest and most reliable, but anyone can do this with some tinkering
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        BlockPos blockpos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState blockstate = world.getBlockState(blockpos);
        BlockState resultToSet = useAsAxe(blockstate, context);
        if (resultToSet == null) {
            // We can't strip the block that was right-clicked, so we use it as a shovel
            if (context.getClickedFace() == Direction.DOWN) {
				return InteractionResult.PASS;
			}
            BlockState foundResult = blockstate.getToolModifiedState(context, ItemAbilities.SHOVEL_FLATTEN, false);
            if (foundResult != null && world.isEmptyBlock(blockpos.above())) {
                // Flatten the block as a shovel
                world.playSound(player, blockpos, SoundEvents.SHOVEL_FLATTEN, SoundSource.BLOCKS, 1.0F, 1.0F);
                resultToSet = foundResult;
            } else {
                resultToSet = blockstate.getToolModifiedState(context, ItemAbilities.SHOVEL_DOUSE, false);
                // Use the paxel as a shovel to extinguish a campfire
                if (resultToSet != null && !world.isClientSide) {
					world.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, blockpos, 0);
				}
            }
            if (resultToSet == null) {
				return InteractionResult.PASS;
			}
        }
        if (!world.isClientSide) {
            ItemStack stack = context.getItemInHand();
            if (player instanceof ServerPlayer serverPlayer) {
				CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger(serverPlayer, blockpos, stack);
			}
            world.setBlock(blockpos, resultToSet, Block.UPDATE_ALL_IMMEDIATE);
            world.gameEvent(GameEvent.BLOCK_CHANGE, blockpos, GameEvent.Context.of(player, resultToSet));
            if (player != null) {
				stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(context.getHand()));
			}
        }
        return InteractionResult.sidedSuccess(world.isClientSide);
    }

    private BlockState useAsAxe(BlockState state, UseOnContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState resultToSet = state.getToolModifiedState(context, ItemAbilities.AXE_STRIP, false);
        if (resultToSet != null) {
            world.playSound(player, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
            return resultToSet;
        }
        resultToSet = state.getToolModifiedState(context, ItemAbilities.AXE_SCRAPE, false);
        if (resultToSet != null) {
            world.playSound(player, pos, SoundEvents.AXE_SCRAPE, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.levelEvent(player, LevelEvent.PARTICLES_SCRAPE, pos, 0);
            return resultToSet;
        }
        resultToSet = state.getToolModifiedState(context, ItemAbilities.AXE_WAX_OFF, false);
        if (resultToSet != null) {
            world.playSound(player, pos, SoundEvents.AXE_WAX_OFF, SoundSource.BLOCKS, 1.0F, 1.0F);
            world.levelEvent(player, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
            return resultToSet;
        }
        return null;
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ItemAbility action) {
        return DEFAULT_PAXEL_ACTIONS.contains(action);
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?> recipeType) {
        if (this == ModItems.WOODEN_PAXEL.get())
            return 200; // 200 ticks per item smelted
        return 0;
    }

    // Is this necessary? Who knows...
    public boolean isFireResistant() {
        return (this == ModItems.NETHERITE_PAXEL.get());
    }
}
