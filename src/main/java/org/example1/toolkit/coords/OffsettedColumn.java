package org.example1.toolkit.coords;

import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.world.chunk.BaseChunk;
import com.github.retrooper.packetevents.protocol.world.chunk.Column;
import com.github.retrooper.packetevents.protocol.world.chunk.HeightmapType;
import com.github.retrooper.packetevents.protocol.world.chunk.TileEntity;

import java.util.Map;

public final class OffsettedColumn extends Column {

    private final Column inner;
    private final FixedOffset offset;

    public OffsettedColumn(Column inner, FixedOffset offset) {
        super(0, 0, false, new BaseChunk[0], new TileEntity[0]);
        this.inner = inner;
        this.offset = offset;
    }

    @Override
    public int getX() {
        return inner.getX() - offset.chunkX();
    }

    @Override
    public int getZ() {
        return inner.getZ() - offset.chunkZ();
    }

    @Override
    public boolean isFullChunk() {
        return inner.isFullChunk();
    }

    @Override
    public BaseChunk[] getChunks() {
        return inner.getChunks();
    }

    @Override
    public TileEntity[] getTileEntities() {
        return inner.getTileEntities();
    }

    @Override
    public boolean hasHeightMaps() {
        return inner.hasHeightMaps();
    }

    @Override
    public NBTCompound getHeightMaps() {
        return inner.getHeightMaps();
    }

    @Override
    public Map<HeightmapType, long[]> getHeightmaps() {
        return inner.getHeightmaps();
    }

    @Override
    public boolean hasBiomeData() {
        return inner.hasBiomeData();
    }

    @Override
    public int[] getBiomeDataInts() {
        return inner.getBiomeDataInts();
    }

    @Override
    public byte[] getBiomeDataBytes() {
        return inner.getBiomeDataBytes();
    }
}
