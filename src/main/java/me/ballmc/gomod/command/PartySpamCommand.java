package me.ballmc.gomod.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Command to repeatedly send party invites to a player and then disband.
 * This is useful for party spam scenarios.
 */
public class PartySpamCommand extends Command {
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final AtomicBoolean running = new AtomicBoolean(false);
    private static final int DELAY = 210; // ms between party commands
    private static ScheduledFuture<?> currentTask = null;
    private static String targetPlayer = "";
    
    // Map to track active party spam for different players
    private static final Map<String, Boolean> activeSpamMap = new HashMap<>();
    
    public PartySpamCommand() {
        super("pspam");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length < 1) {
            sendMessage(EnumChatFormatting.RED + "Usage: /pspam <player>");
            return;
        }
        
        String playerName = args[0];
        boolean isActive = activeSpamMap.getOrDefault(playerName.toLowerCase(), false);
        
        if (isActive) {
            // Stop spam for this player
            stopPartySpam();
            sendMessage(EnumChatFormatting.RED + "Stopped party spam for " + EnumChatFormatting.GOLD + playerName);
            activeSpamMap.put(playerName.toLowerCase(), false);
        } else {
            // Start spam for this player
            startPartySpam(playerName);
            sendMessage(EnumChatFormatting.GREEN + "Started party spam for " + EnumChatFormatting.GOLD + playerName);
            activeSpamMap.put(playerName.toLowerCase(), true);
        }
    }
    
    /**
     * Stops the party spam
     */
    private void stopPartySpam() {
        running.set(false);
        
        // Cancel the current task if it exists
        if (currentTask != null && !currentTask.isDone() && !currentTask.isCancelled()) {
            currentTask.cancel(false);
            currentTask = null;
        }
        
        // Reset the scheduler to ensure a clean state
        scheduler.shutdown();
        scheduler = Executors.newScheduledThreadPool(1);
        
        targetPlayer = "";
    }
    
    /**
     * Starts party spam for a specific player
     */
    private void startPartySpam(String playerName) {
        // First make sure any previous spam is properly stopped
        if (running.get()) {
            stopPartySpam();
        }
        
        targetPlayer = playerName;
        running.set(true);
        
        final AtomicBoolean invitePhase = new AtomicBoolean(true);
        
        // Schedule the spam task
        currentTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (!running.get()) {
                    return;
                }
                
                if (invitePhase.get()) {
                    // Send party invite
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/p invite " + targetPlayer);
                } else {
                    // Disband party
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/p disband");
                }
                
                // Toggle between invite and disband phases
                invitePhase.set(!invitePhase.get());
                
            } catch (Exception e) {
                System.err.println("Error in party spam task: " + e.getMessage());
                e.printStackTrace();
            }
        }, 0, DELAY, TimeUnit.MILLISECONDS);
    }
    
    /**
     * Sends a message to the player
     */
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
}
