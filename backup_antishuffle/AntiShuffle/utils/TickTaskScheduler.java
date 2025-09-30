package me.ballmc.AntiShuffle.utils;

import net.minecraft.client.Minecraft;
import net.weavemc.loader.api.event.SubscribeEvent;
import net.weavemc.loader.api.event.TickEvent;
import net.weavemc.loader.api.event.EventBus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility class for scheduling tasks to run after a specific number of ticks.
 * This is useful for spacing out operations that would otherwise cause lag or
 * rate limiting if done all at once.
 */
public class TickTaskScheduler {
    // Singleton instance
    private static TickTaskScheduler instance;
    
    // Task storage: Map of task ID to task and remaining ticks
    private final Map<Integer, TaskEntry> scheduledTasks = new ConcurrentHashMap<>();
    
    // Task ID counter
    private final AtomicInteger taskIdCounter = new AtomicInteger(0);
    
    // Whether the scheduler has been initialized
    private boolean initialized = false;
    
    /**
     * Private constructor to enforce singleton pattern
     */
    private TickTaskScheduler() {
        // Register for tick events
        EventBus.subscribe(this);
        initialized = true;
        System.out.println("[TickTaskScheduler] Initialized");
    }
    
    /**
     * Get the singleton instance
     */
    public static TickTaskScheduler getInstance() {
        if (instance == null) {
            instance = new TickTaskScheduler();
        }
        return instance;
    }
    
    /**
     * Schedule a task to run after a specified number of ticks
     * 
     * @param task The task to run
     * @param delayTicks The number of ticks to wait before running the task
     * @return The task ID, which can be used to cancel the task
     */
    public int scheduleTask(Runnable task, int delayTicks) {
        if (task == null) return -1;
        
        // Ensure at least 1 tick delay
        int delay = Math.max(1, delayTicks);
        
        // Generate task ID
        int taskId = taskIdCounter.incrementAndGet();
        
        // Store the task
        scheduledTasks.put(taskId, new TaskEntry(task, delay));
        
        return taskId;
    }
    
    /**
     * Cancel a scheduled task
     * 
     * @param taskId The ID of the task to cancel
     * @return true if the task was cancelled, false if it was not found
     */
    public boolean cancelTask(int taskId) {
        return scheduledTasks.remove(taskId) != null;
    }
    
    /**
     * Handle tick events to run scheduled tasks
     */
    @SubscribeEvent
    public void onTick(TickEvent event) {
        if (!initialized || Minecraft.getMinecraft() == null || scheduledTasks.isEmpty()) {
            return;
        }
        
        // Iterate through tasks and decrease their counters
        scheduledTasks.entrySet().removeIf(entry -> {
            TaskEntry taskEntry = entry.getValue();
            taskEntry.remainingTicks--;
            
            if (taskEntry.remainingTicks <= 0) {
                // Time to run the task
                try {
                    taskEntry.task.run();
                } catch (Exception e) {
                    System.err.println("[TickTaskScheduler] Error running task: " + e.getMessage());
                }
                return true; // Remove the task
            }
            
            return false; // Keep the task
        });
    }
    
    /**
     * Clear all scheduled tasks
     */
    public void clearAllTasks() {
        scheduledTasks.clear();
    }
    
    /**
     * Entry that stores a task and its remaining ticks
     */
    private static class TaskEntry {
        private final Runnable task;
        private int remainingTicks;
        
        public TaskEntry(Runnable task, int remainingTicks) {
            this.task = task;
            this.remainingTicks = remainingTicks;
        }
    }
} 