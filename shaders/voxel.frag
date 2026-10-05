#version 450
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec3 inNormal;
layout(location = 1) in vec2 inUV;
layout(location = 2) in float inAO;
layout(location = 3) in float inLight;
layout(location = 4) in flat uint inMaterialID;

// Final attachment output
layout(location = 0) out vec4 outFragColor;

// Material palette (Material ID [0..7])
const vec3 MATERIAL_PALETTE[8] = vec3[](
    vec3(0.0, 0.0, 0.0), // 0: Air
    vec3(0.2, 0.7, 0.3), // 1: Grass
    vec3(0.5, 0.3, 0.1), // 2: Dirt
    vec3(0.5, 0.5, 0.5), // 3: Stone
    vec3(0.8, 0.7, 0.4), // 4: Sand
    vec3(0.1, 0.8, 0.9), // 5: Diamond / Crystal
    vec3(0.9, 0.3, 0.2), // 6: Lava / Magma
    vec3(0.2, 0.4, 0.9)  // 7: Water
);

void main() {
    // Look up base albedo from material ID
    vec3 baseColor = MATERIAL_PALETTE[clamp(inMaterialID, 0u, 7u)];

    // Directional sunlight diffuse calculation
    vec3 sunDir = normalize(vec3(0.4, 0.9, 0.3));
    float ndotl = max(dot(inNormal, sunDir), 0.15);

    // Apply ambient occlusion curve & sunlight intensity
    float aoFactor = mix(0.35, 1.0, inAO);
    float finalLight = ndotl * inLight * aoFactor;

    // Output shaded fragment
    outFragColor = vec4(baseColor * finalLight, 1.0);
}
