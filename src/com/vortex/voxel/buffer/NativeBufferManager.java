package com.vortex.voxel.buffer;

import java.lang.reflect.Field;
import sun.misc.Unsafe;

/**
 * Off-Heap Native Buffer Pool bypassing JVM Garbage Collection entirely.
 * Manages raw host-visible / staging memory pointers for Vulkan / GPU DMA transfers.
 */
public final class NativeBufferManager implements AutoCloseable {

    private static final Unsafe UNSAFE;

    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to access sun.misc.Unsafe for off-heap buffer management", e);
        }
    }

    private final long address;
    private final long capacityBytes;
    private long writeOffset = 0;
    private boolean freed = false;

    public NativeBufferManager(long capacityBytes) {
        this.capacityBytes = capacityBytes;
        this.address = UNSAFE.allocateMemory(capacityBytes);
        UNSAFE.setMemory(this.address, capacityBytes, (byte) 0);
    }

    public long getAddress() {
        return address;
    }

    public long getCapacityBytes() {
        return capacityBytes;
    }

    public long getWriteOffset() {
        return writeOffset;
    }

    public void reset() {
        writeOffset = 0;
    }

    /**
     * Appends a packed 32-bit integer vertex directly to off-heap memory.
     */
    public void putInt(int value) {
        if (writeOffset + 4 > capacityBytes) {
            throw new IndexOutOfBoundsException("Off-heap buffer capacity exceeded: " + writeOffset);
        }
        UNSAFE.putInt(address + writeOffset, value);
        writeOffset += 4;
    }

    /**
     * Bulk writes packed 32-bit vertices from an on-heap array into native memory.
     */
    public void writeBulk(int[] data, int offset, int length) {
        long bytesToCopy = (long) length * 4;
        if (writeOffset + bytesToCopy > capacityBytes) {
            throw new IndexOutOfBoundsException("Bulk write exceeds off-heap capacity");
        }
        long dest = address + writeOffset;
        for (int i = 0; i < length; i++) {
            UNSAFE.putInt(dest + ((long) i * 4), data[offset + i]);
        }
        writeOffset += bytesToCopy;
    }

    @Override
    public void close() {
        if (!freed) {
            UNSAFE.freeMemory(address);
            freed = true;
        }
    }
}
