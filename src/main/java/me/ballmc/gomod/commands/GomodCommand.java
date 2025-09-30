package me.ballmc.gomod.commands;

import me.ballmc.gomod.gui.GomodGUI;
import me.ballmc.gomod.gui.GoStatsGUI;
import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.ApiKeyManager;
import me.ballmc.gomod.features.ConfigManager;
import net.weavemc.loader.api.command.Command;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.client.Minecraft;
import static me.ballmc.gomod.Main.sendMessage;

public class GomodCommand extends Command {
    public GomodCommand() {
        super("gomod");
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            // Delay opening slightly so ChatScreen can close first, then open on main thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100L);
                    } catch (InterruptedException ignored) {}
                    Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                        @Override
                        public void run() {
                            Minecraft.getMinecraft().displayGuiScreen(new GomodGUI());
                        }
                    });
                }
            }, "OpenGomodGui").start();
        } else if (args.length >= 1) {
            String subCommand = args[0].toLowerCase();
            
            switch (subCommand) {
                case "help":
                    showHelp();
                    break;
                case "gostats":
                case "gostats123":
                    openGoStatsGUI();
                    break;
                case "api":
                    handleApiCommand(args);
                    break;
                case "status":
                    showStatus();
                    break;
                case "mapqueuer":
                    openMapQueuerGUI();
                    break;
                case "playerqueuer":
                    openPlayerQueuerGUI();
                    break;
                default:
                    showHelp();
                    break;
            }
        }
    }
    
    private void showHelp() {
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Gomod Commands:");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod" + EnumChatFormatting.WHITE + " - Open Gomod Main Menu GUI");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod gostats" + EnumChatFormatting.WHITE + " - Open GoStats AI Settings");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod mapqueuer" + EnumChatFormatting.WHITE + " - Open Map Queuer GUI");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod playerqueuer" + EnumChatFormatting.WHITE + " - Open Player Queuer GUI");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod api openai <key>" + EnumChatFormatting.WHITE + " - Set OpenAI API key");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod api hypixel <key>" + EnumChatFormatting.WHITE + " - Set Hypixel API key");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod status" + EnumChatFormatting.WHITE + " - Show current status");
        sendMessage(EnumChatFormatting.YELLOW + "  /gomod help" + EnumChatFormatting.WHITE + " - Show this help message");
        sendMessage(EnumChatFormatting.GRAY + "Access all gomod features from the main menu.");
    }
    
    private void openGoStatsGUI() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {}
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().displayGuiScreen(new GoStatsGUI());
                    }
                });
            }
        }, "OpenGoStatsGui").start();
    }
    
    private void openMapQueuerGUI() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {}
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().displayGuiScreen(new me.ballmc.gomod.gui.MapQueuerGUI(null));
                    }
                });
            }
        }, "OpenMapQueuerGui").start();
    }
    
    private void openPlayerQueuerGUI() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException ignored) {}
                Minecraft.getMinecraft().addScheduledTask(new Runnable() {
                    @Override
                    public void run() {
                        Minecraft.getMinecraft().displayGuiScreen(new me.ballmc.gomod.gui.PlayerQueuerGUI(null));
                    }
                });
            }
        }, "OpenPlayerQueuerGui").start();
    }
    
    private void handleApiCommand(String[] args) {
        if (args.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: /gomod api <service> <key>");
            sendMessage(EnumChatFormatting.YELLOW + "Available services: openai, hypixel");
            return;
        }
        
        String service = args[1].toLowerCase();
        if (args.length < 3) {
            // Show current API key status
            if (service.equals("openai")) {
                if (ApiKeyManager.hasApiKey("openai")) {
                    sendMessage(EnumChatFormatting.GREEN + "OpenAI API key is set: " + ApiKeyManager.getMaskedApiKey("openai"));
                } else {
                    sendMessage(EnumChatFormatting.RED + "No OpenAI API key is set.");
                    sendMessage(EnumChatFormatting.YELLOW + "Usage: /gomod api openai YOUR_API_KEY");
                }
            } else if (service.equals("hypixel")) {
                if (ApiKeyManager.hasApiKey("hypixel")) {
                    sendMessage(EnumChatFormatting.GREEN + "Hypixel API key is set: " + ApiKeyManager.getMaskedApiKey("hypixel"));
                } else {
                    sendMessage(EnumChatFormatting.RED + "No Hypixel API key is set.");
                    sendMessage(EnumChatFormatting.YELLOW + "Usage: /gomod api hypixel YOUR_API_KEY");
                }
            } else {
                sendMessage(EnumChatFormatting.RED + "Unknown service: " + service);
                sendMessage(EnumChatFormatting.YELLOW + "Available services: openai, hypixel");
            }
            return;
        }
        
        // Set API key
        if (service.equals("openai")) {
            String apiKey = args[2];
            ApiKeyManager.setApiKey("openai", apiKey);
            sendMessage(EnumChatFormatting.GREEN + "OpenAI API key has been set successfully!");
            sendMessage(EnumChatFormatting.YELLOW + "GoStats AI is now ready to use.");
        } else if (service.equals("hypixel")) {
            String apiKey = args[2];
            ApiKeyManager.setApiKey("hypixel", apiKey);
            sendMessage(EnumChatFormatting.GREEN + "Hypixel API key has been set successfully!");
            sendMessage(EnumChatFormatting.YELLOW + "Kill effect commands are now ready to use.");
        } else {
            sendMessage(EnumChatFormatting.RED + "Unknown service: " + service);
            sendMessage(EnumChatFormatting.YELLOW + "Available services: openai, hypixel");
        }
    }
    
    private void showStatus() {
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.BLUE + "Gomod Status:");
        sendMessage(EnumChatFormatting.YELLOW + "GoStats AI: " + 
                   (ConfigManager.isGoStatsEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        sendMessage(EnumChatFormatting.YELLOW + "OpenAI API: " + 
                   (ApiKeyManager.hasApiKey("openai") ? EnumChatFormatting.GREEN + "Configured" : EnumChatFormatting.RED + "Not Configured"));
        sendMessage(EnumChatFormatting.YELLOW + "Hypixel API: " + 
                   (ApiKeyManager.hasApiKey("hypixel") ? EnumChatFormatting.GREEN + "Configured" : EnumChatFormatting.RED + "Not Configured"));
        sendMessage(EnumChatFormatting.YELLOW + "Auto Queue: " + 
                   (ConfigManager.isAutoQueueEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
        sendMessage(EnumChatFormatting.YELLOW + "Inventory HUD: " + 
                   (ConfigManager.isInventoryHUDEnabled() ? EnumChatFormatting.GREEN + "Enabled" : EnumChatFormatting.RED + "Disabled"));
    }
}
