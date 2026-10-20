# Rainy Water 1.1.0 (Fabric 1.21.4, client-only)
- Rain works on ANY server (client-side), even when the server has clear weather.
- Key V cycles: Off / Light / Rain / Storm (rebind in Controls).
- Dark murky water + ripples on water.
Build: JDK 21 -> `gradle wrapper` once -> `./gradlew build` -> build/libs/rainywater-1.1.0.jar
Put into mods/ with Fabric API. For reflections use Iris + a shader pack.
Note: in cold biomes vanilla shows snow instead of rain; deserts have no rain.

## 1.2.0 - TargetESP
Pink marker around the entity you attack / aim at. Key G cycles Off / Ring / Orbit / Scanner.
Depth-tested: not visible through walls. Jar: build/libs/rainywater-1.2.0.jar
