package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import me.ballmc.AntiShuffle.features.PlayerLooker;
import net.minecraft.entity.player.EntityPlayer;

public class PlayerLookCommand extends Command {
    private final PlayerLooker playerLooker;
    
    public PlayerLookCommand(PlayerLooker playerLooker) {
        super("plook");
        this.playerLooker = playerLooker;
    }
    
    @Override
    public void handle(String[] args) {
        // Toggle feature or clear target with no args
        if (args.length == 0) {
            boolean newState = !playerLooker.isEnabled();
            playerLooker.setEnabled(newState);
            
            if (newState) {
                // If enabling without a target, clear any previous target
                playerLooker.setTargetPlayerName(null);
                sendMessage(EnumChatFormatting.BLUE + "Player Looker " + 
                            EnumChatFormatting.GREEN + "enabled" + 
                            EnumChatFormatting.BLUE + " (tracking nearest player)");
            } else {
                sendMessage(EnumChatFormatting.BLUE + "Player Looker " + 
                            EnumChatFormatting.RED + "disabled");
            }
            return;
        }
        
        // Handle /plook off command
        if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            playerLooker.setEnabled(false);
            sendMessage(EnumChatFormatting.BLUE + "Player Looker " + 
                        EnumChatFormatting.RED + "disabled");
            return;
        }
        
        // Handle /plook <player> command
        String targetPlayerName = args[0];
        boolean playerExists = false;
        
        // Check if player exists in the world
        if (Minecraft.getMinecraft().theWorld != null) {
            for (EntityPlayer player : Minecraft.getMinecraft().theWorld.playerEntities) {
                if (player.getName().equalsIgnoreCase(targetPlayerName)) {
                    playerExists = true;
                    break;
                }
            }
        }
        
        // Set the target and enable tracking
        playerLooker.setTargetPlayerName(targetPlayerName);
        playerLooker.setEnabled(true);
        
        // Provide feedback
        if (playerExists) {
            sendMessage(EnumChatFormatting.BLUE + "Now tracking player: " + 
                        EnumChatFormatting.YELLOW + targetPlayerName);
        } else {
            sendMessage(EnumChatFormatting.BLUE + "Set to track player: " + 
                        EnumChatFormatting.YELLOW + targetPlayerName + 
                        EnumChatFormatting.RED + " (player not found in the current world)");
        }
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 