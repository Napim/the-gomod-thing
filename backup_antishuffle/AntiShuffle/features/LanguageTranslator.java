package me.ballmc.AntiShuffle.features;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class LanguageTranslator {

    public static String detectLanguage(String text) throws Exception {
        // DeepL API does not provide a separate language detection endpoint, so this method might not be needed.
        // If DeepL supports detection, adjust the endpoint accordingly.
        return null; // Placeholder for detection logic if needed
    }

    public static String translateText(String text, String targetLanguage) throws Exception {
        // Check if API key is set
        String apiKey = ApiKeyManager.getApiKey("translation");
        if (apiKey == null || apiKey.isEmpty()) {
            return "Translation error: No API key configured. Use /gmapi translation YOUR_API_KEY to set one.";
        }
        
        String urlString = "https://api-free.deepl.com/v2/translate";
        String urlParameters = "auth_key=" + URLEncoder.encode(apiKey, "UTF-8") +
                               "&text=" + URLEncoder.encode(text, "UTF-8") +
                               "&target_lang=" + URLEncoder.encode(targetLanguage, "UTF-8");

        HttpURLConnection conn = null;
        BufferedReader reader = null;
        
        try {
            URL url = new URL(urlString);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(urlParameters.length()));
            conn.setDoOutput(true);
            conn.setDoInput(true);

            // Send the request
            try (OutputStream os = conn.getOutputStream()) {
                os.write(urlParameters.getBytes("UTF-8"));
                os.flush();
            }

            // Check the response code
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                System.out.println("Translation API Error: " + responseCode);
                
                // Check if it's an authentication error
                if (responseCode == 401 || responseCode == 403) {
                    return "Translation error: Invalid API key. Please get a valid key from DeepL and set it with /gmapi translation YOUR_API_KEY";
                }
                
                return "Translation error: " + responseCode;
            }

            // Read the response
            StringBuilder response = new StringBuilder();
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            // Parse the JSON response
            JsonParser parser = new JsonParser();
            JsonObject jsonResponse = parser.parse(response.toString()).getAsJsonObject();
            
            if (jsonResponse.has("translations") && jsonResponse.getAsJsonArray("translations").size() > 0) {
                String translatedText = jsonResponse.getAsJsonArray("translations")
                                                  .get(0).getAsJsonObject()
                                                  .get("text").getAsString();
                return translatedText;
            } else {
                return "Translation failed: No translation available";
            }
        } catch (Exception e) {
            System.out.println("Translation error: " + e.getMessage());
            return "Translation failed: " + e.getMessage();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    // Ignore
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
} 