package com.zangetsumod.system;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.zangetsumod.ModConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Acepta 20s, 20m, 20hs (también 20h). Devuelve ticks. */
public final class DurationParser {
    private static final Pattern P = Pattern.compile("^(\\d{1,9})(s|m|hs|h)$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {}

    public static long parseTicks(String text) throws CommandSyntaxException {
        Matcher m = P.matcher(text.trim());
        if (!m.matches()) throw err("Formato inválido. Usa 20s, 20m o 20hs.");
        long n = Long.parseLong(m.group(1));
        long seconds = switch (m.group(2).toLowerCase()) {
            case "s" -> n;
            case "m" -> n * 60;
            default -> n * 3600;
        };
        if (seconds <= 0) throw err("La duración debe ser mayor que 0.");
        long max = ModConfig.get().maxTimerSeconds;
        if (seconds > max) throw err("Duración máxima permitida: " + max + " segundos.");
        return seconds * 20L;
    }

    private static CommandSyntaxException err(String msg) {
        return new SimpleCommandExceptionType(new LiteralMessage(msg)).create();
    }
}
