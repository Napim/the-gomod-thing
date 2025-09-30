package me.ballmc.gomod.command;

import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.ConfigManager;
import me.ballmc.gomod.features.GlobalChatClient;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

import static me.ballmc.gomod.Main.sendMessage;

/**
 * Command to control cross-server global chat.
 */
public class GlobalChatCommand extends Command {
    private final GlobalChatClient client;

    public GlobalChatCommand(GlobalChatClient client) {
        super("gchat");
        this.client = client;
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            showHelp();
            return;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "on":
            case "enable":
                ConfigManager.setGlobalChatEnabled(true);
                client.start();
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Global chat enabled.");
                break;
            case "off":
            case "disable":
                ConfigManager.setGlobalChatEnabled(false);
                client.stop();
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Global chat disabled.");
                break;
            case "send":
                if (args.length < 2) {
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Usage: /gchat send <message>");
                    return;
                }
                if (!ConfigManager.isGlobalChatEnabled()) {
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Enable global chat first with /gchat on");
                    return;
                }
                String msg = join(args, 1);
                client.send(msg);
                break;
            case "url":
                if (args.length < 2) {
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Current endpoint: " + ConfigManager.getGlobalChatEndpoint());
                    return;
                }
                ConfigManager.setGlobalChatEndpoint(args[1]);
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Endpoint updated.");
                break;
            case "channel":
                if (args.length < 2) {
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Current channel: " + ConfigManager.getGlobalChatChannel());
                    return;
                }
                ConfigManager.setGlobalChatChannel(args[1]);
                sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Channel set to '" + args[1] + "'.");
                break;
            case "poll":
                if (args.length < 2) {
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Current poll seconds: " + ConfigManager.getGlobalChatPollSeconds());
                    return;
                }
                try {
                    int sec = Integer.parseInt(args[1]);
                    ConfigManager.setGlobalChatPollSeconds(sec);
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Poll interval set to " + ConfigManager.getGlobalChatPollSeconds() + "s.");
                } catch (NumberFormatException e) {
                    sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Invalid number: " + args[1]);
                }
                break;
            case "help":
            default:
                showHelp();
        }
    }

    private void showHelp() {
        sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "Global Chat Commands:");
        sendMessage(EnumChatFormatting.YELLOW + "  /gchat on|off" + EnumChatFormatting.WHITE + " - Enable or disable global chat");
        sendMessage(EnumChatFormatting.YELLOW + "  /gchat send <message>" + EnumChatFormatting.WHITE + " - Send a message to global chat");
        sendMessage(EnumChatFormatting.YELLOW + "  /gchat url [endpoint]" + EnumChatFormatting.WHITE + " - Show or set relay endpoint");
        sendMessage(EnumChatFormatting.YELLOW + "  /gchat channel [name]" + EnumChatFormatting.WHITE + " - Show or set channel");
        sendMessage(EnumChatFormatting.YELLOW + "  /gchat poll [seconds]" + EnumChatFormatting.WHITE + " - Show or set poll interval");
    }

    private String join(String[] arr, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < arr.length; i++) {
            if (i > start) sb.append(' ');
            sb.append(arr[i]);
        }
        return sb.toString();
    }
}


