package com.jdar.zenithteamsfabric.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ZenithTeamTest {

    private ZenithTeam team;
    private UUID leaderId;

    // Se ejecuta ANTES de cada test para darnos un entorno limpio
    @BeforeEach
    void setUp() {
        leaderId = UUID.randomUUID(); // <-- Corrección aquí
        team = new ZenithTeam("t1", "Titanes", "RED", leaderId);
    }

    @Test
    void testTeamCreationAndLeader() {
        assertEquals("t1", team.getTeamId(), "El ID del equipo debe coincidir.");
        assertEquals("RED", team.getHexColor(), "El color debe ser RED.");
        assertTrue(team.getMembers().contains(leaderId), "El líder debe ser añadido como miembro automáticamente.");
    }

    @Test
    void testAddAndRemoveMember() {
        UUID newMember = UUID.randomUUID();

        team.addMember(newMember);
        assertTrue(team.getMembers().contains(newMember), "El jugador debería estar en la lista de miembros.");

        team.removeMember(newMember);
        assertFalse(team.getMembers().contains(newMember), "El jugador debería haber sido eliminado.");
    }

    @Test
    void testInviteSystem() {
        UUID invitedPlayer = UUID.randomUUID();

        assertFalse(team.hasInvite(invitedPlayer), "No debería tener invitación inicial.");

        team.addInvite(invitedPlayer);
        assertTrue(team.hasInvite(invitedPlayer), "El sistema debe registrar la invitación.");

        team.removeInvite(invitedPlayer);
        assertFalse(team.hasInvite(invitedPlayer), "La invitación debe borrarse tras rechazar/aceptar.");
    }
}
