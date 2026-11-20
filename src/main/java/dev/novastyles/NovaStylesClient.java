package dev.novastyles;

import dev.novastyles.config.Settings;
import dev.novastyles.gui.StylesScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NovaStylesClient implements ClientModInitializer {
    public static final String MOD_ID = "novastyles";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private boolean wasDown;

    @Override
    public void onInitializeClient() {
        Settings.load();
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> Settings.save());
    }

    // Right Shift is polled directly from GLFW: one cheap call per tick, no keybinding API needed.
    private void onTick(MinecraftClient client) {
        if (client.getWindow() == null) return;
        boolean down = GLFW.glfwGetKey(client.getWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (down && !wasDown) {
            if (client.currentScreen instanceof StylesScreen) {
                client.setScreen(null);
            } else if (client.currentScreen == null) {
                client.setScreen(new StylesScreen());
            }
        }
        wasDown = down;
    }
}
