package dev.novastyles.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.novastyles.NovaStylesClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * All settings live here as public static fields.
 * Render hooks (mixins) read them directly, e.g. Settings.CRYSTAL_SCALE.value.
 */
public final class Settings {
    private Settings() { }

    // Crystals
    public static final Setting.Bool CRYSTAL_ENABLED = new Setting.Bool("crystal.enabled", "Enabled", true);
    public static final Setting.Mode CRYSTAL_STYLE = new Setting.Mode("crystal.style", "Style", 0, "Vanilla", "Minimal", "Glow");
    public static final Setting.Num CRYSTAL_SCALE = new Setting.Num("crystal.scale", "Scale", 0.5f, 1.5f, 1.0f, 0.05f);
    public static final Setting.Num CRYSTAL_SPIN = new Setting.Num("crystal.spin", "Spin speed", 0f, 2f, 1.0f, 0.05f);

    // Anchors
    public static final Setting.Bool ANCHOR_ENABLED = new Setting.Bool("anchor.enabled", "Enabled", true);
    public static final Setting.Mode ANCHOR_STYLE = new Setting.Mode("anchor.style", "Style", 0, "Vanilla", "Minimal", "Glow");
    public static final Setting.Num ANCHOR_GLOW = new Setting.Num("anchor.glow", "Glow strength", 0f, 1f, 0.6f, 0.05f);

    // Armor
    public static final Setting.Bool ARMOR_ENABLED = new Setting.Bool("armor.enabled", "Enabled", true);
    public static final Setting.Mode ARMOR_STYLE = new Setting.Mode("armor.style", "Style", 0, "Vanilla", "Flat", "Tinted");
    public static final Setting.Num ARMOR_OPACITY = new Setting.Num("armor.opacity", "Opacity", 0.1f, 1f, 1.0f, 0.05f);

    // Fire
    public static final Setting.Bool FIRE_ENABLED = new Setting.Bool("fire.enabled", "Enabled", true);
    public static final Setting.Mode FIRE_STYLE = new Setting.Mode("fire.style", "Style", 0, "Vanilla", "Low", "Clean");
    public static final Setting.Num FIRE_OPACITY = new Setting.Num("fire.opacity", "Opacity", 0.1f, 1f, 1.0f, 0.05f);

    // Interface
    public static final Setting.Mode ACCENT = new Setting.Mode("ui.accent", "Accent color", 0, "Violet", "Blue", "Pink", "Green", "Orange");
    public static final Setting.Bool ANIMATIONS = new Setting.Bool("ui.animations", "Animations", true);

    public static final List<Category> CATEGORIES = List.of(
            new Category("Crystals", List.of(CRYSTAL_ENABLED, CRYSTAL_STYLE, CRYSTAL_SCALE, CRYSTAL_SPIN)),
            new Category("Anchors", List.of(ANCHOR_ENABLED, ANCHOR_STYLE, ANCHOR_GLOW)),
            new Category("Armor", List.of(ARMOR_ENABLED, ARMOR_STYLE, ARMOR_OPACITY)),
            new Category("Fire", List.of(FIRE_ENABLED, FIRE_STYLE, FIRE_OPACITY)),
            new Category("Interface", List.of(ACCENT, ANIMATIONS))
    );

    private static final int[] ACCENT_COLORS = {0xFF8B5CF6, 0xFF3B82F6, 0xFFEC4899, 0xFF22C55E, 0xFFF97316};

    public static int accent() {
        return ACCENT_COLORS[ACCENT.index];
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("novastyles.json");
    }

    public static void load() {
        Path p = file();
        if (!Files.exists(p)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(p)) {
            Map<String, String> map = GSON.fromJson(r, new TypeToken<Map<String, String>>() { }.getType());
            if (map == null) return;
            for (Category c : CATEGORIES) {
                for (Setting s : c.settings()) {
                    String v = map.get(s.id);
                    if (v != null) s.deserialize(v);
                }
            }
        } catch (Exception e) {
            NovaStylesClient.LOGGER.warn("Failed to load config, using defaults", e);
        }
    }

    public static void save() {
        Map<String, String> map = new LinkedHashMap<>();
        for (Category c : CATEGORIES) {
            for (Setting s : c.settings()) map.put(s.id, s.serialize());
        }
        Path p = file();
        try {
            Files.createDirectories(p.getParent());
            try (Writer w = Files.newBufferedWriter(p)) {
                GSON.toJson(map, w);
            }
        } catch (IOException e) {
            NovaStylesClient.LOGGER.warn("Failed to save config", e);
        }
    }
}
