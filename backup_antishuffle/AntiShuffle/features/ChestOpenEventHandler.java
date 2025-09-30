package me.ballmc.AntiShuffle.features;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.EventBus;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.weavemc.loader.api.event.TickEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;

/**
 * Handles chest open events and sends a chat message listing the items in the chest
 * when the itemcall feature is enabled.
 */
public class ChestOpenEventHandler {
    
    private static boolean initialized = false;
    private static final int MAX_MESSAGE_LENGTH = 100;
    private long lastMessageTime = 0;
    private static final long MESSAGE_COOLDOWN = 500; // 500ms cooldown
    private boolean hasProcessedCurrentChest = false;
    private static final Pattern COLOR_CODE_PATTERN = Pattern.compile("§[0-9a-fk-or]");
    private Map<Integer, ItemStack> previousItems = new HashMap<>();
    private Map<String, ItemStack> itemsToVerify = new HashMap<>(); // Track items that need verification
    private Map<String, Boolean> itemWasInInventory = new HashMap<>(); // Track if item was ever in our inventory
    private List<String> lootedItems = new ArrayList<>();
    private Set<String> reportedItems = new HashSet<>(); // Track which items we've already reported
    private boolean isTrackingChest = false;

    // List of items we want to track
    private static final Set<String> TRACKED_ITEMS = new HashSet<>(Arrays.asList(
        "tile.diamond",
        "tile.ingotIron",
        "tile.ingotGold"
    ));

    private boolean shouldTrackItem(ItemStack stack) {
        if (stack == null) return false;
        
        String itemName = stack.getItem().getUnlocalizedName().toLowerCase();
        System.out.println("Checking item: " + itemName); // Debug line to see item names
        
        // In 1.8.9 these are the correct names
        return itemName.contains("diamond") || 
               itemName.contains("ingotiron") || 
               itemName.contains("ingotgold");
    }

    public ChestOpenEventHandler() {
        EventBus.subscribe(this);
        initialize();
    }
    
    private static void initialize() {
        if (initialized) {
            return;
        }
        
        initialized = true;
        System.out.println("[ChestOpenEventHandler] Initialized");
    }
    
    private String stripColorCodes(String text) {
        if (text == null) return "";
        return COLOR_CODE_PATTERN.matcher(text).replaceAll("");
    }
    
    private boolean isRealChest(Container container) {
        if (container == null) return false;
        
        // Get the chest's title/name
        String chestTitle = "";
        try {
            IInventory inventory = ((GuiChest) Minecraft.getMinecraft().currentScreen).lowerChestInventory;
            if (inventory != null) {
                chestTitle = inventory.getDisplayName().getUnformattedText();
            }
        } catch (Exception e) {
            return false;
        }

        // Strip color codes and check if it's a real chest
        chestTitle = stripColorCodes(chestTitle.toLowerCase());
        return chestTitle.equals("chest") || chestTitle.contains("chest");
    }
    
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!ConfigManager.isItemCallEnabled()) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) {
            isTrackingChest = false;
            hasProcessedCurrentChest = false;
            lootedItems.clear();
            previousItems.clear();
            itemsToVerify.clear();
            reportedItems.clear();
            return;
        }

        // Check for stolen items on every tick
        checkForStolenItems();

        // If the screen was just closed and we were tracking a chest
        if (mc.currentScreen == null && isTrackingChest) {
            isTrackingChest = false;
            hasProcessedCurrentChest = false;
            lootedItems.clear();
            previousItems.clear();
            reportedItems.clear();
            return;
        }
        
        // Check if we're in a chest GUI and it's a real chest
        if (mc.currentScreen instanceof GuiChest) {
            Container container = mc.thePlayer.openContainer;
            if (!isRealChest(container)) {
                isTrackingChest = false;
                hasProcessedCurrentChest = true;
                lootedItems.clear();
                return;
            }
            
            if (!isTrackingChest) {
                // Just opened the chest, store initial items
                previousItems.clear();
                lootedItems.clear();
                reportedItems.clear();
                for (int i = 0; i < container.inventorySlots.size() && i < 27; i++) {
                    Slot slot = container.getSlot(i);
                    if (slot != null && slot.getHasStack()) {
                        ItemStack stack = slot.getStack();
                        if (stack != null) {
                            previousItems.put(i, stack.copy());
                        }
                    }
                }
                isTrackingChest = true;
                hasProcessedCurrentChest = true;
            } else {
                // Check for changes while chest is open
                checkForTakenItems(container);
            }
        }
    }
    
    private void checkForTakenItems(Container container) {
        List<Integer> slotsToRemove = new ArrayList<>();
        Map<Integer, ItemStack> itemsToUpdate = new HashMap<>();
        List<String> newLootedItems = new ArrayList<>();
        
        for (Map.Entry<Integer, ItemStack> entry : previousItems.entrySet()) {
            int slot = entry.getKey();
            ItemStack previousStack = entry.getValue();
            
            ItemStack currentStack = null;
            if (container != null && slot < container.inventorySlots.size()) {
                Slot currentSlot = container.getSlot(slot);
                if (currentSlot != null && currentSlot.getHasStack()) {
                    currentStack = currentSlot.getStack();
                }
            }
            
            if (previousStack != null && shouldTrackItem(previousStack)) {
                if (currentStack == null || !ItemStack.areItemStacksEqual(previousStack, currentStack)) {
                    String itemName = stripColorCodes(previousStack.getDisplayName());
                    int takenAmount = currentStack == null ? previousStack.stackSize : 
                                    (previousStack.stackSize - currentStack.stackSize);
                    
                    if (takenAmount > 0) {
                        String lootEntry = itemName + " x" + takenAmount;
                        if (!reportedItems.contains(lootEntry)) {
                            newLootedItems.add(lootEntry);
                            itemsToVerify.put(lootEntry, previousStack.copy());
                            itemWasInInventory.put(lootEntry, false);
                            reportedItems.add(lootEntry);
                        }
                        
                        if (currentStack == null) {
                            slotsToRemove.add(slot);
                        } else {
                            itemsToUpdate.put(slot, currentStack.copy());
                        }
                    }
                }
            }
        }
        
        for (Integer slot : slotsToRemove) {
            previousItems.remove(slot);
        }
        
        previousItems.putAll(itemsToUpdate);
        
        if (!newLootedItems.isEmpty()) {
            lootedItems.clear();
            lootedItems.addAll(newLootedItems);
            sendLootedItemsMessage();
        }
    }
    
    private void checkForStolenItems() {
        if (itemsToVerify.isEmpty()) return;
        
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null) return;
        
        // Create a copy of the keys to avoid concurrent modification
        List<String> itemsToCheck = new ArrayList<>(itemsToVerify.keySet());
        
        for (String lootEntry : itemsToCheck) {
            ItemStack stack = itemsToVerify.get(lootEntry);
            if (stack == null) continue;
            
            boolean itemFound = false;
            
            // Check main inventory
            for (ItemStack invStack : mc.thePlayer.inventory.mainInventory) {
                if (invStack != null && invStack.getItem() == stack.getItem()) {
                    itemFound = true;
                    itemWasInInventory.put(lootEntry, true); // Mark that we had this item
                    break;
                }
            }
            
            // Check armor slots
            if (!itemFound) {
                for (ItemStack armorStack : mc.thePlayer.inventory.armorInventory) {
                    if (armorStack != null && armorStack.getItem() == stack.getItem()) {
                        itemFound = true;
                        itemWasInInventory.put(lootEntry, true); // Mark that we had this item
                        break;
                    }
                }
            }
            
            // If we found the item, keep tracking
            if (itemFound) {
                continue;
            }
            
            // If we didn't find the item
            Boolean wasInInventory = itemWasInInventory.get(lootEntry);
            if (wasInInventory == null || !wasInInventory) {
                // If the item was never in our inventory and now we can't find it, it was stolen
                String stolenMessage = "#STOLEN: " + lootEntry;
                sendPlayerMessage(stolenMessage);
            }
            
            // Stop tracking this item
            itemsToVerify.remove(lootEntry);
            itemWasInInventory.remove(lootEntry);
        }
    }
    
    private void sendLootedItemsMessage() {
        // Prevent double messages
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMessageTime < MESSAGE_COOLDOWN) return;
        lastMessageTime = currentTime;
        
        if (lootedItems.isEmpty()) return; // Don't send empty messages
        
        // Prepare all messages
        List<String> messages = new ArrayList<>();
        StringBuilder currentMessage = new StringBuilder("#Looted: ");
        
        for (String item : lootedItems) {
            String nextItem = item + ", ";
            if (currentMessage.length() + nextItem.length() > MAX_MESSAGE_LENGTH) {
                currentMessage.setLength(currentMessage.length() - 2); // Remove last ", "
                messages.add(currentMessage.toString());
                currentMessage = new StringBuilder("#");
            }
            currentMessage.append(nextItem);
        }
        
        // Add final message if needed
        if (currentMessage.length() > 2) { // Only if we have items
            currentMessage.setLength(currentMessage.length() - 2); // Remove last ", "
            messages.add(currentMessage.toString());
            
            // Send all messages in order with a small delay
            new Thread(() -> {
                try {
                    for (String message : messages) {
                        sendPlayerMessage(message);
                        Thread.sleep(50); // 50ms delay between messages
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
    
    private void sendPlayerMessage(String message) {
        if (Minecraft.getMinecraft().thePlayer != null) {
            Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
        }
    }
} 