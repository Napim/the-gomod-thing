package me.ballmc.gomod.command;

import me.ballmc.gomod.features.KillCounter;
import me.ballmc.gomod.Main;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

import java.util.*;

/**
 * Command handler for /startkc command.
 */
public class KillCounterCommand extends Command {
    
    public KillCounterCommand() {
        super("startkc");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "reset":
                    handleReset();
                    break;
                case "status":
                    handleStatus();
                    break;
                case "test":
                    handleTest();
                    break;
                case "scankits":
                    handleScanKits();
                    break;
                default:
                    handleStart();
                    break;
            }
        } else {
            handleStart();
        }
    }
    
    /**
     * Handles the /startkc command.
     */
    private void handleStart() {
        if (KillCounter.isEnabled()) {
            Main.sendMessage(EnumChatFormatting.YELLOW + "Kill counter is already running!");
            return;
        }
        
        KillCounter.start();
        Main.sendMessage(EnumChatFormatting.GREEN + "Kill counter started! Tracking kills in the top-left corner.");
    }
    
    /**
     * Handles the /stopkc command.
     */
    private void handleStop() {
        if (!KillCounter.isEnabled()) {
        Main.sendMessage(EnumChatFormatting.YELLOW + "Kill counter is not running!");
        return;
    }
    
    KillCounter.stop();
    Main.sendMessage(EnumChatFormatting.RED + "Kill counter stopped!");
    }
    
    /**
     * Handles the reset command.
     */
    private void handleReset() {
    KillCounter.reset();
    Main.sendMessage(EnumChatFormatting.YELLOW + "Kill counter reset! All kill counts cleared.");
    }
    
    /**
     * Handles the status command.
     */
    private void handleStatus() {
        if (!KillCounter.isEnabled()) {
        Main.sendMessage(EnumChatFormatting.RED + "Kill counter is not running.");
        return;
    }
    
    var killCounts = KillCounter.getAllKillCounts();
    if (killCounts.isEmpty()) {
        Main.sendMessage(EnumChatFormatting.YELLOW + "Kill counter is running but no kills recorded yet.");
        return;
    }
    
    Main.sendMessage(EnumChatFormatting.GREEN + "Kill counter is running. Current kill counts:");
        
        // Sort by kill count (descending)
        List<Map.Entry<String, Integer>> sortedKills = new ArrayList<>(killCounts.entrySet());
        sortedKills.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        
        for (Map.Entry<String, Integer> entry : sortedKills) {
            String playerName = entry.getKey();
            int kills = entry.getValue();
            Main.sendMessage(EnumChatFormatting.WHITE + "  " + playerName + ": " + 
                       EnumChatFormatting.RED + kills + " kills");
            
            // Show victims for this player
            List<String> victims = KillCounter.getPlayerVictims(playerName);
            if (!victims.isEmpty()) {
                String victimList = String.join(", ", victims);
                Main.sendMessage(EnumChatFormatting.GRAY + "    Victims: " + victimList);
            }
        }
    }
    
    /**
     * Handles the test command.
     */
    private void handleTest() {
        Main.sendMessage(EnumChatFormatting.GOLD + "Testing kit detection functionality...");
        KillCounter.testKitDetection();
        Main.sendMessage(EnumChatFormatting.GREEN + "Kit detection test completed! Check console for results.");
    }
    
    /**
     * Handles the scankits command.
     */
    private void handleScanKits() {
        Main.sendMessage(EnumChatFormatting.GOLD + "Manually scanning for enemy kits...");
        KillCounter.manualKitScan();
        Main.sendMessage(EnumChatFormatting.GREEN + "Kit scan completed! Check console for results.");
    }
    
    /**
     * Sends the help message.
     */
    private void sendHelpMessage() {
        Main.sendMessage(EnumChatFormatting.GOLD + "=== TAB Stats Commands ===");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/startkc - Start tracking kills");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/stopkc - Stop tracking kills");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/startkc reset - Reset kill counts");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/startkc status - Show current kill counts");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/startkc test - Test kit detection functionality");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/startkc scankits - Manually scan for enemy kits");
    }
}
