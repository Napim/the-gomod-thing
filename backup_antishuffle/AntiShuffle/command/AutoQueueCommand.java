package me.ballmc.AntiShuffle.command;

import net.weavemc.loader.api.command.Command;
import me.ballmc.AntiShuffle.features.AutoQueue;
import me.ballmc.AntiShuffle.features.AutoQueue.QueueMode;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

/**
 * Command to toggle auto-queue functionality for rejoining games.
 * Usage: /autoq [teams|solo|off]
 */
public class AutoQueueCommand extends Command {
    
    public AutoQueueCommand() {
        super("autoq");
    }
    
    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            // Toggle mode cycling: DISABLED -> TEAMS -> SOLO -> DISABLED
            QueueMode currentMode = AutoQueue.getMode();
            QueueMode newMode;
            
            switch (currentMode) {
                case DISABLED:
                    newMode = QueueMode.TEAMS;
                    break;
                case TEAMS:
                    newMode = QueueMode.SOLO;
                    break;
                default: // SOLO or any other value
                    newMode = QueueMode.DISABLED;
                    break;
            }
            
            // Set the new mode and send notification
            String message = AutoQueue.setMode(newMode);
            sendMessage(message);
            return;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "teams":
                String teamsMessage = AutoQueue.setMode(QueueMode.TEAMS);
                sendMessage(teamsMessage);
                break;
                
            case "solo":
                String soloMessage = AutoQueue.setMode(QueueMode.SOLO);
                sendMessage(soloMessage);
                break;
                
            case "off":
            case "disable":
                String disableMessage = AutoQueue.setMode(QueueMode.DISABLED);
                sendMessage(disableMessage);
                break;
                
            default:
                sendMessage(EnumChatFormatting.RED + "Unknown auto-queue mode: " + subCommand);
                sendMessage(EnumChatFormatting.YELLOW + "Available modes: teams, solo, off");
        }
    }
    
    private void sendMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
        }
    }
} 