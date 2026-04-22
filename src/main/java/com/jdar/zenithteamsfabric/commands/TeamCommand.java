package com.jdar.zenithteamsfabric.commands;

import com.jdar.zenithteamsfabric.ZenithTeamsFabric;
import com.jdar.zenithteamsfabric.manager.TeamManager;
import com.jdar.zenithteamsfabric.ui.ScoreboardEngine;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class TeamCommand {

    private static final List<String> VALID_COLORS = List.of("RED", "BLUE", "GREEN", "YELLOW", "AQUA", "GOLD", "LIGHT_PURPLE", "WHITE", "GRAY");

    private static final SuggestionProvider<ServerCommandSource> COLOR_SUGGESTIONS = (context, builder) -> {
        String remaining = builder.getRemaining().toUpperCase();
        for (String color : VALID_COLORS) {
            if (color.startsWith(remaining)) builder.suggest(color);
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("zt")
                .executes(TeamCommand::executeHelp)
                .then(CommandManager.literal("help").executes(TeamCommand::executeHelp))

                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .then(CommandManager.argument("name", StringArgumentType.string())
                                        .then(CommandManager.argument("color", StringArgumentType.word())
                                                .suggests(COLOR_SUGGESTIONS)
                                                .executes(TeamCommand::executeCreate)
                                        )
                                )
                        )
                )
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(TeamCommand::executeInvite)
                        )
                )
                .then(CommandManager.literal("accept")
                        .then(CommandManager.argument("input", StringArgumentType.word())
                                .executes(TeamCommand::executeAccept)
                        )
                )
                .then(CommandManager.literal("leave").executes(TeamCommand::executeLeave))
                .then(CommandManager.literal("disband").executes(TeamCommand::executeDisband))
                .then(CommandManager.literal("chat").executes(TeamCommand::executeChat))
                .then(CommandManager.literal("chest").executes(TeamCommand::executeChest))
                .then(CommandManager.literal("home").executes(TeamCommand::executeHome))
                .then(CommandManager.literal("sethome").executes(TeamCommand::executeSetHome))
        );
    }

    private static int executeHelp(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;
        player.sendMessage(Text.literal("\n--- ZenithTeams (zt) ---").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("/zt create <id> <Nombre> <Color>").formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("/zt invite <Jugador> | /zt accept <id>").formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("/zt leave | /zt disband").formatted(Formatting.AQUA), false);
        player.sendMessage(Text.literal("/zt chest | /zt chat | /zt home").formatted(Formatting.AQUA), false);
        return 1;
    }

    private static int executeCreate(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player == null) return 0;

            TeamManager manager = ZenithTeamsFabric.getTeamManager();
            ScoreboardEngine ui = ZenithTeamsFabric.getScoreboardEngine();

            if (manager == null || ui == null) {
                player.sendMessage(Text.literal("El sistema aún está arrancando...").formatted(Formatting.RED), false);
                return 0;
            }

            String id = StringArgumentType.getString(context, "id");
            String name = StringArgumentType.getString(context, "name");
            String color = StringArgumentType.getString(context, "color").toUpperCase();

            if (manager.getPlayerTeam(player.getUuid()) != null) {
                player.sendMessage(Text.literal("Ya tienes un equipo.").formatted(Formatting.RED), false);
                return 0;
            }

            if (!VALID_COLORS.contains(color)) {
                player.sendMessage(Text.literal("Color inválido.").formatted(Formatting.RED), false);
                return 0;
            }

            manager.createTeam(id, name, color, player.getUuid()).thenAccept(success -> {
                context.getSource().getServer().execute(() -> {
                    try {
                        if (success) {
                            player.sendMessage(Text.literal("¡Equipo " + name + " creado!").formatted(Formatting.GREEN), false);
                            ui.updatePlayerScoreboard(player);
                        } else {
                            player.sendMessage(Text.literal("Error: ID duplicado.").formatted(Formatting.RED), false);
                        }
                    } catch (Exception e) {
                        player.sendMessage(Text.literal("ERROR SCOREBOARD: " + e.getMessage()).formatted(Formatting.DARK_RED), false);
                        e.printStackTrace();
                    }
                });
            });
            return 1;
        } catch (Exception e) {
            if (context.getSource().getPlayer() != null) {
                context.getSource().getPlayer().sendMessage(Text.literal("ERROR FATAL AL CREAR: " + e.toString()).formatted(Formatting.DARK_RED), false);
            }
            e.printStackTrace();
            return 0;
        }
    }

    private static int executeInvite(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity leader = context.getSource().getPlayer();
            TeamManager manager = ZenithTeamsFabric.getTeamManager();
            if (leader == null || manager == null) return 0;

            ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
            var team = manager.getPlayerTeam(leader.getUuid());

            if (team == null || !team.getLeaderUuid().equals(leader.getUuid())) {
                leader.sendMessage(Text.literal("No eres líder de un equipo.").formatted(Formatting.RED), false);
                return 0;
            }

            team.addInvite(target.getUuid());
            manager.saveTeamData(team);

            leader.sendMessage(Text.literal("Invitación enviada.").formatted(Formatting.YELLOW), false);

            Text inviteMsg = Text.literal("Has sido invitado a " + team.getDisplayName() + ". ")
                    .append(Text.literal("[ACEPTAR]").formatted(Formatting.GREEN)
                            .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/zt accept " + team.getTeamId()))
                                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Clic para unirte")))));
            target.sendMessage(inviteMsg, false);
            return 1;
        } catch (Exception e) {
            if (context.getSource().getPlayer() != null) {
                context.getSource().getPlayer().sendMessage(Text.literal("ERROR AL INVITAR: " + e.toString()).formatted(Formatting.DARK_RED), false);
            }
            return 0;
        }
    }

    private static int executeAccept(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            TeamManager manager = ZenithTeamsFabric.getTeamManager();
            ScoreboardEngine ui = ZenithTeamsFabric.getScoreboardEngine();
            if (player == null || manager == null || ui == null) return 0;

            String id = StringArgumentType.getString(context, "input");

            manager.acceptInvite(player.getUuid(), id).thenAccept(success -> {
                context.getSource().getServer().execute(() -> {
                    if (success) {
                        player.sendMessage(Text.literal("¡Te has unido al equipo!").formatted(Formatting.GREEN), false);
                        ui.updatePlayerScoreboard(player);
                    } else {
                        player.sendMessage(Text.literal("No tienes invitaciones válidas.").formatted(Formatting.RED), false);
                    }
                });
            });
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int executeLeave(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            TeamManager manager = ZenithTeamsFabric.getTeamManager();
            ScoreboardEngine ui = ZenithTeamsFabric.getScoreboardEngine();
            if (player == null || manager == null || ui == null) return 0;

            manager.leaveTeam(player.getUuid()).thenAccept(success -> {
                context.getSource().getServer().execute(() -> {
                    if (success) {
                        player.sendMessage(Text.literal("Has salido del equipo.").formatted(Formatting.YELLOW), false);
                        ui.removePlayerFromScoreboard(player);
                    } else {
                        player.sendMessage(Text.literal("No estás en un equipo.").formatted(Formatting.RED), false);
                    }
                });
            });
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int executeDisband(CommandContext<ServerCommandSource> context) {
        try {
            ServerPlayerEntity player = context.getSource().getPlayer();
            TeamManager manager = ZenithTeamsFabric.getTeamManager();
            ScoreboardEngine ui = ZenithTeamsFabric.getScoreboardEngine();
            if (player == null || manager == null || ui == null) return 0;

            manager.disbandTeam(player.getUuid()).thenAccept(success -> {
                context.getSource().getServer().execute(() -> {
                    if (success) {
                        player.sendMessage(Text.literal("Equipo disuelto.").formatted(Formatting.RED), false);
                        ui.removePlayerFromScoreboard(player);
                    } else {
                        player.sendMessage(Text.literal("No eres el líder de un equipo.").formatted(Formatting.RED), false);
                    }
                });
            });
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int executeChat(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        TeamManager manager = ZenithTeamsFabric.getTeamManager();
        if (player != null && manager != null) {
            boolean enabled = manager.toggleTeamChat(player.getUuid());
            player.sendMessage(Text.literal(enabled ? "Chat de Equipo: ON" : "Chat de Equipo: OFF").formatted(Formatting.AQUA), false);
        }
        return 1;
    }

    private static int executeChest(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        TeamManager manager = ZenithTeamsFabric.getTeamManager();
        if (player != null && manager != null) {
            manager.openTeamVault(player);
        }
        return 1;
    }

    private static int executeHome(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        TeamManager manager = ZenithTeamsFabric.getTeamManager();
        if (player != null && manager != null) {
            manager.teleportToHome(player);
        }
        return 1;
    }

    private static int executeSetHome(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        TeamManager manager = ZenithTeamsFabric.getTeamManager();
        if (player != null && manager != null) {
            manager.setTeamHome(player);
            player.sendMessage(Text.literal("Base del equipo establecida.").formatted(Formatting.GREEN), false);
        }
        return 1;
    }
}