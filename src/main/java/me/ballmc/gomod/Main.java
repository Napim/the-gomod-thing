package me.ballmc.gomod;

import net.weavemc.loader.api.ModInitializer;
import net.weavemc.loader.api.command.CommandBus;
import net.weavemc.loader.api.event.*;

import me.ballmc.gomod.command.AutoQueueCommand;
import me.ballmc.gomod.command.InventoryHUDCommand;
import me.ballmc.gomod.command.KillEffectCommand;
import me.ballmc.gomod.command.KillCounterCommand;
import me.ballmc.gomod.command.StopKillCounterCommand;
import me.ballmc.gomod.command.KillCounterSettingsCommand;
import me.ballmc.gomod.command.PartySpamCommand;
import me.ballmc.gomod.command.PerspectiveCommand;
import me.ballmc.gomod.command.PerspectiveGuiCommand;
import me.ballmc.gomod.command.QueueStatusCommand;
import me.ballmc.gomod.command.FOnlineCommand;
import me.ballmc.gomod.command.PlayerInfoCommand;
import me.ballmc.gomod.command.MapQueuerCommand;
import me.ballmc.gomod.command.TeamSpamCommand;
import me.ballmc.gomod.command.KitLevelsCommand;
import me.ballmc.gomod.commands.GomodCommand;
import me.ballmc.gomod.command.ScoreboardDebugCommand;
import me.ballmc.gomod.command.PlayerQueuerCommand;

import me.ballmc.gomod.features.ConfigManager;
import me.ballmc.gomod.features.AutoQueue;
import me.ballmc.gomod.features.InventoryHUD;
import me.ballmc.gomod.features.AIChatHandler;
import me.ballmc.gomod.features.ApiKeyManager;
import me.ballmc.gomod.features.TeamInviteResponseHandler;
import me.ballmc.gomod.features.PerspectiveDistance;
import me.ballmc.gomod.features.TeamSpamManager;
import me.ballmc.gomod.features.FriendListScanner;
import me.ballmc.gomod.features.KillCounter;
import me.ballmc.gomod.features.HotbarScrollBlocker;
import me.ballmc.gomod.features.PlayerQueuer;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

public class Main implements ModInitializer {
    private final AutoQueue autoQueue = new AutoQueue();
    private final AIChatHandler aiChatHandler = new AIChatHandler();
    private final TeamInviteResponseHandler teamInviteResponseHandler = new TeamInviteResponseHandler();
    private final KillCounter killCounter = new KillCounter();
    private final PlayerQueuer playerQueuer = new PlayerQueuer();

    public static final String CHAT_PREFIX =
            EnumChatFormatting.DARK_GRAY.toString() + EnumChatFormatting.BOLD + "[" +
            EnumChatFormatting.DARK_GREEN + "" + EnumChatFormatting.BOLD + "gomod" +
            EnumChatFormatting.DARK_GRAY + "" + EnumChatFormatting.BOLD + "] " +
            EnumChatFormatting.RESET;

    @Override
    public void preInit() {
        System.out.println("Initializing gomod mod!");

        ConfigManager.loadConfig();
        ApiKeyManager.loadApiKeys();
        AutoQueue.loadSetting();
        InventoryHUD.loadSettings();
        PerspectiveDistance.loadSetting();
        TeamSpamManager.loadPlayers();
        me.ballmc.gomod.features.KitLevelsManager.loadCaches();
        KillCounter.initializeSettings();
        PlayerQueuer.loadConfig();

        System.out.println("Initial settings - AutoQueue: " + (AutoQueue.isEnabled() ? "enabled" : "disabled"));
        System.out.println("Initial settings - InventoryHUD: " + (InventoryHUD.isEnabled() ? "enabled" : "disabled"));
        System.out.println("Initial settings - GoStats AI: " + (ConfigManager.isGoStatsEnabled() ? "enabled" : "disabled"));
        System.out.println("Initial settings - KillCounter: " + (KillCounter.isEnabled() ? "enabled" : "disabled"));

        CommandBus.register(new GomodCommand());
        CommandBus.register(new AutoQueueCommand());
        CommandBus.register(new InventoryHUDCommand());
        CommandBus.register(new KillEffectCommand());
        CommandBus.register(new KillCounterCommand());
        CommandBus.register(new StopKillCounterCommand());
        CommandBus.register(new KillCounterSettingsCommand());
        CommandBus.register(new PartySpamCommand());
        CommandBus.register(new PerspectiveCommand());
        CommandBus.register(new PerspectiveGuiCommand());
        CommandBus.register(new QueueStatusCommand());
        CommandBus.register(new PlayerInfoCommand());
        CommandBus.register(new MapQueuerCommand());
        CommandBus.register(new TeamSpamCommand());
        CommandBus.register(new KitLevelsCommand());
        CommandBus.register(new ScoreboardDebugCommand());
        CommandBus.register(new FOnlineCommand());
        CommandBus.register(new PlayerQueuerCommand());

        EventBus.subscribe(this);
        EventBus.subscribe(autoQueue);
        EventBus.subscribe(aiChatHandler);
        EventBus.subscribe(teamInviteResponseHandler);
        EventBus.subscribe(killCounter);
        EventBus.subscribe(playerQueuer);
        EventBus.subscribe(new KillEffectCommand());

        // Register Weave event handler to lock hotbar while in perspective
        EventBus.subscribe(new HotbarScrollBlocker());
        EventBus.subscribe(new FriendListScanner());

        // Save caches on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                me.ballmc.gomod.features.KitLevelsManager.saveCaches();
            } catch (Exception ignored) {}
        }));
    }

    public static void sendMessage(String message) {
        try {
            if (Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
            }
        } catch (Exception e) {
            System.out.println("[gomod] " + message);
        }
    }
}

 
