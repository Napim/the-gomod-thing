# Inventory HUD Feature

## Overview
The Inventory HUD feature displays your Minecraft inventory in a corner of the screen without needing to open the inventory GUI. This allows you to quickly check your items while playing.

## Commands

### Basic Usage
- `/invhud` - Toggle the inventory HUD on/off

### Advanced Commands
- `/invhud position <0-3>` - Set the position of the HUD
  - `0` = Top-left
  - `1` = Top-right  
  - `2` = Bottom-left
  - `3` = Bottom-right

- `/invhud scale <1-4>` - Set the scale factor of the HUD
  - `1` = Small
  - `2` = Medium (default)
  - `3` = Large
  - `4` = Extra Large

- `/invhud opacity <0-255>` - Set background opacity
  - `0` = Completely transparent
  - `128` = Half transparent (default)
  - `255` = Completely opaque

- `/invhud status` - Show current settings

## Features

### Visual Display
- Shows all 27 slots of your main inventory (3 rows × 9 columns)
- Displays item icons with proper rendering
- Shows stack counts for items with quantity > 1
- Semi-transparent background for better visibility
- Configurable position, scale, and background opacity
- **Smart visibility**: Only displays when inventory contains items (empty inventories are hidden)

### Configuration
- Settings are automatically saved between game sessions
- Position and scale preferences are remembered
- Toggle state is preserved

### Performance
- Lightweight rendering that doesn't impact game performance
- Only renders when enabled and player is in-game
- Efficient GL state management

## Technical Details

### Files Added
- `src/main/java/me/ballmc/AntiShuffle/features/InventoryHUD.java` - Main feature class
- `src/main/java/me/ballmc/AntiShuffle/command/InventoryHUDCommand.java` - Command handler
- `src/main/java/me/ballmc/AntiShuffle/mixins/GuiIngameMixin.java` - Rendering mixin

### Files Modified
- `src/main/java/me/ballmc/AntiShuffle/Main.java` - Added command registration and settings loading
- `src/main/java/me/ballmc/AntiShuffle/features/ConfigManager.java` - Added HUD configuration methods
- `src/main/resources/ballmc.mixins.json` - Added new mixin

### Compatibility
- Minecraft 1.8.9
- WeaveLoader
- Compatible with existing AntiShuffle mod features

## Usage Examples

1. **Enable the HUD**: `/invhud`
2. **Move to top-right**: `/invhud position 1`
3. **Make it larger**: `/invhud scale 3`
4. **Make background transparent**: `/invhud opacity 50`
5. **Check settings**: `/invhud status`
6. **Disable**: `/invhud`

The inventory HUD will display your items in real-time, updating whenever your inventory changes.
