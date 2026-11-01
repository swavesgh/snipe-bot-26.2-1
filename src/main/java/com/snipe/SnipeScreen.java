package com.snipe;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SnipeScreen extends Screen {
    static int selected = -1, listPage = 0, window = 1; // window: 0 hour, 1 24h, 2 7d
    static final long[] WINDOWS = {3600_000L, 86400_000L, 7 * 86400_000L};
    EditBox nameField, priceField;
    int x0, y0;

    public SnipeScreen() { super(Component.literal("AH Sniper")); }

    @Override protected void init() {
        x0 = width / 2 - 200; y0 = height / 2 - 120;
        addRenderableWidget(Button.builder(Component.literal(Sniper.running && Sniper.buy ? "Stop" : "Start sniping"), b -> {
            if (Sniper.running) Sniper.stop(); else Sniper.start(true); rebuildWidgets(); }).bounds(x0, y0, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal(Sniper.running && !Sniper.buy ? "Stop" : "Track only"), b -> {
            if (Sniper.running) Sniper.stop(); else Sniper.start(false); rebuildWidgets(); }).bounds(x0 + 105, y0, 90, 20).build());
        String[] w = {"Past hour", "Past 24h", "Past 7 days"};
        for (int i = 0; i < 3; i++) { int idx = i;
            addRenderableWidget(Button.builder(Component.literal(w[i]), b -> window = idx).bounds(x0 + 205 + i * 66, y0, 64, 20).build()); }

        var t = Config.I.targets;
        for (int i = 0; i < 8; i++) {
            int idx = listPage * 8 + i; if (idx >= t.size()) break;
            var tg = t.get(idx);
            addRenderableWidget(Button.builder(Component.literal((idx == selected ? "> " : "") + tg.name + " (" + Config.fmt(tg.maxPrice) + ")"),
                b -> { selected = idx; rebuildWidgets(); }).bounds(x0, y0 + 28 + i * 20, 195, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("<"), b -> { if (listPage > 0) { listPage--; rebuildWidgets(); } }).bounds(x0, y0 + 192, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal(">"), b -> { if ((listPage + 1) * 8 < t.size()) { listPage++; rebuildWidgets(); } }).bounds(x0 + 24, y0 + 192, 20, 18).build());
        addRenderableWidget(Button.builder(Component.literal("Remove selected"), b -> {
            if (selected >= 0 && selected < t.size()) { t.remove(selected); selected = -1; Config.save(); rebuildWidgets(); } }).bounds(x0 + 48, y0 + 192, 100, 18).build());

        nameField = new EditBox(font, x0 + 205, y0 + 150, 195, 18, Component.literal("Item"));
        nameField.setHint(Component.literal("Item name"));
        priceField = new EditBox(font, x0 + 205, y0 + 172, 100, 18, Component.literal("Price"));
        priceField.setHint(Component.literal("Snipe under"));
        addRenderableWidget(nameField); addRenderableWidget(priceField);
        addRenderableWidget(Button.builder(Component.literal("Add to snipes"), b -> {
            double p = Config.parseUser(priceField.getValue());
            if (!nameField.getValue().isBlank() && p > 0) {
                t.add(new Config.Target(nameField.getValue().trim(), p)); selected = t.size() - 1; Config.save(); rebuildWidgets();
            } }).bounds(x0 + 310, y0 + 172, 90, 18).build());
    }

    @Override public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float delta) {
        super.extractRenderState(g, mx, my, delta);
        int tx = x0 + 205, ty = y0 + 32;
        var t = Config.I.targets;
        String status = Sniper.running ? (Sniper.buy ? "SNIPING" : "TRACKING") : "idle";
        g.text(font, "Status: " + status, tx, ty, Sniper.running ? 0xFF55FF55 : 0xFFAAAAAA);
        if (selected >= 0 && selected < t.size()) {
            var tg = t.get(selected);
            g.text(font, tg.name, tx, ty + 16, 0xFF55FFFF);
            g.text(font, "Snipe under: " + Config.fmt(tg.maxPrice), tx, ty + 28, 0xFFFFFFFF);
            var s = Market.stats(tg.name, WINDOWS[window]);
            if (s == null) g.text(font, "No market data yet - run Track only", tx, ty + 44, 0xFFFFAA00);
            else {
                g.text(font, "Lowest:  " + Config.fmt(s.low()), tx, ty + 44, 0xFF55FF55);
                g.text(font, "Average: " + Config.fmt(s.avg()), tx, ty + 56, 0xFFFFFF55);
                g.text(font, "Median:  " + Config.fmt(s.median()), tx, ty + 68, 0xFFFFFFFF);
                g.text(font, "Highest: " + Config.fmt(s.high()), tx, ty + 80, 0xFFFFFFFF);
                g.text(font, "Samples: " + s.n(), tx, ty + 92, 0xFFAAAAAA);
            }
        } else g.text(font, "Select a snipe to see market stats", tx, ty + 16, 0xFFAAAAAA);
    }

    @Override public boolean isPauseScreen() { return false; }
}
