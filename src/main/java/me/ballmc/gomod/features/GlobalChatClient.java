package me.ballmc.gomod.features;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.ballmc.gomod.Main;
import net.minecraft.client.Minecraft;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.event.EventBus;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.TickEvent;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Cross-server chat client. Polls a relay endpoint and posts messages.
 * Keeps lightweight dependencies and mirrors existing HttpURLConnection usage.
 */
public class GlobalChatClient {
    private static final Gson GSON = new Gson();

    private volatile long lastMessageTs = 0L;
    private final AtomicBoolean subscribed = new AtomicBoolean(false);
    private int tickCounter = 0;

    public void start() {
        if (subscribed.compareAndSet(false, true)) {
            EventBus.subscribe(this);
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.GREEN + "Global chat active on channel '" + ConfigManager.getGlobalChatChannel() + "'.");
        }
    }

    public void stop() {
        if (subscribed.compareAndSet(true, false)) {
            EventBus.unsubscribe(this);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!ConfigManager.isGlobalChatEnabled()) return;
        if (Minecraft.getMinecraft().thePlayer == null) return;

        tickCounter++;
        int pollEvery = Math.max(1, ConfigManager.getGlobalChatPollSeconds()) * 20; // ticks
        if (tickCounter % pollEvery != 0) return;

        try {
            fetchNewMessages();
        } catch (Exception ignored) {}
    }

    public void send(String message) {
        if (Minecraft.getMinecraft().thePlayer == null) return;
        String player = Minecraft.getMinecraft().thePlayer.getName();
        String server = Minecraft.getMinecraft().getCurrentServerData() != null ?
                Minecraft.getMinecraft().getCurrentServerData().serverIP : "singleplayer";
        String channel = ConfigManager.getGlobalChatChannel();
        String endpoint = ConfigManager.getGlobalChatEndpoint();

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "send");
            payload.addProperty("channel", channel);
            payload.addProperty("sender", player);
            payload.addProperty("server", server);
            payload.addProperty("message", message);

            post(endpoint, payload);
        } catch (Exception e) {
            Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.RED + "Failed to send global chat message.");
        }
    }

    private void fetchNewMessages() {
        String endpoint = ConfigManager.getGlobalChatEndpoint();
        String channel = ConfigManager.getGlobalChatChannel();

        try {
            JsonObject payload = new JsonObject();
            payload.addProperty("type", "poll");
            payload.addProperty("channel", channel);
            payload.addProperty("since", lastMessageTs);

            String response = post(endpoint, payload);
            if (response == null || response.isEmpty()) return;

            JsonObject obj = GSON.fromJson(response, JsonObject.class);
            if (obj == null || !obj.has("messages")) return;
            JsonArray arr = obj.getAsJsonArray("messages");
            for (int i = 0; i < arr.size(); i++) {
                JsonObject m = arr.get(i).getAsJsonObject();
                long ts = m.has("ts") ? m.get("ts").getAsLong() : System.currentTimeMillis();
                String sender = safeGet(m, "sender");
                String server = safeGet(m, "server");
                String msg = safeGet(m, "message");
                if (ts > lastMessageTs) lastMessageTs = ts;

                // Display nicely
                Main.sendMessage(Main.CHAT_PREFIX + EnumChatFormatting.AQUA + "[GC] " + EnumChatFormatting.YELLOW + sender +
                        EnumChatFormatting.GRAY + "@" + server + EnumChatFormatting.WHITE + ": " + msg);
            }
        } catch (Exception ignored) {}
    }

    private String safeGet(JsonObject m, String key) {
        return m != null && m.has(key) && !m.get(key).isJsonNull() ? m.get(key).getAsString() : "";
        
    }

    private String post(String urlStr, JsonObject payload) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        conn.setDoOutput(true);

        byte[] body = payload.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body);
        }

        int code = conn.getResponseCode();
        if (code >= 200 && code < 300) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                return sb.toString();
            }
        }
        return null;
    }
}


