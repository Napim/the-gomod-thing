package me.ballmc.AntiShuffle.command;

import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.TickEvent;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.weavemc.loader.api.command.Command;
import net.minecraft.network.play.server.S02PacketChat;
import net.weavemc.loader.api.event.EventBus;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraft.client.network.NetworkPlayerInfo;
import me.ballmc.AntiShuffle.features.TeamSpamManager;
import me.ballmc.AntiShuffle.features.TeamSpamManager.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TeamSpamCommand extends Command {
    private boolean isSpamming = false;
    private List<PlayerInfo> players = new ArrayList<>();
    private int currentPlayerIndex = 0;
    private long lastInviteTime = 0;
    private static final long DELAY_MS = 750; // 
    private PlayerInfo lastInvitedPlayer = null;

    private static class PlayerInfo {
        String name;
        String uuid;
        
        PlayerInfo(String name, String uuid) {
            this.name = name;
            this.uuid = uuid;
        }
    }

    public TeamSpamCommand() {
        super("teamspam");  // Changed to "teamspam" to avoid conflict with TeamSpamCommands
        EventBus.subscribe(this);
    }

    private void loadPlayers() {
        // Load players from TeamSpamManager
        players.clear();
        List<Player> managedPlayers = TeamSpamManager.getPlayers();
        
        if (managedPlayers.isEmpty()) {
            sendMessage(EnumChatFormatting.YELLOW + "No players in the team spam list. Add players with /tspam add <username>");
        } else {
            for (Player player : managedPlayers) {
                players.add(new PlayerInfo(
                    player.getUsername(),
                    player.getUuid()
                ));
            }
            sendMessage(EnumChatFormatting.GRAY + "Loaded " + players.size() + " players from team spam list.");
        }
    }

    // Start team spam with players from TeamSpamManager
    public void startSpamming() {
        loadPlayers();  // Reload players from TeamSpamManager
        
        if (players.isEmpty()) {
            return; // Message already sent in loadPlayers
        }
        
        isSpamming = true;
        currentPlayerIndex = 0;
        lastInviteTime = 0;
        sendMessage(EnumChatFormatting.GREEN + "Started team invite spam!");
    }
    
    // Stop team spam
    public void stopSpamming() {
        if (isSpamming) {
            isSpamming = false;
            sendMessage(EnumChatFormatting.RED + "Stopped team invite spam!");
        }
    }

    @Override
    public void handle(String[] args) {
        // This command is now accessed via /teamspam instead of /tspam
        if (!isSpamming) {
            startSpamming();
        } else {
            stopSpamming();
        }
    }

    @SubscribeEvent
    public void onChat(Receive event) {
        if (!(event.getPacket() instanceof S02PacketChat)) return;
        
        String message = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
        
        // If it's any of the error messages and we're spamming, cancel them
        if (isSpamming && (
            message.contains("Couldn't find a player by that name!") ||
            message.contains("That player is already on a full team!")
        )) {
            event.setCancelled(true);
            if (message.contains("That player is already on a full team!") && lastInvitedPlayer != null) {
                sendMessage(EnumChatFormatting.YELLOW + lastInvitedPlayer.name + 
                          EnumChatFormatting.GRAY + " is in game (team full)");
            }
            lastInvitedPlayer = null;
            return;
        }
        
        // If we don't have a last invited player, return
        if (lastInvitedPlayer == null) return;
        
        if (message.startsWith("Team request sent to ")) {
            String actualPlayer = message.substring("Team request sent to ".length(), message.length() - 1);
            if (!lastInvitedPlayer.name.equals(actualPlayer)) {
                sendMessage(EnumChatFormatting.YELLOW + lastInvitedPlayer.name + 
                          EnumChatFormatting.GRAY + " is nicked as " + 
                          EnumChatFormatting.YELLOW + actualPlayer);
            }
        }
        
        lastInvitedPlayer = null;
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!isSpamming || Minecraft.getMinecraft().thePlayer == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastInviteTime >= DELAY_MS) {
            if (currentPlayerIndex >= players.size()) {
                sendMessage(EnumChatFormatting.GREEN + "Finished inviting all players!");
                currentPlayerIndex = 0;
                isSpamming = false;
                return;
            }
            
            PlayerInfo player = players.get(currentPlayerIndex);
            
            // Check if player is in our team via scoreboard
            boolean skipPlayer = false;
            if (Minecraft.getMinecraft().theWorld != null) {
                net.minecraft.scoreboard.Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
                if (scoreboard != null) {
                    for (net.minecraft.scoreboard.ScoreObjective objective : scoreboard.getScoreObjectives()) {
                        java.util.Collection<net.minecraft.scoreboard.Score> scores = scoreboard.getSortedScores(objective);
                        for (net.minecraft.scoreboard.Score score : scores) {
                            net.minecraft.scoreboard.ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                            String line = team != null ? team.getColorPrefix() + score.getPlayerName() + team.getColorSuffix() : score.getPlayerName();
                            
                            if (line != null && line.contains("Team") && line.contains(player.name)) {
                                skipPlayer = true;
                                sendMessage(EnumChatFormatting.GRAY + "Skipping " + player.name + " (already in team)");
                                break;
                            }
                        }
                        if (skipPlayer) break;
                    }
                }
            }
            
            // Check if player is in the tab list
            boolean isInTabList = false;
            if (Minecraft.getMinecraft().getNetHandler() != null) {
                for (NetworkPlayerInfo tabPlayer : Minecraft.getMinecraft().getNetHandler().getPlayerInfoMap()) {
                    if (tabPlayer.getGameProfile().getName().equals(player.name)) {
                        isInTabList = true;
                        sendMessage(EnumChatFormatting.GRAY + "Skipping " + player.name + " (in game)");
                        break;
                    }
                }
            }
            
            if (!isInTabList && !skipPlayer) {
                lastInvitedPlayer = player;
                String command = "/team invite " + player.name;
                sendMessage(EnumChatFormatting.GRAY + "Executing: " + command);
                Minecraft.getMinecraft().thePlayer.sendChatMessage(command);
                // Play sound effects directly from the client
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation("note.pling"), 1.0F, 0.4F, 0.0F, 0.0F, 0.0F));
                Minecraft.getMinecraft().getSoundHandler().playSound(new PositionedSoundRecord(new ResourceLocation("note.pling"), 1.0F, 0.6F, 0.0F, 0.0F, 0.0F));
            }
            
            lastInviteTime = currentTime;
            currentPlayerIndex++;
        }
    }

    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 