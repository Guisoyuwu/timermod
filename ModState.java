package com.zangetsumod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Estado persistente por mundo: <mundo>/zangetsu_mod_state.json */
public class ModState {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ModState instance = new ModState();
    private static Path file;

    public boolean globalActive = false;
    public long globalRemainingTicks = 0;
    public Map<String, Long> playerTimers = new HashMap<>();   // UUID -> ticks restantes
    public Set<String> pendingFinished = new HashSet<>();      // UUID de quienes deben recibir "(Tiempo Acabado)"
    public Map<String, String> vanished = new HashMap<>();     // UUID -> nombre

    public static ModState get() { return instance; }

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("zangetsu_mod_state.json");
        instance = new ModState();
        try {
            if (Files.exists(file)) {
                ModState s = GSON.fromJson(Files.readString(file), ModState.class);
                if (s != null) instance = s;
            }
        } catch (Exception e) {
            System.err.println("[zangetsu_mod] No se pudo leer el estado: " + e);
        }
        if (instance.playerTimers == null) instance.playerTimers = new HashMap<>();
        if (instance.pendingFinished == null) instance.pendingFinished = new HashSet<>();
        if (instance.vanished == null) instance.vanished = new HashMap<>();
    }

    public static void save() {
        if (file == null) return;
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(instance));
            Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            System.err.println("[zangetsu_mod] No se pudo guardar el estado: " + e);
        }
    }
}
