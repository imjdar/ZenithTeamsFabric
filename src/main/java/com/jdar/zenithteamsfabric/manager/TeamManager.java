package com.jdar.zenithteamsfabric.manager;

import com.jdar.zenithteamsfabric.data.FabricDataStorage;
import com.jdar.zenithteamsfabric.model.ZenithTeam;
import com.jdar.zenithteamsfabric.ui.TeamVaultInventory;
import com.jdar.zenithteamsfabric.utils.ItemSerializer;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {

    private final MinecraftServer server;
    private final FabricDataStorage storage;
    private final ConcurrentHashMap<String, ZenithTeam> activeTeams;
    private final ConcurrentHashMap<UUID, String> playerTeamMap;
    private final Set<UUID> teamChatEnabled;

    public TeamManager(MinecraftServer server, FabricDataStorage storage) {
        this.server = server;
        this.storage = storage;
        this.activeTeams = new ConcurrentHashMap<>();
        this.playerTeamMap = new ConcurrentHashMap<>();
        this.teamChatEnabled = ConcurrentHashMap.newKeySet();
    }

    public FabricDataStorage getStorage() { return storage; }

    // --- GESTIÓN DE EQUIPOS (LÓGICA CORE) ---

    public CompletableFuture<Boolean> createTeam(String teamId, String displayName, String hexColor, UUID leaderUuid) {
        String id = teamId.toLowerCase();
        if (activeTeams.containsKey(id)) return CompletableFuture.completedFuture(false);

        ZenithTeam newTeam = new ZenithTeam(id, displayName, hexColor, leaderUuid);
        activeTeams.put(id, newTeam);
        playerTeamMap.put(leaderUuid, id);

        return storage.saveTeam(newTeam)
                .thenCompose(v -> storage.setPlayerTeam(leaderUuid, id))
                .thenApply(v -> true);
    }

    public CompletableFuture<Void> loadPlayerTeamOnJoin(UUID playerUuid) {
        return storage.getPlayerTeamId(playerUuid).thenCompose(teamId -> {
            if (teamId == null) return CompletableFuture.completedFuture(null);
            playerTeamMap.put(playerUuid, teamId);
            if (!activeTeams.containsKey(teamId)) {
                return storage.getTeam(teamId).thenAccept(team -> {
                    if (team != null) activeTeams.put(teamId, team);
                });
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    public ZenithTeam getPlayerTeam(UUID playerUuid) {
        String teamId = playerTeamMap.get(playerUuid);
        return (teamId != null) ? activeTeams.get(teamId) : null;
    }

    public ZenithTeam getTeamById(String teamId) {
        return activeTeams.get(teamId.toLowerCase());
    }

    public void handlePlayerQuit(UUID playerUuid) {
        playerTeamMap.remove(playerUuid);
        teamChatEnabled.remove(playerUuid);
    }

    // --- ACCIONES DE MIEMBROS ---

    public CompletableFuture<Boolean> leaveTeam(UUID playerUuid) {
        ZenithTeam team = getPlayerTeam(playerUuid);
        if (team == null) return CompletableFuture.completedFuture(false);

        team.removeMember(playerUuid);
        playerTeamMap.remove(playerUuid);
        teamChatEnabled.remove(playerUuid);

        // Si el líder se va, delegamos o disolvemos
        if (team.getLeaderUuid().equals(playerUuid)) {
            if (!team.getMembers().isEmpty()) {
                UUID nextLeader = team.getMembers().iterator().next();
                team.setLeaderUuid(nextLeader);
            } else {
                return disbandTeam(playerUuid);
            }
        }

        return storage.setPlayerTeam(playerUuid, null)
                .thenCompose(v -> storage.saveTeam(team))
                .thenApply(v -> true);
    }

    public CompletableFuture<Boolean> kickMember(UUID leaderUuid, UUID targetUuid) {
        ZenithTeam team = getPlayerTeam(leaderUuid);
        if (team == null || !team.getLeaderUuid().equals(leaderUuid)) return CompletableFuture.completedFuture(false);
        if (!team.getMembers().contains(targetUuid) || leaderUuid.equals(targetUuid)) return CompletableFuture.completedFuture(false);

        team.removeMember(targetUuid);
        playerTeamMap.remove(targetUuid);

        return storage.setPlayerTeam(targetUuid, null)
                .thenCompose(v -> storage.saveTeam(team))
                .thenApply(v -> true);
    }

    public CompletableFuture<Boolean> disbandTeam(UUID leaderUuid) {
        ZenithTeam team = getPlayerTeam(leaderUuid);
        if (team == null || !team.getLeaderUuid().equals(leaderUuid)) return CompletableFuture.completedFuture(false);

        String teamId = team.getTeamId();
        activeTeams.remove(teamId);

        return storage.deleteTeam(teamId).thenApply(v -> {
            team.getMembers().forEach(playerTeamMap::remove);
            return true;
        });
    }

    // --- SISTEMA DE INVITACIONES ---

    public void addJoinRequest(String teamId, UUID playerUuid) {
        ZenithTeam team = activeTeams.get(teamId.toLowerCase());
        if (team != null) {
            team.addRequest(playerUuid);
            saveTeamData(team);
        }
    }

    public CompletableFuture<Boolean> acceptRequest(UUID leaderUuid, UUID targetUuid) {
        ZenithTeam team = getPlayerTeam(leaderUuid);
        if (team == null || !team.getLeaderUuid().equals(leaderUuid)) return CompletableFuture.completedFuture(false);

        team.removeRequest(targetUuid);
        team.addMember(targetUuid);
        playerTeamMap.put(targetUuid, team.getTeamId());

        return storage.saveTeam(team)
                .thenCompose(v -> storage.setPlayerTeam(targetUuid, team.getTeamId()))
                .thenApply(v -> true);
    }

    public CompletableFuture<Boolean> acceptInvite(UUID playerUuid, String teamId) {
        ZenithTeam team = getTeamById(teamId);
        if (team == null || !team.hasInvite(playerUuid)) return CompletableFuture.completedFuture(false);

        team.removeInvite(playerUuid);
        team.addMember(playerUuid);
        playerTeamMap.put(playerUuid, team.getTeamId());

        return storage.saveTeam(team)
                .thenCompose(v -> storage.setPlayerTeam(playerUuid, team.getTeamId()))
                .thenApply(v -> true);
    }

    // --- UTILIDADES (BASE, CHAT, VAULT) ---

    public void saveTeamData(ZenithTeam team) {
        storage.saveTeam(team);
    }

    public boolean toggleTeamChat(UUID playerUuid) {
        if (teamChatEnabled.contains(playerUuid)) {
            teamChatEnabled.remove(playerUuid);
            return false;
        }
        teamChatEnabled.add(playerUuid);
        return true;
    }

    public boolean isTeamChatEnabled(UUID playerUuid) {
        return teamChatEnabled.contains(playerUuid);
    }

    public void setTeamHome(ServerPlayerEntity leader) {
        ZenithTeam team = getPlayerTeam(leader.getUuid());
        if (team == null || !team.getLeaderUuid().equals(leader.getUuid())) return;

        team.setHomeWorld(leader.getServerWorld().getRegistryKey().getValue().toString());
        team.setHomeX(leader.getX()); team.setHomeY(leader.getY()); team.setHomeZ(leader.getZ());
        team.setHomeYaw(leader.getYaw()); team.setHomePitch(leader.getPitch());
        saveTeamData(team);
    }

    public void teleportToHome(ServerPlayerEntity player) {
        ZenithTeam team = getPlayerTeam(player.getUuid());
        if (team == null || team.getHomeWorld() == null) return;

        ServerWorld world = server.getWorld(RegistryKey.of(RegistryKeys.WORLD, Identifier.of(team.getHomeWorld())));
        if (world != null) {
            player.teleport(world, team.getHomeX(), team.getHomeY(), team.getHomeZ(), team.getHomeYaw(), team.getHomePitch());
        }
    }

    public void openTeamVault(ServerPlayerEntity player) {
        ZenithTeam team = getPlayerTeam(player.getUuid());
        if (team == null) return;

        DefaultedList<ItemStack> items = ItemSerializer.fromBase64(team.getVaultBase64(), server.getRegistryManager(), 27);
        TeamVaultInventory vault = new TeamVaultInventory(team, this, server, 27);

        for (int i = 0; i < items.size(); i++) {
            vault.setStack(i, items.get(i));
        }

        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, p) ->
                        new GenericContainerScreenHandler(ScreenHandlerType.GENERIC_9X3, syncId, playerInventory, vault, 3),
                Text.literal("Cofre del Equipo")
        ));
    }
}
