package com.jdar.zenithteamsfabric.ui;

import com.jdar.zenithteamsfabric.manager.TeamManager;
import com.jdar.zenithteamsfabric.model.ZenithTeam;
import com.jdar.zenithteamsfabric.utils.ItemSerializer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.collection.DefaultedList;

public class TeamVaultInventory extends SimpleInventory {

    private final ZenithTeam team;
    private final TeamManager teamManager;
    private final MinecraftServer server;

    public TeamVaultInventory(ZenithTeam team, TeamManager teamManager, MinecraftServer server, int size) {
        super(size);
        this.team = team;
        this.teamManager = teamManager;
        this.server = server;
    }

    @Override
    public void onClose(PlayerEntity player) {
        super.onClose(player);

        // Convertimos el contenido a DefaultedList
        DefaultedList<ItemStack> items = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        for (int i = 0; i < this.size(); i++) {
            items.set(i, this.getStack(i));
        }

        // Guardamos usando el wrapper de registros nativo del servidor
        String base64 = ItemSerializer.toBase64(items, server.getRegistryManager());
        team.setVaultBase64(base64);
        teamManager.saveTeamData(team);
    }
}
