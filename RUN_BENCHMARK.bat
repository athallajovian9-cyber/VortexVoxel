@echo off
setlocal
cd /d "%~dp0"
echo ============================================================
echo   Vortex Voxel Engine // Vulkan Off-Heap Compaction Runner
echo ============================================================
java -cp bin com.vortex.voxel.Main
pause
