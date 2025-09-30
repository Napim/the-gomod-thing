package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.ChatReceivedEvent;
import net.weavemc.loader.api.event.EventBus;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.TickEvent;
import me.ballmc.AntiShuffle.Main;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages player connections for showing inventory HUD between connected players
 */
public class InventoryConnection {
    // Connection state flags
    private static final int NOT_CONNECTED = 0;
    private static final int INVITATION_SENT = 1;
    private static final int INVITATION_RECEIVED = 2;
    private static final int CONNECTED = 3;
    
    // Singleton instance
    private static InventoryConnection instance;
    
    // Current connection state
    private int connectionState = NOT_CONNECTED;
    
    // Connected player's name
    private String connectedPlayerName = "";
    
    // Connected player's current inventory
    private ItemStack[] connectedPlayerInventory = new ItemStack[36]; // 27 (chest) + 9 (hotbar)
    
    // Connection request pattern
    private static final Pattern CONNECTION_REQUEST_PATTERN = 
        Pattern.compile("\\[gomod\\] (.*) wants to connect inventories with you\\. Type /acceptconnect to accept\\.");
    
    // Track if we need to update the render
    private boolean inventoryDirty = true;
    
    // Cooldown for connection requests (in ticks)
    private int connectionCooldown = 0;
    private static final int CONNECTION_COOLDOWN_TIME = 200; // 10 seconds at 20 ticks per second
    
    /**
     * Gets the singleton instance
     */
    public static InventoryConnection getInstance() {
        if (instance == null) {
            instance = new InventoryConnection();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private InventoryConnection() {
        EventBus.subscribe(this);
        System.out.println("[InventoryConnection] Initialized");
    }
    
    /**
     * Send a connection request to another player
     * @param playerName Player to connect with
     * @return true if request was sent
     */
    public boolean sendConnectionRequest(String playerName) {
        // Don't allow connections if already connected or request in progress
        if (connectionState != NOT_CONNECTED) {
            if (connectionState == CONNECTED) {
                Main.sendMessage(EnumChatFormatting.RED + "You are already connected to " + 
                    EnumChatFormatting.GOLD + connectedPlayerName + EnumChatFormatting.RED + 
                    ". Type /disconnect first.");
            } else {
                Main.sendMessage(EnumChatFormatting.RED + "You have a pending connection. " + 
                    "Type /disconnect to cancel.");
            }
            return false;
        }
        
        // Check cooldown
        if (connectionCooldown > 0) {
            Main.sendMessage(EnumChatFormatting.RED + "Please wait before sending another connection request.");
            return false;
        }
        
        // Set state and remember player name
        connectionState = INVITATION_SENT;
        connectedPlayerName = playerName;
        
        // Send the command in chat for the other player to see
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(
                "/msg " + playerName + " [gomod] " + 
                Minecraft.getMinecraft().thePlayer.getName() + 
                " wants to connect inventories with you. Type /acceptconnect to accept."
            );
            
            // Set cooldown
            connectionCooldown = CONNECTION_COOLDOWN_TIME;
            
            // Inform the player
            Main.sendMessage(EnumChatFormatting.GREEN + "Connection request sent to " + 
                EnumChatFormatting.GOLD + playerName + EnumChatFormatting.GREEN + ".");
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Accept a connection request from another player
     * @return true if successful
     */
    public boolean acceptConnection() {
        if (connectionState != INVITATION_RECEIVED) {
            Main.sendMessage(EnumChatFormatting.RED + "You don't have any pending connection requests.");
            return false;
        }
        
        // Update state
        connectionState = CONNECTED;
        
        // Send confirmation message to the other player
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(
                "/msg " + connectedPlayerName + " [gomod] " + 
                "I've accepted your inventory connection request."
            );
            
            // Inform the player
            Main.sendMessage(EnumChatFormatting.GREEN + "You are now connected with " + 
                EnumChatFormatting.GOLD + connectedPlayerName + EnumChatFormatting.GREEN + 
                ". You can see each other's inventories.");
            
            // Force inventory update
            inventoryDirty = true;
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Disconnect from the current connection
     */
    public void disconnect() {
        if (connectionState != NOT_CONNECTED) {
            // Notify the other player if we were connected
            if (connectionState == CONNECTED && Minecraft.getMinecraft().thePlayer != null) {
                Minecraft.getMinecraft().thePlayer.sendChatMessage(
                    "/msg " + connectedPlayerName + " [gomod] " + 
                    "I've disconnected our inventory connection."
                );
            }
            
            // Reset state
            connectionState = NOT_CONNECTED;
            
            // Inform the player
            if (connectionState == CONNECTED) {
                Main.sendMessage(EnumChatFormatting.YELLOW + "Disconnected from " + 
                    EnumChatFormatting.GOLD + connectedPlayerName + EnumChatFormatting.YELLOW + ".");
            } else {
                Main.sendMessage(EnumChatFormatting.YELLOW + "Connection request canceled.");
            }
            
            // Clear data
            connectedPlayerName = "";
            connectedPlayerInventory = new ItemStack[36];
        } else {
            Main.sendMessage(EnumChatFormatting.RED + "You are not connected to anyone.");
        }
    }
    
    /**
     * Handle chat messages for connection requests
     */
    @SubscribeEvent
    public void onChatReceived(ChatReceivedEvent event) {
        if (event.getMessage() instanceof ChatComponentText) {
            String message = event.getMessage().getUnformattedText();
            
            // Check for connection requests
            Matcher matcher = CONNECTION_REQUEST_PATTERN.matcher(message);
            if (matcher.find()) {
                String requesterName = matcher.group(1);
                
                // Set state to invitation received
                connectionState = INVITATION_RECEIVED;
                connectedPlayerName = requesterName;
                
                // Highlight the message for better visibility
                Main.sendMessage(EnumChatFormatting.GREEN + "Connection request from " + 
                    EnumChatFormatting.GOLD + requesterName + EnumChatFormatting.GREEN + 
                    ". Type /acceptconnect to accept or /disconnect to decline.");
            }
            
            // Check for accepted connections
            if (connectionState == INVITATION_SENT && 
                message.contains("[gomod] I've accepted your inventory connection request")) {
                // Extract the sender name from the message
                String senderInfo = message.split("\\[gomod\\]")[0].trim();
                if (senderInfo.contains(connectedPlayerName)) {
                    connectionState = CONNECTED;
                    
                    // Inform the player
                    Main.sendMessage(EnumChatFormatting.GREEN + "Connected with " + 
                        EnumChatFormatting.GOLD + connectedPlayerName + EnumChatFormatting.GREEN + 
                        ". You can now see each other's inventories.");
                    
                    // Force inventory update
                    inventoryDirty = true;
                }
            }
            
            // Check for disconnection messages
            if (connectionState == CONNECTED && 
                message.contains("[gomod] I've disconnected our inventory connection")) {
                // Extract the sender name
                String senderInfo = message.split("\\[gomod\\]")[0].trim();
                if (senderInfo.contains(connectedPlayerName)) {
                    connectionState = NOT_CONNECTED;
                    
                    // Inform the player
                    Main.sendMessage(EnumChatFormatting.YELLOW + 
                        connectedPlayerName + " has disconnected from the inventory connection.");
                    
                    // Clear data
                    connectedPlayerName = "";
                    connectedPlayerInventory = new ItemStack[36];
                }
            }
            
            // Check for inventory updates (would be implemented via custom messages)
            if (connectionState == CONNECTED && message.startsWith("[gomod-inv]")) {
                // In a real implementation, this would parse and update inventory data
                // from a serialized format sent by the other player
                inventoryDirty = true;
            }
        }
    }
    
    /**
     * Game tick event handler for periodic updates
     */
    @SubscribeEvent
    public void onTick(TickEvent event) {
        // Update connection cooldown
        if (connectionCooldown > 0) {
            connectionCooldown--;
        }
        
        // If we're connected, we'd send inventory updates periodically
        if (connectionState == CONNECTED) {
            // In a complete implementation, this would periodically send inventory data 
            // to the connected player via messages or another communication channel
        }
    }
    
    /**
     * Get the current connection state
     */
    public int getConnectionState() {
        return connectionState;
    }
    
    /**
     * Get the connected player's name
     */
    public String getConnectedPlayerName() {
        return connectedPlayerName;
    }
    
    /**
     * Get the connected player's inventory
     */
    public ItemStack[] getConnectedPlayerInventory() {
        return connectedPlayerInventory;
    }
    
    /**
     * Check if we're currently connected
     */
    public boolean isConnected() {
        return connectionState == CONNECTED;
    }
    
    /**
     * Check if the inventory needs to be redrawn
     */
    public boolean isInventoryDirty() {
        return inventoryDirty;
    }
    
    /**
     * Mark the inventory as clean after redrawing
     */
    public void setInventoryClean() {
        inventoryDirty = false;
    }
} 