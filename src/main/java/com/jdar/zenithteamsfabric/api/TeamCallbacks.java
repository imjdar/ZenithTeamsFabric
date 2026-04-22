package com.jdar.zenithteamsfabric.api;

import com.jdar.zenithteamsfabric.manager.TeamManager;
import com.jdar.zenithteamsfabric.model.ZenithTeam;
import com.jdar.zenithteamsfabric.ui.ScoreboardEngine;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class TeamCallbacks {

    public static void register(MinecraftServer server, TeamManager teamManager, ScoreboardEngine scoreboardEngine) {

        ServerPlayConnectionEvents.JOIN.register((handler, sender, srv) -> {
            ServerPlayerEntity player = handler.getPlayer();
            teamManager.loadPlayerTeamOnJoin(player.getUuid()).thenRun(() -> {
                srv.execute(() -> {
                    ZenithTeam team = teamManager.getPlayerTeam(player.getUuid());
                    if (team != null) scoreboardEngine.refreshTeamScoreboards(team);
                });
            });
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, srv) -> {
            ServerPlayerEntity player = handler.getPlayer();
            ZenithTeam team = teamManager.getPlayerTeam(player.getUuid());
            teamManager.handlePlayerQuit(player.getUuid());
            if (team != null) scoreboardEngine.refreshTeamScoreboards(team);
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!world.isClient() && player instanceof ServerPlayerEntity attacker && entity instanceof ServerPlayerEntity victim) {
                ZenithTeam attackerTeam = teamManager.getPlayerTeam(attacker.getUuid());
                ZenithTeam victimTeam = teamManager.getPlayerTeam(victim.getUuid());

                if (attackerTeam != null && victimTeam != null && attackerTeam.getTeamId().equals(victimTeam.getTeamId())) {
                    attacker.sendMessage(Text.literal("¡Fuego amigo desactivado!").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
            }
            return ActionResult.PASS;
        });

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            ZenithTeam team = teamManager.getPlayerTeam(sender.getUuid());
            if (team != null && teamManager.isTeamChatEnabled(sender.getUuid())) {
                Formatting teamColor = Formatting.byName(team.getHexColor().toUpperCase());
                if (teamColor == null) teamColor = Formatting.AQUA;

                Text privateMsg = Text.literal("[Chat Equipo] ").formatted(teamColor)
                        .append(Text.literal(sender.getName().getString() + ": ").formatted(Formatting.WHITE))
                        .append(message.getContent().copy().formatted(Formatting.YELLOW));

                for (UUID memberId : team.getMembers()) {
                    ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
                    if (member != null) member.sendMessage(privateMsg, false);
                }
                return false;
            }
            return true;
        });
    }
}