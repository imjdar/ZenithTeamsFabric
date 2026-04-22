package com.jdar.zenithteamsfabric.ui;

import com.jdar.zenithteamsfabric.manager.TeamManager;
import com.jdar.zenithteamsfabric.model.ZenithTeam;
import net.minecraft.scoreboard.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class ScoreboardEngine {

    private final MinecraftServer server;
    private final TeamManager teamManager;

    public ScoreboardEngine(MinecraftServer server, TeamManager teamManager) {
        this.server = server;
        this.teamManager = teamManager;
    }

    public void updatePlayerScoreboard(ServerPlayerEntity player) {
        ZenithTeam team = teamManager.getPlayerTeam(player.getUuid());

        if (team == null) {
            removePlayerFromScoreboard(player);
            return;
        }

        ServerScoreboard board = server.getScoreboard();
        ScoreboardObjective objective = board.getNullableObjective("zenith_board");

        if (objective == null) {
            objective = board.addObjective("zenith_board", ScoreboardCriterion.DUMMY,
                    Text.literal("ZENITH TEAMS").formatted(Formatting.GOLD),
                    ScoreboardCriterion.RenderType.INTEGER, true, null);
            board.setObjectiveSlot(ScoreboardDisplaySlot.SIDEBAR, objective);
        }

        // 1. Sincronizamos TabList y Glowing Nativos
        syncPlayerToTeam(player, team);

        // 2. Sidebar sin Flicker usando la API 1.21+
        setLine(board, objective, "line4", Text.literal("Equipo: ").formatted(Formatting.WHITE)
                .append(Text.literal(team.getDisplayName()).formatted(Formatting.GREEN)), 4);

        setLine(board, objective, "line3", Text.literal("Miembros: ").formatted(Formatting.WHITE)
                .append(Text.literal(String.valueOf(team.getMembers().size())).formatted(Formatting.AQUA)), 3);

        setLine(board, objective, "line2", Text.literal(" "), 2);

        ScoreHolder trackingHolder = ScoreHolder.fromName("line1");
        if (board.getOrCreateScore(trackingHolder, objective).getScore() == 0) {
            setLine(board, objective, "line1", Text.literal("Rastreo: ").formatted(Formatting.WHITE)
                    .append(Text.literal("Buscando...").formatted(Formatting.GRAY)), 1);
        }
    }

    private void setLine(ServerScoreboard board, ScoreboardObjective obj, String holderName, Text display, int scoreVal) {
        ScoreHolder holder = ScoreHolder.fromName(holderName);
        ScoreAccess score = board.getOrCreateScore(holder, obj);
        score.setScore(scoreVal);
        score.setDisplayText(display);
    }

    public void syncPlayerToTeam(ServerPlayerEntity player, ZenithTeam zenithTeam) {
        ServerScoreboard board = server.getScoreboard();
        Team mcTeam = board.getTeam(zenithTeam.getTeamId());

        Formatting teamColor = Formatting.byName(zenithTeam.getHexColor().toUpperCase());
        if (teamColor == null) teamColor = Formatting.WHITE;

        if (mcTeam == null) mcTeam = board.addTeam(zenithTeam.getTeamId());

        mcTeam.setColor(teamColor);
        mcTeam.setPrefix(Text.literal("[" + zenithTeam.getDisplayName() + "] ").formatted(teamColor));

        for (String entry : mcTeam.getPlayerList()) {
            ServerPlayerEntity oldMember = server.getPlayerManager().getPlayer(entry);
            if (oldMember != null && !zenithTeam.getMembers().contains(oldMember.getUuid())) {
                board.removeScoreHolderFromTeam(entry, mcTeam);
            }
        }

        for (UUID memberId : zenithTeam.getMembers()) {
            ServerPlayerEntity member = server.getPlayerManager().getPlayer(memberId);
            if (member != null) board.addScoreHolderToTeam(member.getNameForScoreboard(), mcTeam);
        }

        player.setGlowing(true);
    }

    public void removePlayerFromScoreboard(ServerPlayerEntity player) {
        ServerScoreboard board = server.getScoreboard();
        Team mcTeam = board.getScoreHolderTeam(player.getNameForScoreboard());
        if (mcTeam != null) board.removeScoreHolderFromTeam(player.getNameForScoreboard(), mcTeam);
        player.setGlowing(false);
    }

    public void refreshTeamScoreboards(ZenithTeam team) {
        if (team == null) return;
        for (UUID uuid : team.getMembers()) {
            ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
            if (p != null) updatePlayerScoreboard(p);
        }
    }
}
