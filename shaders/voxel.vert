#version 450
#extension GL_ARB_separate_shader_objects : enable

// Vulkan 1.3 / GLSL 450 Push Constant Uniform Buffer
layout(push_constant) uniform PushConstants {
    mat4 uViewProj;
    vec3 uChunkWorldPos;
} pc;

// Single 32-bit packed integer per vertex
// Format: VK_FORMAT_R32_UINT
layout(location = 0) in uint inPackedVertex;

// Interpolated stage outputs
layout(location = 0) out vec3 outNormal;
layout(location = 1) out vec2 outUV;
layout(location = 2) out float outAO;
layout(location = 3) out float outLight;
layout(location = 4) out flat uint outMaterialID;

// Normal vector lookup table
const vec3 NORMALS[6] = vec3[](
    vec3( 1.0,  0.0,  0.0), // 0: +X
    vec3(-1.0,  0.0,  0.0), // 1: -X
    vec3( 0.0,  1.0,  0.0), // 2: +Y
    vec3( 0.0, -1.0,  0.0), // 3: -Y
    vec3( 0.0,  0.0,  1.0), // 4: +Z
    vec3( 0.0,  0.0, -1.0)  // 5: -Z
);

// Quad UV coordinate lookup table
const vec2 UVS[4] = vec2[](
    vec2(0.0, 0.0), // 0: Bottom-Left
    vec2(1.0, 0.0), // 1: Bottom-Right
    vec2(1.0, 1.0), // 2: Top-Right
    vec2(0.0, 1.0)  // 3: Top-Left
);

void main() {
    // 1. Bitfield extraction from 32-bit integer primitive
    uint posX     = bitfieldExtract(inPackedVertex, 0, 6);
    uint posY     = bitfieldExtract(inPackedVertex, 6, 6);
    uint posZ     = bitfieldExtract(inPackedVertex, 12, 6);
    uint normIdx  = bitfieldExtract(inPackedVertex, 18, 3);
    uint aoIdx    = bitfieldExtract(inPackedVertex, 21, 2);
    uint lightIdx = bitfieldExtract(inPackedVertex, 23, 4);
    uint uvIdx    = bitfieldExtract(inPackedVertex, 27, 2);
    uint matIdx   = bitfieldExtract(inPackedVertex, 29, 3);

    // 2. Compute local & world space positions
    vec3 localPos = vec3(float(posX), float(posY), float(posZ));
    vec3 worldPos = localPos + pc.uChunkWorldPos;

    // 3. Forward interpolated values to fragment stage
    outNormal     = NORMALS[normIdx];
    outUV         = UVS[uvIdx];
    outAO         = float(aoIdx) / 3.0;        // Normalize [0..3] -> [0.0..1.0]
    outLight      = float(lightIdx) / 15.0;     // Normalize [0..15] -> [0.0..1.0]
    outMaterialID = matIdx;

    // 4. Vulkan clip-space transformation (Y-inverted relative to OpenGL)
    gl_Position = pc.uViewProj * vec4(worldPos, 1.0);
}
