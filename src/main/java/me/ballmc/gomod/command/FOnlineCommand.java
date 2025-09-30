package me.ballmc.gomod.command;

import me.ballmc.gomod.Main;
import me.ballmc.gomod.features.FriendListScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

/**
 * Command to scan friends' statuses via /fl pages and summarize Blitz/Away/Busy.
 * Usage: /fonline
 */
public class FOnlineCommand extends Command {

	public FOnlineCommand() {
		super("fonline");
	}

	@Override
	public void handle(String[] args) {
		// Kick off a scan window and send /fl 1..6 with 0.2s delay between each
		if (Minecraft.getMinecraft().thePlayer == null) {
			Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Player not initialized yet.");
			return;
		}

		boolean debug = args != null && args.length > 0 && "debug".equalsIgnoreCase(args[0]);
		FriendListScanner.setDebug(debug);
		Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + (debug ? "[debug] " : "") + "Scanning friend list pages (1-6)...");
		
		new Thread(() -> {
			try {
				// Start collection before sending any commands
				FriendListScanner.startCollection(8000); // collect chat for ~8 seconds total
				
				for (int i = 1; i <= 6; i++) {
					Minecraft.getMinecraft().thePlayer.sendChatMessage("/fl " + i);
					Thread.sleep(200); // 0.2 seconds between pages
				}
				// Ensure we finish shortly after the last page likely arrives
				FriendListScanner.finishAfterDelay(2000);
			} catch (InterruptedException ignored) {}
		}).start();
	}
}


