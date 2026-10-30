package com.zangetsumod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Configuración editable: config/zangetsu_mod.json (se crea con valores por defecto). */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static ModConfig instance = new ModConfig();

    // Zangetsu / Getsuga Tensho
    public int zangetsuCooldownSeconds = 15;
    public float getsugaDamage = 30.0f;            // 30 = 15 corazones
    public double getsugaMaxRange = 40.0;          // bloques
    public double getsugaSpeedBlocksPerTick = 1.5; // velocidad del proyectil

    // A Casa Pa
    public String casaPaKickMessage = "¡A casa pa!";

    // Vanish
    public boolean persistVanish = true;

    // Temporizadores
    public long maxTimerSeconds = 72000;                 // tope por comando (20 h)
    public boolean playerTimerPausesWhileOffline = false; // false: sigue corriendo y avisa al volver
    public boolean notifyPlayerTimerOnJoin = true;

    public static ModConfig get() { return instance; }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("zangetsu_mod.json");
        try {
            if (Files.exists(file)) {
                ModConfig c = GSON.fromJson(Files.readString(file), ModConfig.class);
                if (c != null) instance = c;
            }
            Files.writeString(file, GSON.toJson(instance)); // reescribe para añadir campos nuevos
        } catch (Exception e) {
            System.err.println("[zangetsu_mod] No se pudo leer la config, uso valores por defecto: " + e);
        }
        instance.zangetsuCooldownSeconds = Math.max(0, instance.zangetsuCooldownSeconds);
        instance.getsugaMaxRange = Math.max(1, Math.min(256, instance.getsugaMaxRange));
        instance.getsugaSpeedBlocksPerTick = Math.max(0.2, Math.min(4, instance.getsugaSpeedBlocksPerTick));
        if (instance.casaPaKickMessage == null) instance.casaPaKickMessage = "¡A casa pa!";
    }
}
