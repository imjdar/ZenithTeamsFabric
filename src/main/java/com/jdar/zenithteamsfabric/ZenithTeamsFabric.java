package com.jdar.zenithteamsfabric;

import com.jdar.zenithteamsfabric.api.TeamCallbacks;
import com.jdar.zenithteamsfabric.api.TrackingEngine;
import com.jdar.zenithteamsfabric.commands.TeamCommand;
import com.jdar.zenithteamsfabric.data.FabricDataStorage;
import com.jdar.zenithteamsfabric.manager.TeamManager;
import com.jdar.zenithteamsfabric.ui.ScoreboardEngine;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZenithTeamsFabric implements ModInitializer {

    public static final String MOD_ID = "zenithteamsfabric";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Instancias estáticas vitales para el Acceso Dinámico (Rompe-Escudos)
    private static FabricDataStorage dataStorage;
    private static TeamManager teamManager;
    private static ScoreboardEngine scoreboardEngine;

    @Override
    public void onInitialize() {
        LOGGER.info("Iniciando ZenithTeamsFabric - Arquitectura Dinámica...");

        // 1. INICIALIZACIÓN DEL CICLO DE VIDA
        // Esto solo ocurre cuando el mundo ya está accesible (Garantizando que nada sea null)
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            LOGGER.info("Mundo detectado. Levantando las bases de datos y la UI...");

            dataStorage = new FabricDataStorage(server);
            teamManager = new TeamManager(server, dataStorage);
            scoreboardEngine = new ScoreboardEngine(server, teamManager);

            TeamCallbacks.register(server, teamManager, scoreboardEngine);
            new TrackingEngine(server, teamManager).register();

            LOGGER.info("ZenithTeamsFabric conectado al servidor con éxito.");
        });

        // 2. REGISTRO DE COMANDOS
        // Fíjate que ya NO le pasamos las variables al comando. El comando las buscará
        // usando los métodos de abajo en el instante en que el jugador presione Enter.
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TeamCommand.register(dispatcher);
        });

        LOGGER.info("ZenithTeamsFabric montado en memoria.");
    }

    // --- ACCESORES GLOBALES ESTÁTICOS ---
    // Estos son los métodos que TeamCommand llama para obtener la lógica en tiempo real
    public static FabricDataStorage getDataStorage() {
        return dataStorage;
    }

    public static TeamManager getTeamManager() {
        return teamManager;
    }

    public static ScoreboardEngine getScoreboardEngine() {
        return scoreboardEngine;
    }
}