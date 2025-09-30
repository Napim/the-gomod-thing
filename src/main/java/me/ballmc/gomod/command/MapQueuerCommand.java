package me.ballmc.gomod.command;

import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;
import me.ballmc.gomod.features.MapQueuer;
import me.ballmc.gomod.Main;

public class MapQueuerCommand extends Command {

    public MapQueuerCommand() {
        super("mapqueuer");
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "start":
                if (args.length < 3) {
                    Main.sendMessage(EnumChatFormatting.RED + "Usage: /mapqueuer start <map> <solo|teams>");
                    Main.sendMessage(EnumChatFormatting.YELLOW + "Example: /mapqueuer start Caelum solo");
                    return;
                }
                
                String map = args[1];
                String mode = args[2].toLowerCase();
                
                if (!mode.equals("solo") && !mode.equals("teams")) {
                    Main.sendMessage(EnumChatFormatting.RED + "Mode must be 'solo' or 'teams'");
                    return;
                }
                
                MapQueuer.startMapQueuer(map, mode);
                break;
                
            case "stop":
                System.out.println("[MapQueuerCommand] Stop command received");
                MapQueuer.stopMapQueuer();
                break;
                
            case "resume":
                System.out.println("[MapQueuerCommand] Resume command received");
                MapQueuer.resumeMapQueuer();
                break;
                
            case "status":
                showStatus();
                break;
                
            case "maps":
                showMaps();
                break;
                
            case "gui":
                Minecraft.getMinecraft().displayGuiScreen(new me.ballmc.gomod.gui.MapQueuerGUI(null));
                break;
                
            case "debug":
                debugScoreboard();
                break;
                
            case "clearcache":
                MapQueuer.clearGameIdCache();
                Main.sendMessage(EnumChatFormatting.GREEN + "Game ID cache cleared!");
                break;
                
            case "filtermap":
                filterCurrentMap();
                break;
                
            default:
                showHelp();
                break;
        }
    }

    private void showHelp() {
        Main.sendMessage(EnumChatFormatting.GOLD + "=== Map Queuer Commands ===");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer start <map> <solo|teams>" + EnumChatFormatting.WHITE + " - Start queuing for a specific map");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer stop" + EnumChatFormatting.WHITE + " - Stop the map queuer");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer resume" + EnumChatFormatting.WHITE + " - Resume with last used settings");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer status" + EnumChatFormatting.WHITE + " - Show current status");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer maps" + EnumChatFormatting.WHITE + " - List all available maps");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer gui" + EnumChatFormatting.WHITE + " - Open the map selection GUI");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer debug" + EnumChatFormatting.WHITE + " - Debug scoreboard reading");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer clearcache" + EnumChatFormatting.WHITE + " - Clear game ID cache");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/mapqueuer filtermap" + EnumChatFormatting.WHITE + " - Filter current map's game ID");
        Main.sendMessage(EnumChatFormatting.GRAY + "Use /gomod to access the Map Queuer GUI");
    }

    private void showStatus() {
        if (MapQueuer.isRunning()) {
            Main.sendMessage(EnumChatFormatting.GREEN + "Map Queuer is running!");
            Main.sendMessage(EnumChatFormatting.AQUA + "Target Map: " + EnumChatFormatting.WHITE + MapQueuer.getTargetMap());
            Main.sendMessage(EnumChatFormatting.AQUA + "Mode: " + EnumChatFormatting.WHITE + MapQueuer.getGameMode());
        } else {
            Main.sendMessage(EnumChatFormatting.RED + "Map Queuer is not running.");
        }
    }

    private void showMaps() {
        Main.sendMessage(EnumChatFormatting.GOLD + "=== Available Blitz Maps ===");
        
        String[] allMaps = {
            "Caelum", "Caelum v2", "Cattle Drive", "Egypt", "Enthorran", "Greece", "Hamani",
            "Mithril Revived", "Mirador Basin", "Peaks", "Persia", "Shroom Valley", "Stoneguard",
            "Thorin", "Valley", "Docks v1", "Docks v2", "Seafloor", "Aelin's Tower", "Alice",
            "Bastion", "City", "Citadel", "Darkstone", "Despair v1", "Despair v2", "Impact",
            "KTulu Island", "Moonbase", "Pandora", "Pixelville", "Proxima", "Riverside", "Ruins",
            "Shogun", "Winter", "Woodlands"
        };
        
        StringBuilder soloMaps = new StringBuilder();
        StringBuilder teamsMaps = new StringBuilder();
        StringBuilder bothMaps = new StringBuilder();
        
        for (String map : allMaps) {
            if (MapQueuer.isTeamsOnlyMap(map)) {
                teamsMaps.append(EnumChatFormatting.RED).append(map).append(EnumChatFormatting.WHITE).append(", ");
            } else if (MapQueuer.isSoloOnlyMap(map)) {
                soloMaps.append(EnumChatFormatting.BLUE).append(map).append(EnumChatFormatting.WHITE).append(", ");
            } else {
                bothMaps.append(EnumChatFormatting.GREEN).append(map).append(EnumChatFormatting.WHITE).append(", ");
            }
        }
        
        Main.sendMessage(EnumChatFormatting.AQUA + "Solo-Only Maps:");
        Main.sendMessage(soloMaps.toString());
        
        Main.sendMessage(EnumChatFormatting.AQUA + "Teams-Only Maps:");
        Main.sendMessage(teamsMaps.toString());
        
        Main.sendMessage(EnumChatFormatting.AQUA + "Available in Both Modes:");
        Main.sendMessage(bothMaps.toString());
        
        Main.sendMessage(EnumChatFormatting.BLUE + "Blue = Solo-only, " + EnumChatFormatting.RED + "Red = Teams-only, " + EnumChatFormatting.GREEN + "Green = Both modes");
    }
    
    private void debugScoreboard() {
        Main.sendMessage(EnumChatFormatting.GOLD + "=== Scoreboard Debug ===");
        
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
            if (mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
                Main.sendMessage(EnumChatFormatting.RED + "No scoreboard available");
                return;
            }
            
            net.minecraft.scoreboard.Scoreboard scoreboard = mc.theWorld.getScoreboard();
            net.minecraft.scoreboard.ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
            
            if (objective == null) {
                Main.sendMessage(EnumChatFormatting.RED + "No sidebar objective found");
                return;
            }
            
            Main.sendMessage(EnumChatFormatting.AQUA + "Scoreboard lines:");
            for (net.minecraft.scoreboard.Score score : scoreboard.getSortedScores(objective)) {
                net.minecraft.scoreboard.ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String line = net.minecraft.scoreboard.ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                Main.sendMessage(EnumChatFormatting.WHITE + "  " + line);
                // Also show cleaned line used for gameId detection
                String clean = line.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
                clean = clean.replaceAll("§[0-9A-Fa-f]", "");
                clean = clean.replaceAll("§[0-9A-F]", "");
                Main.sendMessage(EnumChatFormatting.GRAY + "    cleaned: " + clean);
            }
            
            String gameId = me.ballmc.gomod.features.MapQueuer.getCurrentGameId();
            if (gameId != null) {
                Main.sendMessage(EnumChatFormatting.GREEN + "Detected game ID: " + gameId);
            } else {
                Main.sendMessage(EnumChatFormatting.RED + "No game ID detected");
            }

            String currentMap = me.ballmc.gomod.features.MapQueuer.getCurrentMap();
            if (currentMap != null) {
                Main.sendMessage(EnumChatFormatting.GREEN + "Detected map: " + currentMap);
            } else {
                Main.sendMessage(EnumChatFormatting.RED + "No map detected");
            }
            
        } catch (Exception e) {
            Main.sendMessage(EnumChatFormatting.RED + "Error reading scoreboard: " + e.getMessage());
        }
    }
    
    private void filterCurrentMap() {
        try {
            // Get the current game ID using the same logic as MapQueuer
            String currentGameId = me.ballmc.gomod.features.MapQueuer.getCurrentGameId();
            
            if (currentGameId == null || currentGameId.isEmpty()) {
                Main.sendMessage(EnumChatFormatting.RED + "No game ID detected! Make sure you're in a game or lobby.");
                return;
            }
            
            // Add the game ID to the filter list using the GUI's filter system
            me.ballmc.gomod.gui.MapQueuerGUI.addGameFilter(currentGameId);
            
            Main.sendMessage(EnumChatFormatting.GREEN + "Added game filter: " + currentGameId + " (15 minutes)");
            Main.sendMessage(EnumChatFormatting.AQUA + "This game will now be filtered for 15 minutes.");
            
        } catch (Exception e) {
            Main.sendMessage(EnumChatFormatting.RED + "Error filtering current map: " + e.getMessage());
        }
    }
}
