package io.github.roman16.backcountry.fire;

import io.github.roman16.backcountry.Balance;
import io.github.roman16.backcountry.Things;
import io.github.roman16.backcountry.body.Calendar;
import io.github.roman16.backcountry.body.Climate;
import io.github.roman16.backcountry.guide.Guide;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class WoodpileEntity extends BlockEntity {
    private static final int STEP_TICKS = 20;
    private static final double TICKS_AN_HOUR = 1000;
    private static final double HOURS_A_DAY = 24;
    private final List<Piece> pieces = new ArrayList<>();
    private long clock;
    private boolean awake;

    private static final class Piece {
        final ItemStack wood;
        double seasoning;

        Piece(ItemStack wood, double seasoning) {
            this.wood = wood;
            this.seasoning = seasoning;
        }

        ItemStack taken() {
            return FirewoodItem.seasoned(wood.copy(), seasoning);
        }
    }

    public WoodpileEntity(BlockPos pos, BlockState state) {
        super(Things.WOODPILE_ENTITY.get(), pos, state);
    }

    public void add(ItemStack piece) {
        pieces.add(new Piece(piece.copyWithCount(1), FirewoodItem.seasoning(piece)));
        shown();
    }

    public ItemStack take() {
        if (pieces.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack piece = pieces.removeLast().taken();
        shown();
        return piece;
    }

    public List<ItemStack> pieces() {
        return pieces.stream().map(Piece::taken).toList();
    }

    public double seasoning() {
        return pieces.stream().mapToDouble(piece -> piece.seasoning).average().orElse(0);
    }

    public void dry(double hours) {
        if (level == null || Fire.rainedOn(level, worldPosition)) {
            return;
        }
        Balance.Firewood firewood = Balance.get().firewood();
        double frozen = Climate.airC(level, worldPosition, Calendar.day(level)) < 0 ? firewood.frozenDrying() : 1;
        double gained = firewood.dryPerDay() / HOURS_A_DAY * frozen * hours;
        for (Piece piece : pieces) {
            piece.seasoning = Math.min(1, piece.seasoning + gained);
        }
        setChanged();
    }

    public void tell(Player player) {
        if (player != null) {
            player.displayClientMessage(pieces.isEmpty() ? Component.translatable("message.backcountry.woodpile_empty")
                    : Component.translatable("message.backcountry.woodpile", pieces.size(), Balance.get().firewood().pilePieces(), FirewoodItem.percent(seasoning())), true);
        }
    }

    private void shown() {
        setChanged();
        if (level == null) {
            return;
        }
        if (pieces.isEmpty()) {
            level.setBlockAndUpdate(worldPosition, Blocks.AIR.defaultBlockState());
        } else if (getBlockState().getValue(WoodpileBlock.PIECES) != pieces.size()) {
            level.setBlockAndUpdate(worldPosition, getBlockState().setValue(WoodpileBlock.PIECES, pieces.size()));
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, WoodpileEntity woodpile) {
        long now = level.getDayTime();
        if (!woodpile.awake) {
            woodpile.awake = true;
            woodpile.clock = now;
        }
        if ((level.getGameTime() + pos.asLong()) % STEP_TICKS != 0) {
            return;
        }
        double hours = Math.max(0, now - woodpile.clock) / TICKS_AN_HOUR;
        woodpile.clock = now;
        if (hours > 0) {
            woodpile.dry(hours);
        }
        if (level.getServer() != null) {
            Guide.woodpile(level.getServer(), pos, Guide.dry(woodpile.pieces()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        ListTag list = new ListTag();
        for (Piece piece : pieces) {
            CompoundTag entry = new CompoundTag();
            entry.put("wood", piece.wood.save(provider));
            entry.putDouble("seasoning", piece.seasoning);
            list.add(entry);
        }
        tag.put("pieces", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        pieces.clear();
        for (Tag entry : tag.getList("pieces", Tag.TAG_COMPOUND)) {
            CompoundTag piece = (CompoundTag) entry;
            ItemStack.parse(provider, piece.getCompound("wood")).ifPresent(wood -> pieces.add(new Piece(wood, piece.getDouble("seasoning"))));
        }
    }
}
