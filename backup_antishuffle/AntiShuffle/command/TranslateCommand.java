package me.ballmc.AntiShuffle.command;

import me.ballmc.AntiShuffle.features.LanguageTranslator;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.weavemc.loader.api.command.Command;

public class TranslateCommand extends Command {
    public TranslateCommand() {
        super("trl");
    }

    @Override
    public void handle(String[] args) {
        if (args.length == 0) {
            sendMessage(EnumChatFormatting.GREEN + "Supported Languages:");
            sendMessage(EnumChatFormatting.YELLOW + "AR - Arabic");
            sendMessage(EnumChatFormatting.YELLOW + "BG - Bulgarian");
            sendMessage(EnumChatFormatting.YELLOW + "ZH-HANS - Chinese (Simplified)");
            sendMessage(EnumChatFormatting.YELLOW + "ZH-HANT - Chinese (Traditional)");
            sendMessage(EnumChatFormatting.YELLOW + "CS - Czech");
            sendMessage(EnumChatFormatting.YELLOW + "DA - Danish");
            sendMessage(EnumChatFormatting.YELLOW + "NL - Dutch");
            sendMessage(EnumChatFormatting.YELLOW + "EN - English");
            sendMessage(EnumChatFormatting.YELLOW + "EN-GB - British English");
            sendMessage(EnumChatFormatting.YELLOW + "EN-US - American English");
            sendMessage(EnumChatFormatting.YELLOW + "ET - Estonian");
            sendMessage(EnumChatFormatting.YELLOW + "FI - Finnish");
            sendMessage(EnumChatFormatting.YELLOW + "FR - French");
            sendMessage(EnumChatFormatting.YELLOW + "DE - German");
            sendMessage(EnumChatFormatting.YELLOW + "EL - Greek");
            sendMessage(EnumChatFormatting.YELLOW + "HU - Hungarian");
            sendMessage(EnumChatFormatting.YELLOW + "ID - Indonesian");
            sendMessage(EnumChatFormatting.YELLOW + "IT - Italian");
            sendMessage(EnumChatFormatting.YELLOW + "JA - Japanese");
            sendMessage(EnumChatFormatting.YELLOW + "KO - Korean");
            sendMessage(EnumChatFormatting.YELLOW + "LV - Latvian");
            sendMessage(EnumChatFormatting.YELLOW + "LT - Lithuanian");
            sendMessage(EnumChatFormatting.YELLOW + "NB - Norwegian Bokmål");
            sendMessage(EnumChatFormatting.YELLOW + "PL - Polish");
            sendMessage(EnumChatFormatting.YELLOW + "PT - Portuguese");
            sendMessage(EnumChatFormatting.YELLOW + "PT-BR - Brazilian Portuguese");
            sendMessage(EnumChatFormatting.YELLOW + "PT-PT - European Portuguese");
            sendMessage(EnumChatFormatting.YELLOW + "RO - Romanian");
            sendMessage(EnumChatFormatting.YELLOW + "RU - Russian");
            sendMessage(EnumChatFormatting.YELLOW + "SK - Slovak");
            sendMessage(EnumChatFormatting.YELLOW + "SL - Slovenian");
            sendMessage(EnumChatFormatting.YELLOW + "ES - Spanish");
            sendMessage(EnumChatFormatting.YELLOW + "SV - Swedish");
            sendMessage(EnumChatFormatting.YELLOW + "TR - Turkish");
            sendMessage(EnumChatFormatting.YELLOW + "UK - Ukrainian");
            return;
        }

        if (args.length < 2) {
            sendMessage(EnumChatFormatting.RED + "Usage: /trl <sentence> <language>");
            return;
        }

        String targetLanguage = args[args.length - 1];
        StringBuilder sentenceBuilder = new StringBuilder();
        for (int i = 0; i < args.length - 1; i++) {
            sentenceBuilder.append(args[i]);
            if (i < args.length - 2) {
                sentenceBuilder.append(" ");
            }
        }
        String sentence = sentenceBuilder.toString();

        System.out.println("Translating: " + sentence + " to " + targetLanguage);

        new Thread(() -> {
            try {
                String translatedText = LanguageTranslator.translateText(sentence, targetLanguage);
                if (translatedText != null) {
                    sendMessage(EnumChatFormatting.YELLOW + translatedText);
                } else {
                    sendMessage(EnumChatFormatting.RED + "Translation failed.");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendMessage(EnumChatFormatting.RED + "An error occurred during translation.");
            }
        }).start();
    }

    private void sendMessage(String message) {
        Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(message));
    }
} 