package com.jdar.zenithteamsfabric.api;

import com.jdar.zenithteamsfabric.manager.TeamManager;
import com.jdar.zenithteamsfabric.model.ZenithTeam;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class TrackingEngine {

    private final MinecraftServer server;
    private final TeamManager teamManager;

    public TrackingEngine(MinecraftServer server, TeamManager teamManager) {
        this.server = server;
        this.teamManager = teamManager;
    }

    public void register() {
        // Se ejecuta 20 veces por segundo
        ServerTickEvents.START_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer srv) {
        for (ServerPlayerEntity player : srv.getPlayerManager().getPlayerList()) {
            ZenithTeam team = teamManager.getPlayerTeam(player.getUuid());
            if (team == null || team.getMembers().size() <= 1) continue;

            ServerPlayerEntity nearest = null;
            double nearestDist = Double.MAX_VALUE;

            for (UUID memberId : team.getMembers()) {
                if (memberId.equals(player.getUuid())) continue;
                ServerPlayerEntity member = srv.getPlayerManager().getPlayer(memberId);

                if (member != null && member.getServerWorld() == player.getServerWorld()) {
                    double dist = player.squaredDistanceTo(member);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearest = member;
                    }
                }
            }

            if (nearest != null) {
                updateCompass(player, nearest, Math.sqrt(nearestDist));
            }
        }
    }

    private void updateCompass(ServerPlayerEntity player, ServerPlayerEntity target, double distance) {
        double dx = target.getX() - player.getX();
        double dz = target.getZ() - player.getZ();
        double angle = Math.toDegrees(Math.atan2(dz, dx)) - 90;
        double playerYaw = player.getYaw() % 360;
        if (playerYaw < 0) playerYaw += 360;

        double relativeAngle = angle - playerYaw;
        if (relativeAngle < 0) relativeAngle += 360;

        String arrow = getArrow(relativeAngle);
        String distStr = String.format("%.1f", distance) + "m";

        Text trackingText = Text.literal("Buscando: ").formatted(Formatting.WHITE)
                .append(Text.literal(target.getName().getString()).formatted(Formatting.AQUA))
                .append(Text.literal(" " + arrow + " " + distStr).formatted(Formatting.GOLD));

        // Actualizar Action Bar
        player.sendMessage(trackingText, true);

        // Actualizar línea 1 del Scoreboard
        ServerScoreboard board = server.getScoreboard();
        ScoreboardObjective obj = board.getNullableObjective("zenith_board");
        if (obj != null) {
            ScoreAccess score = board.getOrCreateScore(ScoreHolder.fromName("line1"), obj);
            score.setDisplayText(trackingText);
        }
    }

    private String getArrow(double angle) {
        if (angle >= 337.5 || angle < 22.5) return "↑";
        if (angle >= 22.5 && angle < 67.5) return "↗";
        if (angle >= 67.5 && angle < 112.5) return "→";
        if (angle >= 112.5 && angle < 157.5) return "↘";
        if (angle >= 157.5 && angle < 202.5) return "↓";
        if (angle >= 202.5 && angle < 247.5) return "↙";
        if (angle >= 247.5 && angle < 292.5) return "←";
        if (angle >= 292.5 && angle < 337.5) return "↖";
        return "•";
    }
}
