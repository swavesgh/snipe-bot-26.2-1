package com.snipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ContainerInput;

/**
 * All the Minecraft calls most likely to differ between 26.x builds are in this one file.
 * If the build fails, the error will almost certainly point at one of these lines.
 */
public class Compat {
    /** Currently open screen. Fallback if this fails: mc.screen */
    public static Screen screen(Minecraft mc) { return mc.gui.screen(); }

    /** Open a screen. Fallback if this fails: mc.setScreenAndShow(s) or mc.setScreen(s) */
    public static void open(Minecraft mc, Screen s) { mc.gui.setScreen(s); }

    /** Run a command without the slash. */
    public static void command(Minecraft mc, String cmd) { mc.getConnection().sendCommand(cmd); }

    /** Chat message only you can see. */
    public static void say(Minecraft mc, String s) { mc.player.sendSystemMessage(Component.literal(s)); }

    /** Left-click a slot. Fallback if this fails: mc.gameMode.handleInventoryMouseClick(id, slot, 0, ClickType.PICKUP, player) */
    public static void click(Minecraft mc, ChestMenu menu, int slot) {
        mc.gameMode.handleContainerInput(menu.containerId, slot, 0, ContainerInput.PICKUP, mc.player);
    }
}
