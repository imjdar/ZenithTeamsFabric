package com.jdar.zenithteamsfabric.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jdar.zenithteamsfabric.ZenithTeamsFabric;
import com.jdar.zenithteamsfabric.model.ZenithTeam;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class FabricDataStorage {

    private final ObjectMapper objectMapper;
    private final File teamsFolder;
    private final File playersFolder;

    public FabricDataStorage(MinecraftServer server) {
        this.objectMapper = new ObjectMapper();

        Path worldPath = server.getSavePath(WorldSavePath.ROOT);
        File pluginFolder = new File(worldPath.toFile(), "zenithteams");

        this.teamsFolder = new File(pluginFolder, "teams");
        this.playersFolder = new File(pluginFolder, "players");

        if (!teamsFolder.exists()) teamsFolder.mkdirs();
        if (!playersFolder.exists()) playersFolder.mkdirs();
    }

    public CompletableFuture<Void> saveTeam(ZenithTeam team) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(teamsFolder, team.getTeamId() + ".json");
            try {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, team);
            } catch (IOException e) {
                ZenithTeamsFabric.LOGGER.error("Error al guardar el equipo: " + team.getTeamId(), e);
            }
        });
    }

    public CompletableFuture<ZenithTeam> getTeam(String teamId) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(teamsFolder, teamId.toLowerCase() + ".json");
            if (!file.exists()) return null;
            try {
                return objectMapper.readValue(file, ZenithTeam.class);
            } catch (IOException e) {
                ZenithTeamsFabric.LOGGER.error("Error al cargar el equipo: " + teamId, e);
                return null;
            }
        });
    }

    public CompletableFuture<Void> deleteTeam(String teamId) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(teamsFolder, teamId.toLowerCase() + ".json");
            if (file.exists()) file.delete();
        });
    }

    public CompletableFuture<Void> setPlayerTeam(UUID playerUuid, String teamId) {
        return CompletableFuture.runAsync(() -> {
            File file = new File(playersFolder, playerUuid.toString() + ".json");
            try {
                if (teamId == null) {
                    if (file.exists()) file.delete();
                } else {
                    objectMapper.writeValue(file, teamId);
                }
            } catch (IOException e) {
                ZenithTeamsFabric.LOGGER.error("Error guardando datos de jugador: " + playerUuid, e);
            }
        });
    }

    public CompletableFuture<String> getPlayerTeamId(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = new File(playersFolder, playerUuid.toString() + ".json");
            if (!file.exists()) return null;
            try {
                return objectMapper.readValue(file, String.class);
            } catch (IOException e) {
                ZenithTeamsFabric.LOGGER.error("Error cargando datos de jugador: " + playerUuid, e);
                return null;
            }
        });
    }
}