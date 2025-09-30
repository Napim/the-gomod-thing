package me.ballmc.gomod.command;

import me.ballmc.gomod.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.weavemc.loader.api.command.Command;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Dumps current scoreboard sidebar lines to the Minecraft log file for debugging.
 * Usage: /scoreboarddebug
 */
public class ScoreboardDebugCommand extends Command {

    public ScoreboardDebugCommand() {
        super("scoreboarddebug");
    }

    @Override
    public void handle(String[] args) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc == null || mc.theWorld == null) {
                Main.sendMessage(Main.CHAT_PREFIX + "" + "No world loaded. Unable to read scoreboard.");
                return;
            }

            Scoreboard scoreboard = mc.theWorld.getScoreboard();
            if (scoreboard == null) {
                Main.sendMessage(Main.CHAT_PREFIX + "" + "No scoreboard available.");
                return;
            }

            // Sidebar objective (1)
            ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
            if (objective == null) {
                Main.sendMessage(Main.CHAT_PREFIX + "" + "No sidebar objective in the scoreboard.");
                return;
            }

            String title = objective.getDisplayName();

            // Collect scores
            Collection<Score> allScores = scoreboard.getSortedScores(objective);
            List<Score> visible = new ArrayList<>();
            for (Score s : allScores) {
                if (s == null) continue;
                // Filter out invalid or hidden entries
                String playerName = s.getPlayerName();
                if (playerName == null || playerName.startsWith("#")) continue;
                visible.add(s);
            }

            // Sort descending by points (higher first)
            visible.sort(Comparator.comparingInt(Score::getScorePoints).reversed());

            // Support optional "all" flag to dump all entries
            boolean dumpAll = args != null && args.length > 0 && "all".equalsIgnoreCase(args[0]);

            List<String> lines = new ArrayList<>();
            if (dumpAll) {
                for (Score s : visible) {
                    ScorePlayerTeam team = scoreboard.getPlayersTeam(s.getPlayerName());
                    String entry = ScorePlayerTeam.formatPlayerName(team, s.getPlayerName());
                    lines.add(entry + " : " + s.getScorePoints());
                }
            } else {
                // Vanilla sidebar renders up to 15 lines: take top 15
                int limit = Math.min(15, visible.size());
                for (int i = 0; i < limit; i++) {
                    Score s = visible.get(i);
                    ScorePlayerTeam team = scoreboard.getPlayersTeam(s.getPlayerName());
                    String entry = ScorePlayerTeam.formatPlayerName(team, s.getPlayerName());
                    lines.add(entry + " : " + s.getScorePoints());
                }
            }

            // Log to stdout (ends up in latest.log)
            System.out.println("[gomod][ScoreboardDebug] ===== Sidebar Dump =====");
            System.out.println("[gomod][ScoreboardDebug] Title: " + title);
            if (lines.isEmpty()) {
                System.out.println("[gomod][ScoreboardDebug] (no lines)");
            } else {
                for (int i = 0; i < lines.size(); i++) {
                    System.out.println("[gomod][ScoreboardDebug] " + (i + 1) + ". " + lines.get(i));
                }
            }
            System.out.println("[gomod][ScoreboardDebug] =========================");

            Main.sendMessage(Main.CHAT_PREFIX + "" + (dumpAll ? "Scoreboard (all entries) dumped to logs." : "Scoreboard sidebar dumped to logs (15 max)."));
        } catch (Throwable t) {
            System.out.println("[gomod][ScoreboardDebug] Error dumping scoreboard: " + t.getMessage());
            Main.sendMessage(Main.CHAT_PREFIX + "" + "Failed to dump scoreboard. See logs for details.");
        }
    }
}


