package me.ballmc.AntiShuffle;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.weavemc.loader.api.ModInitializer;
import net.weavemc.loader.api.event.EventBus;
import me.ballmc.AntiShuffle.features.KillCounter;

public class AntiShuffle implements ModInitializer {
    private final KillCounter killCounter = new KillCounter();
    
    @Override
    public void preInit() {
        System.out.println("Initializing AntiShuffle!");
        
        // Register kill counter
        EventBus.subscribe(killCounter);
        
        // Register kill effect textures
        registerKillEffectTextures();
    }

    private void registerKillEffectTextures() {
        String[] effects = {
            "regeneration",
            "speed", 
            "resistance",
            "gravedigger",
            "random",
            "level_up",
            "rapid_fire"
        };

        for (String effect : effects) {
            ResourceLocation location = new ResourceLocation("antishuffle", "textures/gui/effects/" + effect + ".png");
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(location);
            } catch (Exception e) {
                System.out.println("Failed to load kill effect texture: " + effect);
                e.printStackTrace();
            }
        }
    }
} 