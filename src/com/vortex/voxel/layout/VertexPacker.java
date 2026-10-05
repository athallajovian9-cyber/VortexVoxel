package com.vortex.voxel.layout;

/**
 * Packs all vertex attributes into a single primitive 32-bit integer (4 bytes):
 * - X Coordinate: 6 bits  [0..63]   (Bits 0 - 5)
 * - Y Coordinate: 6 bits  [0..63]   (Bits 6 - 11)
 * - Z Coordinate: 6 bits  [0..63]   (Bits 12 - 17)
 * - Face Normal:  3 bits  [0..5]    (Bits 18 - 20)  (+X, -X, +Y, -Y, +Z, -Z)
 * - Ambient Occ:  2 bits  [0..3]    (Bits 21 - 22)  (AO intensity levels)
 * - Light Level:  4 bits  [0..15]   (Bits 23 - 26)  (Sunlight / block light)
 * - UV Index:     2 bits  [0..3]    (Bits 27 - 28)  (Quad corner uv selection)
 * - Material ID:  3 bits  [0..7]    (Bits 29 - 31)  (Sub-chunk texture / atlas index)
 */
public final class VertexPacker {

    private VertexPacker() {}

    public static int pack(int x, int y, int z, int normal, int ao, int light, int uv, int material) {
        return (x & 0x3F)
             | ((y & 0x3F) << 6)
             | ((z & 0x3F) << 12)
             | ((normal & 0x07) << 18)
             | ((ao & 0x03) << 21)
             | ((light & 0x0F) << 23)
             | ((uv & 0x03) << 27)
             | ((material & 0x07) << 29);
    }

    public static int getX(int packed) { return packed & 0x3F; }
    public static int getY(int packed) { return (packed >>> 6) & 0x3F; }
    public static int getZ(int packed) { return (packed >>> 12) & 0x3F; }
    public static int getNormal(int packed) { return (packed >>> 18) & 0x07; }
    public static int getAO(int packed) { return (packed >>> 21) & 0x03; }
    public static int getLight(int packed) { return (packed >>> 23) & 0x0F; }
    public static int getUV(int packed) { return (packed >>> 27) & 0x03; }
    public static int getMaterial(int packed) { return (packed >>> 29) & 0x07; }
}
