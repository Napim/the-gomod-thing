package me.ballmc.gomod.command;

import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.PlayerQueuer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

/**
 * Command for managing PlayerQueuer.
 * Usage:
 * - /playerqueuer enable|disable
 * - /playerqueuer add <name>
 * - /playerqueuer remove <name>
 * - /playerqueuer list
 * - /playerqueuer gui
 */
public class PlayerQueuerCommand extends Command {

    public PlayerQueuerCommand() {
        super("playerqueuer");
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }
        String sub = args[0].toLowerCase();
        switch (sub) {
            case "enable":
                PlayerQueuer.setEnabled(true);
                break;
            case "disable":
                PlayerQueuer.setEnabled(false);
                break;
            case "add":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer add <name>");
                } else {
                    Main.sendMessage(PlayerQueuer.addTarget(args[1]));
                }
                break;
            case "remove":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer remove <name>");
                } else {
                    Main.sendMessage(PlayerQueuer.removeTarget(args[1]));
                }
                break;
            case "list":
                java.util.List<String> list = PlayerQueuer.getTargets();
                if (list.isEmpty()) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "No target players added.");
                } else {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.AQUA + "Targets: " + EnumChatFormatting.WHITE + String.join(", ", list));
                }
                break;
            case "excludelist":
                java.util.List<String> exlist = PlayerQueuer.getExcludeTargets();
                if (exlist.isEmpty()) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "No excluded players.");
                } else {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.AQUA + "Excluded: " + EnumChatFormatting.WHITE + String.join(", ", exlist));
                }
                break;
            case "excludeadd":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer excludeadd <name>");
                } else {
                    Main.sendMessage(PlayerQueuer.addExcludeTarget(args[1]));
                }
                break;
            case "excluderemove":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer excluderemove <name>");
                } else {
                    Main.sendMessage(PlayerQueuer.removeExcludeTarget(args[1]));
                }
                break;
            case "excludeclear":
                PlayerQueuer.clearExcludeTargets();
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Cleared all excluded players.");
                break;
            case "mode":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer mode <any|all>");
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Include mode: " + (PlayerQueuer.isRequireAllInclude() ? "ALL" : "ANY"));
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Exclude mode: " + (PlayerQueuer.isRequireAllExclude() ? "ALL" : "ANY"));
                } else {
                    String mode = args[1].toLowerCase();
                    if (mode.equals("any")) {
                        PlayerQueuer.setRequireAllInclude(false);
                    } else if (mode.equals("all")) {
                        PlayerQueuer.setRequireAllInclude(true);
                    } else {
                        Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Mode must be 'any' or 'all'");
                    }
                }
                break;
            case "modeinclude":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer modeinclude <any|all>");
                } else {
                    String mode = args[1].toLowerCase();
                    if (mode.equals("any")) PlayerQueuer.setRequireAllInclude(false);
                    else if (mode.equals("all")) PlayerQueuer.setRequireAllInclude(true);
                    else Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Mode must be 'any' or 'all'");
                }
                break;
            case "modeexclude":
                if (args.length < 2) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /playerqueuer modeexclude <any|all>");
                } else {
                    String mode = args[1].toLowerCase();
                    if (mode.equals("any")) PlayerQueuer.setRequireAllExclude(false);
                    else if (mode.equals("all")) PlayerQueuer.setRequireAllExclude(true);
                    else Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Mode must be 'any' or 'all'");
                }
                break;
            case "gui":
                try {
                    if (Minecraft.getMinecraft() != null) {
                        Minecraft.getMinecraft().displayGuiScreen(new me.ballmc.gomod.gui.PlayerQueuerGUI(null));
                    }
                } catch (Throwable t) {
                    Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Unable to open GUI: " + t.getMessage());
                }
                break;
            default:
                showHelp();
        }
    }

    private void showHelp() {
        Main.sendMessage(EnumChatFormatting.GOLD + "=== Player Queuer ===");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer enable|disable" + EnumChatFormatting.WHITE + " - Toggle feature");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer add <name>" + EnumChatFormatting.WHITE + " - Add target player");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer remove <name>" + EnumChatFormatting.WHITE + " - Remove target player");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer list" + EnumChatFormatting.WHITE + " - List targets");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer excludeadd <name>" + EnumChatFormatting.WHITE + " - Add excluded player");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer excluderemove <name>" + EnumChatFormatting.WHITE + " - Remove excluded player");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer excludelist" + EnumChatFormatting.WHITE + " - List excluded players");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer excludeclear" + EnumChatFormatting.WHITE + " - Clear excluded players");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer mode <any|all>" + EnumChatFormatting.WHITE + " - Set include mode (shortcut)");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer modeinclude <any|all>" + EnumChatFormatting.WHITE + " - Set include mode");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer modeexclude <any|all>" + EnumChatFormatting.WHITE + " - Set exclude mode");
        Main.sendMessage(EnumChatFormatting.YELLOW + "/playerqueuer gui" + EnumChatFormatting.WHITE + " - Open GUI");
    }
}


