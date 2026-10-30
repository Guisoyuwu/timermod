package com.zangetsumod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zangetsumod.system.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Predicate;

public final class ModCommands {
    private ModCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        // OP nivel 2 (gamemasters) o superior.
        Predicate<CommandSourceStack> op = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);

        d.register(Commands.literal("timer").requires(op)
                .then(Commands.literal("stop").executes(ModCommands::timerStop))
                .then(Commands.argument("duration", StringArgumentType.word()).executes(ModCommands::timerStart)));

        d.register(Commands.literal("timerplayer").requires(op)
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("duration", StringArgumentType.word()).executes(ModCommands::timerPlayer))));

        d.register(Commands.literal("tagvanish").requires(op).executes(c -> tag(c, true)));
        d.register(Commands.literal("untagvanish").requires(op).executes(c -> tag(c, false)));
        d.register(Commands.literal("vanish").requires(op).executes(c -> vanish(c, true)));
        d.register(Commands.literal("unvanish").requires(op).executes(c -> vanish(c, false)));
    }

    private static int timerStart(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        long ticks = DurationParser.parseTicks(StringArgumentType.getString(c, "duration"));
        TimerManager.startGlobal(c.getSource().getServer(), ticks);
        c.getSource().sendSuccess(() -> Component.literal("Temporizador global iniciado: " + ticks / 20 + "s"), true);
        return 1;
    }

    private static int timerStop(CommandContext<CommandSourceStack> c) {
        TimerManager.stopGlobal(c.getSource().getServer());
        c.getSource().sendSuccess(() -> Component.literal("Temporizador global detenido."), true);
        return 1;
    }

    private static int timerPlayer(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "target");
        long ticks = DurationParser.parseTicks(StringArgumentType.getString(c, "duration"));
        TimerManager.startPlayer(target, ticks);
        c.getSource().sendSuccess(() -> Component.literal("Temporizador de " + ticks / 20 + "s iniciado para " + target.getScoreboardName()), true);
        return 1;
    }

    private static int tag(CommandContext<CommandSourceStack> c, boolean hide) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        if (hide) {
            String err = TagVanishManager.enable(c.getSource().getServer(), p);
            if (err != null) { c.getSource().sendFailure(Component.literal(err)); return 0; }
            c.getSource().sendSuccess(() -> Component.literal("Tu nombre ahora está oculto sobre tu cabeza."), false);
        } else {
            boolean ok = TagVanishManager.disable(c.getSource().getServer(), p);
            c.getSource().sendSuccess(() -> Component.literal(ok ? "Tu nombre vuelve a ser visible." : "Tu nombre ya era visible."), false);
        }
        return 1;
    }

    private static int vanish(CommandContext<CommandSourceStack> c, boolean on) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        boolean already = VanishManager.isVanished(p);
        if (on == already) {
            c.getSource().sendFailure(Component.literal(on ? "Ya estás en vanish." : "No estás en vanish."));
            return 0;
        }
        if (on) VanishManager.vanish(c.getSource().getServer(), p);
        else VanishManager.unvanish(c.getSource().getServer(), p);
        c.getSource().sendSuccess(() -> Component.literal(on ? "Vanish activado." : "Vanish desactivado."), false);
        return 1;
    }
}
