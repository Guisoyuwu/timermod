package com.zangetsumod.system;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

/**
 * Oculta SOLO el nombre sobre la cabeza usando un equipo de scoreboard con nameTagVisibility = NEVER.
 * El jugador sigue en TAB y visible. Limitación: un jugador solo puede estar en un equipo a la vez.
 */
public final class TagVanishManager {
    public static final String TEAM = "zm_tagvanish";

    private TagVanishManager() {}

    /** @return null si OK, o un mensaje de error. */
    public static String enable(MinecraftServer server, ServerPlayer p) {
        Scoreboard sb = server.getScoreboard();
        PlayerTeam t = sb.getPlayerTeam(TEAM);
        if (t == null) t = sb.addPlayerTeam(TEAM);
        t.setNameTagVisibility(Team.Visibility.NEVER);
        Team current = p.getTeam();
        if (current != null && !TEAM.equals(current.getName())) {
            return "Ya perteneces al equipo '" + current.getName() + "'; sal de él antes de usar /tagvanish.";
        }
        sb.addPlayerToTeam(p.getScoreboardName(), t);
        return null;
    }

    public static boolean disable(MinecraftServer server, ServerPlayer p) {
        Scoreboard sb = server.getScoreboard();
        PlayerTeam t = sb.getPlayerTeam(TEAM);
        if (t == null) return false;
        Team current = p.getTeam();
        if (current == null || !TEAM.equals(current.getName())) return false;
        sb.removePlayerFromTeam(p.getScoreboardName(), t);
        return true;
    }
}
