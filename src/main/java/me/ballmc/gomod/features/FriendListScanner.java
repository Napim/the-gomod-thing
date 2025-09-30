package me.ballmc.gomod.features;

import me.ballmc.gomod.Main;
import net.minecraft.network.play.server.S02PacketChat;
import net.weavemc.loader.api.event.PacketEvent.Receive;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.minecraft.util.EnumChatFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses friend list chat output to summarize players by status.
 * Activated by FOnlineCommand.startCollection() and automatically stops after a timeout.
 */
public class FriendListScanner {

	private static volatile boolean collecting = false;
	private static volatile long endAtMs = 0L;
	private static volatile boolean debug = false;

	private static final ConcurrentMap<String, String> blitz = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, String> away = new ConcurrentHashMap<>();
	private static final ConcurrentMap<String, String> busy = new ConcurrentHashMap<>();

	// Regex patterns tolerant of ranks and text variants
	private static final Pattern BLITZ_PATTERN = Pattern.compile(
			"(?i).*?\\b(?:\\[[^\\]]+\\]\\s*)?([A-Za-z0-9_]{1,16})\\s+is\\s+(?:in|playing)\\s+(?:a\\s+)?Blitz(?:\\s+Survival\\s+Games)?(?:\\s+Game)?\\b"
	);

	private static final Pattern STATUS_PATTERN = Pattern.compile(
			"(?i).*?\\b(?:\\[[^\\]]+\\]\\s*)?([A-Za-z0-9_]{1,16})\\s+is\\s+currently\\s+(Away|Busy|Idle|AFK)\\b"
	);

	public static void startCollection(long windowMs) {
		collecting = true;
		endAtMs = System.currentTimeMillis() + Math.max(1000, windowMs);
		blitz.clear();
		away.clear();
		busy.clear();
	}

	public static void finishAfterDelay(long delayMs) {
		new Thread(() -> {
			try {
				Thread.sleep(Math.max(200, delayMs));
			} catch (InterruptedException ignored) {}
			forceFinish();
		}).start();
	}

	public static void setDebug(boolean enabled) {
		debug = enabled;
	}

	private static void finishIfDue() {
		if (!collecting) return;
		if (System.currentTimeMillis() < endAtMs) return;
		collecting = false;
		report();
	}

	private static void forceFinish() {
		if (!collecting) return;
		collecting = false;
		report();
	}

	@SubscribeEvent
	public void onChat(Receive event) {
		if (!collecting) return;
		if (!(event.getPacket() instanceof S02PacketChat)) return;
		S02PacketChat pkt = (S02PacketChat) event.getPacket();
		String raw = pkt.getChatComponent().getFormattedText();
		if (raw == null || raw.isEmpty()) return;

		// Hypixel friend list includes colors/ranks; sanitize then regex-match
		String text = sanitize(raw);
		if (debug) {
			DebugBuffer.log("raw:", raw);
			DebugBuffer.log("san:", text);
			System.out.println("[FOnline][raw] " + raw);
			System.out.println("[FOnline][san] " + text);
		}
		parseLine(text);

		// Auto-finish when time window passes
		if (System.currentTimeMillis() >= endAtMs) {
			finishIfDue();
		}
	}

	private static void parseLine(String line) {
		Matcher mBlitz = BLITZ_PATTERN.matcher(line);
		if (mBlitz.find()) {
			String name = mBlitz.group(1);
			if (name != null && !name.isEmpty()) {
				blitz.put(name, "Blitz Survival Games");
				if (debug) System.out.println("[FOnline][match] blitz:" + name);
			}
			return;
		}

		Matcher mStatus = STATUS_PATTERN.matcher(line);
		if (mStatus.find()) {
			String name = mStatus.group(1);
			String status = mStatus.group(2);
			if (name != null && !name.isEmpty() && status != null) {
				if (equalsIgnoreCase(status, "Away") || equalsIgnoreCase(status, "Idle") || equalsIgnoreCase(status, "AFK")) {
					away.put(name, "Away");
					if (debug) System.out.println("[FOnline][match] away:" + name + " (" + status + ")");
				} else if (equalsIgnoreCase(status, "Busy")) {
					busy.put(name, "Busy");
					if (debug) System.out.println("[FOnline][match] busy:" + name + ")");
				}
			}
		}
	}

	private static void report() {
		List<String> blitzNames = new ArrayList<>(blitz.keySet());
		List<String> awayNames = new ArrayList<>(away.keySet());
		List<String> busyNames = new ArrayList<>(busy.keySet());
		Collections.sort(blitzNames, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(awayNames, String.CASE_INSENSITIVE_ORDER);
		Collections.sort(busyNames, String.CASE_INSENSITIVE_ORDER);

		boolean any = false;
		if (!blitzNames.isEmpty()) {
			any = true;
			for (String name : blitzNames) {
				Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + name + " is in a Blitz Survival Games Game");
			}
		}
		if (!awayNames.isEmpty()) {
			any = true;
			for (String name : awayNames) {
				Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + name + " is currently Away");
			}
		}
		if (!busyNames.isEmpty()) {
			any = true;
			for (String name : busyNames) {
				Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + name + " is currently Busy");
			}
		}

		if (!any) {
			Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "No friends found in Blitz/Away/Busy.");
		}

		if (debug) {
			Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "[debug] collected: blitz=" + blitzNames.size() + ", away=" + awayNames.size() + ", busy=" + busyNames.size());
			DebugBuffer.flushToChat();
			System.out.println("[FOnline][summary] blitz=" + blitzNames.size() + ", away=" + awayNames.size() + ", busy=" + busyNames.size());
		}
	}

	private static int indexOfIgnoreCase(String haystack, String needle) {
		return haystack.toLowerCase().indexOf(needle.toLowerCase());
	}

	private static boolean startsWithIgnoreCase(String text, String prefix) {
		return text.regionMatches(true, 0, prefix, 0, prefix.length());
	}

	private static boolean equalsIgnoreCase(String a, String b) {
		return a != null && a.equalsIgnoreCase(b);
	}

	private static String sanitize(String s) {
		// Strip Minecraft color codes and weird section symbols
		String noColors = s.replaceAll("§[0-9A-FK-ORa-fk-or]", "");
		// Collapse whitespace and trim
		return noColors.replaceAll("\\s+", " ").trim();
	}

	// Simple ring buffer for debug lines
	private static class DebugBuffer {
		private static final int MAX = 12;
		private static final List<String> LINES = new ArrayList<>();
		static void log(String tag, String line) {
			if (!debug) return;
			String entry = tag + " " + (line.length() > 220 ? line.substring(0, 220) + "..." : line);
			if (LINES.size() >= MAX) {
				LINES.remove(0);
			}
			LINES.add(entry);
		}
		static void flushToChat() {
			if (!debug) return;
			for (String l : LINES) {
				Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.YELLOW + "[debug] " + l);
			}
			LINES.clear();
		}
	}
}


