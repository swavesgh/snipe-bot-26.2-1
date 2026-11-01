package com.snipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;
import java.util.regex.*;

public class Sniper {
    enum State { OPEN, SCAN, CONFIRM, WAIT }
    public static boolean running = false;
    public static boolean buy = true;   // false = track prices only
    static State state = State.OPEN;
    static int cooldown, wait, page;
    static Map<String, Double> scanLow = new HashMap<>();

    public static void start(boolean buyMode) { buy = buyMode; running = true; state = State.OPEN; cooldown = 0; msg(buyMode ? "Sniping started" : "Tracking started"); }
    public static void stop() { running = false; msg("Stopped"); }

    static void msg(String s) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) Compat.say(mc, "[Sniper] " + s);
    }

    static double listingPrice(ItemStack st) {
        ItemLore lore = st.getOrDefault(DataComponents.LORE, ItemLore.EMPTY);
        Pattern p = Pattern.compile(Config.I.priceRegex);
        for (Component line : lore.lines()) {
            Matcher m = p.matcher(line.getString());
            if (m.find()) {
                try {
                    double v = Double.parseDouble(m.group(1).replace(",", "")) * Config.suffix(m.groupCount() >= 2 && m.group(2) != null ? m.group(2) : "");
                    return Config.I.pricePerItem ? v / Math.max(1, st.getCount()) : v;
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    static int findButton(ChestMenu h, String contains) {
        int size = h.getRowCount() * 9;
        for (int i = 0; i < size; i++) {
            ItemStack s = h.getSlot(i).getItem();
            if (!s.isEmpty() && s.getHoverName().getString().toLowerCase().contains(contains.toLowerCase())) return i;
        }
        return -1;
    }

    static void click(Minecraft mc, ChestMenu h, int slot) {
        Compat.click(mc, h, slot);
        cooldown = Config.I.delayTicks;
    }

    public static void tick(Minecraft mc) {
        if (!running || mc.player == null || mc.getConnection() == null) return;
        if (cooldown > 0) { cooldown--; return; }

        switch (state) {
            case OPEN -> {
                scanLow.clear(); page = 0; wait = 0;
                Compat.command(mc, Config.I.command);
                state = State.SCAN; cooldown = Config.I.delayTicks * 2;
            }
            case SCAN -> {
                if (!(Compat.screen(mc) instanceof ContainerScreen gs)) {
                    if (++wait > 80) { state = State.OPEN; }
                    return;
                }
                wait = 0;
                ChestMenu h = gs.getMenu();
                int size = h.getRowCount() * 9;
                for (int i = 0; i < size; i++) {
                    ItemStack st = h.getSlot(i).getItem();
                    if (st.isEmpty()) continue;
                    double unit = listingPrice(st);
                    if (unit < 0) continue;
                    String name = st.getHoverName().getString();
                    scanLow.merge(name, unit, Math::min);
                    if (!buy) continue;
                    for (Config.Target t : Config.I.targets) {
                        if (name.toLowerCase().contains(t.name.toLowerCase()) && unit <= t.maxPrice) {
                            msg("Buying " + name + " for " + Config.fmt(unit) + " (limit " + Config.fmt(t.maxPrice) + ")");
                            click(mc, h, i);
                            state = State.CONFIRM; wait = 0;
                            return;
                        }
                    }
                }
                int next = findButton(h, Config.I.nextName);
                if (next >= 0 && page < Config.I.maxPages) { page++; click(mc, h, next); cooldown += 4; return; }
                // finished this scan
                scanLow.forEach(Market::record);
                mc.player.closeContainer();
                state = State.WAIT; cooldown = Config.I.refreshTicks;
            }
            case CONFIRM -> {
                if (Compat.screen(mc) instanceof ContainerScreen gs) {
                    int c = findButton(gs.getMenu(), Config.I.confirmName);
                    if (c >= 0) { click(mc, gs.getMenu(), c); state = State.SCAN; cooldown += 8; return; }
                }
                if (++wait > 20) { state = State.SCAN; } // no confirm screen: assume instant buy
            }
            case WAIT -> state = State.OPEN;
        }
    }
}
