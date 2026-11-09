package at.ucmd.ucmdmax;

import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.loader.api.FabricLoader;

import at.ucmd.ucmdmax.api.BotApi;

/**
 * Einstiegspunkt der Mod (laeuft nur im Client).
 *
 * <p>Die Mod redet nie direkt mit Google. Alles laeuft ueber den UCMD-Discord-Bot
 * (mod_api.py im Bot-Ordner): Der Bot prueft Rollen und Nickname und schreibt dann
 * genau wie /newakti ins Akti-Sheet.
 */
public final class UcmdMax implements ClientModInitializer {
	public static final String MOD_ID = "ucmdmax";
	public static final Logger LOG = LoggerFactory.getLogger("UCMD Max");

	private static UcmdKonfig konfig;
	private static BotApi api;
	private static Screen naechsterScreen;
	private static KeyMapping aktiTaste;

	@Override
	public void onInitializeClient() {
		konfig = UcmdKonfig.laden();
		api = new BotApi(konfig, version());

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> UcmdBefehle.registrieren(dispatcher));

		// Taste fuer das Akti-Fenster (Standard K, aenderbar unter Optionen > Steuerung > UCMD Max)
		KeyMapping.Category kategorie = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "ucmdmax"));
		aktiTaste = KeyMappingHelper.registerKeyMapping(
				new KeyMapping("key.ucmdmax.newakti", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, kategorie));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (aktiTaste.consumeClick()) {
				if (client.player != null && client.gui.screen() == null) {
					UcmdBefehle.newakti();
				}
			}
			// Bildschirme aus Chat-Befehlen erst im naechsten Tick oeffnen,
			// sonst macht der Chat sie beim Schliessen gleich wieder zu.
			if (naechsterScreen != null) {
				Screen screen = naechsterScreen;
				naechsterScreen = null;
				client.gui.setScreen(screen);
			}
		});

		LOG.info("UCMD Max {} geladen (Bot: {})", version(), konfig.apiUrl());
	}

	public static UcmdKonfig konfig() {
		return konfig;
	}

	public static BotApi api() {
		return api;
	}

	/** Oeffnet den Bildschirm im naechsten Client-Tick. */
	public static void oeffnen(Screen screen) {
		naechsterScreen = screen;
	}

	/** Nachricht mit [UCMD]-Praefix in den eigenen Chat (sieht nur man selbst). */
	public static void chat(Component text) {
		Minecraft mc = Minecraft.getInstance();
		Component nachricht = Component.empty()
				.append(Component.literal("[UCMD] ").withStyle(ChatFormatting.AQUA))
				.append(text);
		mc.execute(() -> mc.gui.hud.getChat().addClientSystemMessage(nachricht));
	}

	public static void chat(String text) {
		chat(Component.literal(text));
	}

	public static void chatFehler(String text) {
		chat(Component.literal(text).withStyle(ChatFormatting.RED));
	}

	public static String version() {
		return FabricLoader.getInstance().getModContainer(MOD_ID)
				.map(c -> c.getMetadata().getVersion().getFriendlyString())
				.orElse("?");
	}
}
