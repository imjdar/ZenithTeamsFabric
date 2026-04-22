package com.jdar.zenithteamsfabric.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ZenithTeam {

    private String teamId;
    private String displayName;
    private String hexColor;
    private UUID leaderUuid;
    private Set<UUID> members;
    private Set<UUID> pendingRequests;
    private Set<UUID> invitedPlayers;

    private String vaultBase64;
    private String homeWorld;
    private double homeX, homeY, homeZ;
    private float homeYaw, homePitch;

    public ZenithTeam() {
        this.members = ConcurrentHashMap.newKeySet();
        this.pendingRequests = ConcurrentHashMap.newKeySet();
        this.invitedPlayers = ConcurrentHashMap.newKeySet();
    }

    @JsonCreator
    public ZenithTeam(
            @JsonProperty("teamId") String teamId,
            @JsonProperty("displayName") String displayName,
            @JsonProperty("hexColor") String hexColor,
            @JsonProperty("leaderUuid") UUID leaderUuid) {
        this.teamId = teamId;
        this.displayName = displayName;
        this.hexColor = hexColor;
        this.leaderUuid = leaderUuid;
        this.members = ConcurrentHashMap.newKeySet();
        this.pendingRequests = ConcurrentHashMap.newKeySet();
        this.invitedPlayers = ConcurrentHashMap.newKeySet();
        this.members.add(leaderUuid);
    }

    public String getTeamId() { return teamId; }
    public void setTeamId(String teamId) { this.teamId = teamId; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getHexColor() { return hexColor; }
    public void setHexColor(String hexColor) { this.hexColor = hexColor; }

    public UUID getLeaderUuid() { return leaderUuid; }
    public void setLeaderUuid(UUID leaderUuid) { this.leaderUuid = leaderUuid; }

    public Set<UUID> getMembers() { return members; }
    public void setMembers(Set<UUID> members) { this.members = members; }

    public Set<UUID> getPendingRequests() { return pendingRequests; }
    public void setPendingRequests(Set<UUID> pendingRequests) { this.pendingRequests = pendingRequests; }

    public Set<UUID> getInvitedPlayers() { return invitedPlayers; }
    public void setInvitedPlayers(Set<UUID> invitedPlayers) { this.invitedPlayers = invitedPlayers; }

    public String getVaultBase64() { return vaultBase64; }
    public void setVaultBase64(String vaultBase64) { this.vaultBase64 = vaultBase64; }

    public String getHomeWorld() { return homeWorld; }
    public void setHomeWorld(String homeWorld) { this.homeWorld = homeWorld; }

    public double getHomeX() { return homeX; }
    public void setHomeX(double homeX) { this.homeX = homeX; }

    public double getHomeY() { return homeY; }
    public void setHomeY(double homeY) { this.homeY = homeY; }

    public double getHomeZ() { return homeZ; }
    public void setHomeZ(double homeZ) { this.homeZ = homeZ; }

    public float getHomeYaw() { return homeYaw; }
    public void setHomeYaw(float homeYaw) { this.homeYaw = homeYaw; }

    public float getHomePitch() { return homePitch; }
    public void setHomePitch(float homePitch) { this.homePitch = homePitch; }

    public void addMember(UUID uuid) { this.members.add(uuid); }
    public void removeMember(UUID uuid) { this.members.remove(uuid); }

    public void addRequest(UUID uuid) { this.pendingRequests.add(uuid); }
    public void removeRequest(UUID uuid) { this.pendingRequests.remove(uuid); }
    public boolean hasRequest(UUID uuid) { return this.pendingRequests.contains(uuid); }

    public void addInvite(UUID uuid) { this.invitedPlayers.add(uuid); }
    public void removeInvite(UUID uuid) { this.invitedPlayers.remove(uuid); }
    public boolean hasInvite(UUID uuid) { return this.invitedPlayers.contains(uuid); }
}