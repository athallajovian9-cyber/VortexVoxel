package com.vortex.voxel.meshing;

import com.vortex.voxel.buffer.NativeBufferManager;
import com.vortex.voxel.layout.VertexPacker;

import java.util.Arrays;

/**
 * High-performance 3D Greedy Meshing Compaction Engine.
 * Scans 32x32x32 voxel chunks across 6 primary directions (X+, X-, Y+, Y-, Z+, Z-),
 * identifies adjacent identical block faces, and merges them into optimal planar quads.
 */
public final class GreedyMesher {

    public static final int CHUNK_SIZE = 32;

    // Normal face direction IDs:
    // 0: +X, 1: -X, 2: +Y, 3: -Y, 4: +Z, 5: -Z
    public static final int DIR_POS_X = 0;
    public static final int DIR_NEG_X = 1;
    public static final int DIR_POS_Y = 2;
    public static final int DIR_NEG_Y = 3;
    public static final int DIR_POS_Z = 4;
    public static final int DIR_NEG_Z = 5;

    private final short[] mask = new short[CHUNK_SIZE * CHUNK_SIZE];

    public int generateMesh(short[] voxelGrid, NativeBufferManager nativeBuffer) {
        nativeBuffer.reset();
        int quadCount = 0;

        // Sweep through all 3 coordinate axes
        for (int d = 0; d < 3; d++) {
            int u = (d + 1) % 3;
            int v = (d + 2) % 3;

            int[] x = new int[3];
            int[] q = new int[3];
            q[d] = 1;

            // Iterate through chunk slices
            for (x[d] = -1; x[d] < CHUNK_SIZE;) {
                int n = 0;

                // 1. Compute face differential mask
                for (x[v] = 0; x[v] < CHUNK_SIZE; x[v]++) {
                    for (x[u] = 0; x[u] < CHUNK_SIZE; x[u]++) {
                        short a = (x[d] >= 0) ? getVoxel(voxelGrid, x[0], x[1], x[2]) : 0;
                        short b = (x[d] < CHUNK_SIZE - 1) ? getVoxel(voxelGrid, x[0] + q[0], x[1] + q[1], x[2] + q[2]) : 0;

                        if ((a != 0) == (b != 0)) {
                            mask[n++] = 0;
                        } else if (a != 0) {
                            mask[n++] = a; // Forward face
                        } else {
                            mask[n++] = (short) -b; // Backward face
                        }
                    }
                }

                x[d]++;
                n = 0;

                // 2. Greedy merge adjacent coplanar mask entries
                for (int j = 0; j < CHUNK_SIZE; j++) {
                    for (int i = 0; i < CHUNK_SIZE;) {
                        short c = mask[n];
                        if (c != 0) {
                            int width;
                            for (width = 1; i + width < CHUNK_SIZE && mask[n + width] == c; width++) {}

                            int height;
                            boolean done = false;
                            for (height = 1; j + height < CHUNK_SIZE; height++) {
                                for (int k = 0; k < width; k++) {
                                    if (mask[n + k + height * CHUNK_SIZE] != c) {
                                        done = true;
                                        break;
                                    }
                                }
                                if (done) break;
                            }

                            x[u] = i;
                            x[v] = j;

                            int[] du = new int[3];
                            int[] dv = new int[3];

                            if (c > 0) {
                                du[u] = width;
                                dv[v] = height;
                            } else {
                                c = (short) -c;
                                du[v] = height;
                                dv[u] = width;
                            }

                            int normalId = (c > 0) ? (d * 2) : (d * 2 + 1);
                            emitQuad(nativeBuffer, x, du, dv, normalId, c);
                            quadCount++;

                            // Clear consumed section of mask
                            for (int l = 0; l < height; l++) {
                                for (int k = 0; k < width; k++) {
                                    mask[n + k + l * CHUNK_SIZE] = 0;
                                }
                            }

                            i += width;
                            n += width;
                        } else {
                            i++;
                            n++;
                        }
                    }
                }
            }
        }

        return quadCount;
    }

    private void emitQuad(NativeBufferManager buf, int[] x, int[] du, int[] dv, int normal, int material) {
        int ao = 3;    // Maximum ambient illumination by default
        int light = 15; // Full sunlight

        // Quad Vertex 0
        buf.putInt(VertexPacker.pack(x[0], x[1], x[2], normal, ao, light, 0, material));
        // Quad Vertex 1
        buf.putInt(VertexPacker.pack(x[0] + du[0], x[1] + du[1], x[2] + du[2], normal, ao, light, 1, material));
        // Quad Vertex 2
        buf.putInt(VertexPacker.pack(x[0] + du[0] + dv[0], x[1] + du[1] + dv[1], x[2] + du[2] + dv[2], normal, ao, light, 2, material));
        // Quad Vertex 3
        buf.putInt(VertexPacker.pack(x[0] + dv[0], x[1] + dv[1], x[2] + dv[2], normal, ao, light, 3, material));
    }

    private short getVoxel(short[] grid, int x, int y, int z) {
        if (x < 0 || x >= CHUNK_SIZE || y < 0 || y >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE) return 0;
        return grid[x + y * CHUNK_SIZE + z * CHUNK_SIZE * CHUNK_SIZE];
    }
}
