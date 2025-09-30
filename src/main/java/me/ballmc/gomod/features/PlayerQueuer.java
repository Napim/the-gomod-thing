package me.ballmc.gomod.features;

import me.ballmc.gomod.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.weavemc.loader.api.event.SubscribeEvent;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PlayerQueuer feature queues games aiming to join lobbies containing one of the target players.
 *
 * Behavior:
 * - Maintain a list of target player names.
 * - When the chat announces "The game starts in 2 seconds!", if none of the targets are in TAB, requeue.
 * - Chooses /play blitz_solo_normal or /play blitz_teams_normal based on scoreboard containing the word "Team".
 *
 * Persistence:
 * - Saves enabled flag and target players to ~/.weave/gomod/playerqueuer.json
 */
public class PlayerQueuer {
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.weave/gomod";
    private static final String CONFIG_FILE = CONFIG_DIR + "/playerqueuer.json";

    private static volatile boolean enabled = false;
    private static final Set<String> targetPlayers = new HashSet<>();
    private static final Set<String> excludePlayers = new HashSet<>();
    private static volatile boolean requireAllInclude = false; // false = any include, true = all include
    private static volatile boolean requireAllExclude = false; // false = any exclude triggers, true = all exclude triggers

    /** Loads configuration from disk. */
    public static void loadConfig() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(CONFIG_FILE);
            if (!file.exists()) {
                saveConfig();
                return;
            }
            try (FileReader reader = new FileReader(file)) {
                com.google.gson.JsonObject obj = new com.google.gson.JsonParser().parse(reader).getAsJsonObject();
                enabled = obj.has("enabled") && obj.get("enabled").getAsBoolean();
                targetPlayers.clear();
                if (obj.has("players") && obj.get("players").isJsonArray()) {
                    com.google.gson.JsonArray arr = obj.getAsJsonArray("players");
                    for (int i = 0; i < arr.size(); i++) {
                        String name = arr.get(i).getAsString();
                        if (name != null && !name.trim().isEmpty()) {
                            targetPlayers.add(name.trim().toLowerCase());
                        }
                    }
                }
                excludePlayers.clear();
                if (obj.has("excludePlayers") && obj.get("excludePlayers").isJsonArray()) {
                    com.google.gson.JsonArray arr = obj.getAsJsonArray("excludePlayers");
                    for (int i = 0; i < arr.size(); i++) {
                        String name = arr.get(i).getAsString();
                        if (name != null && !name.trim().isEmpty()) {
                            excludePlayers.add(name.trim().toLowerCase());
                        }
                    }
                }
                // Backward compatibility for older config key
                if (obj.has("requireAllPlayers")) {
                    requireAllInclude = obj.get("requireAllPlayers").getAsBoolean();
                }
                if (obj.has("requireAllInclude")) {
                    requireAllInclude = obj.get("requireAllInclude").getAsBoolean();
                }
                if (obj.has("requireAllExclude")) {
                    requireAllExclude = obj.get("requireAllExclude").getAsBoolean();
                }
            }
        } catch (Exception e) {
            System.err.println("[PlayerQueuer] Failed to load config: " + e.getMessage());
        }
    }

    /** Saves configuration to disk. */
    private static void saveConfig() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists()) dir.mkdirs();
            File file = new File(CONFIG_FILE);
            com.google.gson.JsonObject obj = new com.google.gson.JsonObject();
            obj.addProperty("enabled", enabled);
            obj.addProperty("requireAllInclude", requireAllInclude);
            obj.addProperty("requireAllExclude", requireAllExclude);
            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
            for (String p : new ArrayList<>(targetPlayers)) {
                arr.add(new com.google.gson.JsonPrimitive(p));
            }
            obj.add("players", arr);
            com.google.gson.JsonArray arrEx = new com.google.gson.JsonArray();
            for (String p : new ArrayList<>(excludePlayers)) {
                arrEx.add(new com.google.gson.JsonPrimitive(p));
            }
            obj.add("excludePlayers", arrEx);
            try (FileWriter writer = new FileWriter(file)) {
                new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(obj, writer);
            }
        } catch (Exception e) {
            System.err.println("[PlayerQueuer] Failed to save config: " + e.getMessage());
        }
    }

    public static boolean isEnabled() { return enabled; }

    public static void setEnabled(boolean value) {
        enabled = value;
        saveConfig();
        Main.sendMessage(Main.CHAT_PREFIX + (value ? EnumChatFormatting.GREEN + "Player Queuer enabled" : EnumChatFormatting.RED + "Player Queuer disabled"));
    }

    /** Silent enable/disable for GUI usage: no chat messages. */
    public static void setEnabledSilent(boolean value) {
        enabled = value;
        saveConfig();
    }

    public static List<String> getTargets() {
        return new ArrayList<>(targetPlayers);
    }

    public static String addTarget(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Main.CHAT_PREFIX + EnumChatFormatting.RED + "Provide a player name.";
        }
        final String requested = name.trim();

        // Resolve and correct username off-thread to avoid UI hitch
        new Thread(() -> {
            String correctedName = correctUsername(requested);
            String norm = correctedName.toLowerCase();
            synchronized (targetPlayers) {
                if (!targetPlayers.contains(norm)) {
                    targetPlayers.add(norm);
                    saveConfig();
                }
            }
            // Notify on client thread
            try {
                Minecraft.getMinecraft().addScheduledTask(() -> Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Added target: " + correctedName));
            } catch (Throwable ignored) {}
        }, "PQ-AddInclude").start();

        return Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Adding target...";
    }

    /** Silent variant for GUI usage: no chat messages. */
    public static void addTargetSilent(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        final String requested = name.trim();
        
        // Add immediately with original name, then correct in background
        String norm = requested.toLowerCase();
        synchronized (targetPlayers) {
            if (!targetPlayers.contains(norm)) {
                targetPlayers.add(norm);
                saveConfig();
            }
        }
        
        // Correct username in background and update if different
        new Thread(() -> {
            String correctedName = correctUsername(requested);
            String correctedNorm = correctedName.toLowerCase();
            synchronized (targetPlayers) {
                if (!correctedNorm.equals(norm) && !targetPlayers.contains(correctedNorm)) {
                    targetPlayers.remove(norm); // Remove original
                    targetPlayers.add(correctedNorm); // Add corrected
                    saveConfig();
                }
            }
        }, "PQ-AddInclude-Silent").start();
    }

    public static String removeTarget(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Main.CHAT_PREFIX + EnumChatFormatting.RED + "Provide a player name.";
        }
        String norm = name.trim().toLowerCase();
        if (targetPlayers.remove(norm)) {
            saveConfig();
            return Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Removed target: " + name;
        }
        return Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + name + " was not in the list.";
    }

    /** Silent remove for GUI. */
    public static void removeTargetSilent(String name) {
        if (name == null || name.trim().isEmpty()) return;
        String norm = name.trim().toLowerCase();
        if (targetPlayers.remove(norm)) {
            saveConfig();
        }
    }

    public static void clearTargets() {
        targetPlayers.clear();
        saveConfig();
    }

    /** Silent clear for GUI. */
    public static void clearTargetsSilent() {
        targetPlayers.clear();
        saveConfig();
    }

    public static List<String> getExcludeTargets() {
        return new ArrayList<>(excludePlayers);
    }

    public static String addExcludeTarget(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Main.CHAT_PREFIX + EnumChatFormatting.RED + "Provide a player name.";
        }
        final String requested = name.trim();

        new Thread(() -> {
            String correctedName = correctUsername(requested);
            String norm = correctedName.toLowerCase();
            synchronized (excludePlayers) {
                if (!excludePlayers.contains(norm)) {
                    excludePlayers.add(norm);
                    saveConfig();
                }
            }
            try {
                Minecraft.getMinecraft().addScheduledTask(() -> Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Excluded: " + correctedName));
            } catch (Throwable ignored) {}
        }, "PQ-AddExclude").start();

        return Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Adding to exclude...";
    }

    /** Silent exclude add for GUI. */
    public static void addExcludeTargetSilent(String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        final String requested = name.trim();
        
        // Add immediately with original name, then correct in background
        String norm = requested.toLowerCase();
        synchronized (excludePlayers) {
            if (!excludePlayers.contains(norm)) {
                excludePlayers.add(norm);
                saveConfig();
            }
        }
        
        // Correct username in background and update if different
        new Thread(() -> {
            String correctedName = correctUsername(requested);
            String correctedNorm = correctedName.toLowerCase();
            synchronized (excludePlayers) {
                if (!correctedNorm.equals(norm) && !excludePlayers.contains(correctedNorm)) {
                    excludePlayers.remove(norm); // Remove original
                    excludePlayers.add(correctedNorm); // Add corrected
                    saveConfig();
                }
            }
        }, "PQ-AddExclude-Silent").start();
    }

    public static String removeExcludeTarget(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Main.CHAT_PREFIX + EnumChatFormatting.RED + "Provide a player name.";
        }
        String norm = name.trim().toLowerCase();
        if (excludePlayers.remove(norm)) {
            saveConfig();
            return Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Removed from exclude: " + name;
        }
        return Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + name + " was not in the exclude list.";
    }

    /** Silent exclude remove for GUI. */
    public static void removeExcludeTargetSilent(String name) {
        if (name == null || name.trim().isEmpty()) return;
        String norm = name.trim().toLowerCase();
        if (excludePlayers.remove(norm)) {
            saveConfig();
        }
    }

    public static void clearExcludeTargets() {
        excludePlayers.clear();
        saveConfig();
    }

    /** Silent exclude clear for GUI. */
    public static void clearExcludeTargetsSilent() {
        excludePlayers.clear();
        saveConfig();
    }

    public static boolean isRequireAllInclude() { return requireAllInclude; }
    public static boolean isRequireAllExclude() { return requireAllExclude; }

    public static void setRequireAllInclude(boolean value) {
        requireAllInclude = value;
        saveConfig();
        Main.sendMessage(Main.CHAT_PREFIX + (value ? EnumChatFormatting.GREEN + "Include mode: ALL required" : EnumChatFormatting.YELLOW + "Include mode: ANY allowed"));
    }

    /** Silent include mode set for GUI. */
    public static void setRequireAllIncludeSilent(boolean value) {
        requireAllInclude = value;
        saveConfig();
    }

    public static void setRequireAllExclude(boolean value) {
        requireAllExclude = value;
        saveConfig();
        Main.sendMessage(Main.CHAT_PREFIX + (value ? EnumChatFormatting.GREEN + "Exclude mode: ALL required" : EnumChatFormatting.YELLOW + "Exclude mode: ANY allowed"));
    }

    /** Silent exclude mode set for GUI. */
    public static void setRequireAllExcludeSilent(boolean value) {
        requireAllExclude = value;
        saveConfig();
    }

    /**
     * Corrects username using Mojang API to handle case sensitivity
     */
    private static String correctUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return username;
        }
        try {
            java.net.URL url = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + username.trim());
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            
            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                com.google.gson.JsonObject json = new com.google.gson.JsonParser().parse(response.toString()).getAsJsonObject();
                if (json.has("name")) {
                    String correctedName = json.get("name").getAsString();
                    System.out.println("[PlayerQueuer] Corrected username: " + username + " -> " + correctedName);
                    return correctedName;
                }
            } else if (responseCode == 204) {
                // Player not found, return original
                System.out.println("[PlayerQueuer] Username not found in Mojang API: " + username);
            }
        } catch (Exception e) {
            System.err.println("[PlayerQueuer] Failed to correct username " + username + ": " + e.getMessage());
        }
        return username; // Return original if API fails
    }

    /**
     * Check if target players are in the TAB list based on current mode (any/all).
     */
    private static boolean areTargetsInTab() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) return false;
            if (targetPlayers.isEmpty()) return true; // No targets to find = condition satisfied
            
            Set<String> foundPlayers = new HashSet<>();
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                String base = info.getGameProfile() != null ? info.getGameProfile().getName() : null;
                if (base == null || base.isEmpty()) continue;
                String lower = base.toLowerCase();
                if (targetPlayers.contains(lower)) {
                    foundPlayers.add(lower);
                }
            }
            
            if (requireAllInclude) {
                // Require ALL players to be present
                return foundPlayers.size() == targetPlayers.size();
            } else {
                // Require ANY player to be present
                return !foundPlayers.isEmpty();
            }
        } catch (Exception ignored) {}
        return false;
    }

    private static boolean isExcludedConditionMet() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.getNetHandler() == null) return false;
            if (excludePlayers.isEmpty()) return false;
            int present = 0;
            for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
                String base = info.getGameProfile() != null ? info.getGameProfile().getName() : null;
                if (base == null || base.isEmpty()) continue;
                if (excludePlayers.contains(base.toLowerCase())) present++;
            }
            if (requireAllExclude) {
                return present > 0 && present == excludePlayers.size();
            } else {
                return present > 0;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Determine if current mode is teams by scanning scoreboard lines for the word "Team".
     */
    private static boolean isTeamGameMode() {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) return false;
            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) return false;
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
            if (objective == null) return false;
            for (Score score : scoreboard.getSortedScores(objective)) {
                ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
                String raw = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());
                if (raw == null) continue;
                String clean = raw.replaceAll("§[0-9A-FK-ORa-fk-or]", "").toLowerCase();
                if (clean.contains("team")) return true;
            }
            String objName = objective.getDisplayName();
            if (objName != null && objName.replaceAll("§[0-9A-FK-ORa-fk-or]", "").toLowerCase().contains("team")) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Subscribe to chat to detect the 2-second start warning and requeue if needed.
     */
    @SubscribeEvent
    public void onChat(Receive event) {
        if (!enabled) return;
        if (!(event.getPacket() instanceof S02PacketChat)) return;
        try {
            S02PacketChat chat = (S02PacketChat) event.getPacket();
            String message = chat.getChatComponent().getUnformattedText();
            if (message == null || message.isEmpty()) return;
            if (message.contains("The game starts in 1 second!")) {
                // Exclusion has priority
                if (isExcludedConditionMet()) {
                    boolean teams = isTeamGameMode();
                    String cmd = teams ? "/play blitz_teams_normal" : "/play blitz_solo_normal";
                    String exMode = requireAllExclude ? "ALL excluded present" : "Some excluded present";
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + exMode + ". Requeuing " + (teams ? "teams" : "solo") + "...");
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.sendChatMessage(cmd);
                    }
                    return;
                }

                if (!areTargetsInTab()) {
                    boolean teams = isTeamGameMode();
                    String cmd = teams ? "/play blitz_teams_normal" : "/play blitz_solo_normal";
                    String mode = requireAllInclude ? "all included targets" : "any included target";
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + mode + " not found in TAB. Requeuing " + (teams ? "teams" : "solo") + "...");
                    if (Minecraft.getMinecraft().thePlayer != null) {
                        Minecraft.getMinecraft().thePlayer.sendChatMessage(cmd);
                    }
                } else {
                    String mode = requireAllInclude ? "All included targets" : "Target";
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + mode + " found in TAB. Staying in this game.");
                }
            }
        } catch (Exception e) {
            System.err.println("[PlayerQueuer] onChat error: " + e.getMessage());
        }
    }
}


