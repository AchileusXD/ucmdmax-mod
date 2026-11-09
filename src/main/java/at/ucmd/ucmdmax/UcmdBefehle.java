package at.ucmd.ucmdmax;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import at.ucmd.ucmdmax.api.BotApi;
import at.ucmd.ucmdmax.gui.LadeScreen;

/**
 * Alle Befehle unter /ucmdmax (laufen nur im eigenen Client, der Server sieht sie nicht).
 *
 * <pre>
 * /ucmdmax                     Hilfe
 * /ucmdmax newakti             Akti eintragen (GUI)
 * /ucmdmax verbinden &lt;Code&gt;    Mod mit Discord verbinden (Code aus /modverbinden)
 * /ucmdmax status              Verbindung pruefen
 * /ucmdmax trennen             Verbindung loesen
 * /ucmdmax server [Adresse]    Bot-Adresse anzeigen/aendern ("standard" = zuruecksetzen)
 * </pre>
 */
public final class UcmdBefehle {
	private UcmdBefehle() {
	}

	public static void registrieren(CommandDispatcher<FabricClientCommandSource> dispatcher) {
		dispatcher.register(ClientCommands.literal("ucmdmax")
				// Klickt man in einer Server-Nachricht auf so einen Befehl, fragt Minecraft erst nach
				.requires(FabricClientCommandSource::attended)
				.executes(ctx -> hilfe())
				.then(ClientCommands.literal("newakti").executes(ctx -> newakti()))
				.then(ClientCommands.literal("verbinden")
						.executes(ctx -> {
							UcmdMax.chatFehler("Code fehlt: In Discord /modverbinden eingeben, dann hier /ucmdmax verbinden <Code>.");
							return 0;
						})
						.then(ClientCommands.argument("code", StringArgumentType.greedyString())
								.executes(ctx -> verbinden(StringArgumentType.getString(ctx, "code")))))
				.then(ClientCommands.literal("status").executes(ctx -> status()))
				.then(ClientCommands.literal("trennen").executes(ctx -> trennen()))
				.then(ClientCommands.literal("server")
						.executes(ctx -> serverAnzeigen())
						.then(ClientCommands.argument("adresse", StringArgumentType.greedyString())
								.executes(ctx -> serverSetzen(StringArgumentType.getString(ctx, "adresse"))))));
	}

	private static int hilfe() {
		UcmdMax.chat(Component.literal("UCMD Max " + UcmdMax.version() + " - Befehle:").withStyle(ChatFormatting.GOLD));
		zeile("/ucmdmax newakti", "Akti in dein Akti-Blatt eintragen (oder Taste K)");
		zeile("/ucmdmax verbinden <Code>", "mit Discord verbinden (Code: /modverbinden in Discord)");
		zeile("/ucmdmax status", "Verbindung prüfen");
		zeile("/ucmdmax trennen", "Verbindung lösen");
		zeile("/ucmdmax server [Adresse]", "Bot-Adresse anzeigen oder ändern");
		return 1;
	}

	private static void zeile(String befehl, String text) {
		UcmdMax.chat(Component.literal(befehl).withStyle(ChatFormatting.YELLOW)
				.append(Component.literal(" - " + text).withStyle(ChatFormatting.GRAY)));
	}

	private static void nichtVerbunden() {
		UcmdMax.chatFehler("Die Mod ist noch nicht mit Discord verbunden.");
		UcmdMax.chat(Component.literal("1. In Discord ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal("/modverbinden").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" eingeben  2. Hier ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("/ucmdmax verbinden <Code>").withStyle(ChatFormatting.YELLOW)));
	}

	/** Oeffnet das Akti-Fenster - ueber /ucmdmax newakti oder die Taste (siehe UcmdMax). */
	static int newakti() {
		if (!UcmdMax.konfig().verbunden()) {
			nichtVerbunden();
			return 0;
		}
		UcmdMax.oeffnen(new LadeScreen());
		return 1;
	}

	private static int verbinden(String code) {
		GameProfile profil = Minecraft.getInstance().getGameProfile();
		BotApi api = UcmdMax.api();
		UcmdMax.chat(Component.literal("Verbinde mit dem UCMD-Bot...").withStyle(ChatFormatting.GRAY));
		api.danach(api.verbinden(code.trim(), profil.name(), String.valueOf(profil.id())), antwort -> {
			UcmdMax.konfig().setToken(BotApi.text(antwort, "token", ""));
			UcmdMax.chat(Component.literal("✔ Verbunden mit Discord: ").withStyle(ChatFormatting.GREEN)
					.append(Component.literal(BotApi.text(antwort, "discord", "?")).withStyle(ChatFormatting.WHITE)));
			String blatt = BotApi.text(antwort, "blatt", null);
			if (blatt != null) {
				UcmdMax.chat(Component.literal("Dein Akti-Blatt: " + blatt + " - jetzt geht ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal("/ucmdmax newakti").withStyle(ChatFormatting.YELLOW)));
			} else {
				UcmdMax.chatFehler("Achtung: Kein Akti-Blatt gefunden, das genau wie dein Discord-Nickname heißt. "
						+ "Bitte bei der Leitung melden.");
			}
		}, fehler -> UcmdMax.chatFehler(fehler.getMessage()));
		return 1;
	}

	private static int status() {
		UcmdMax.chat(Component.literal("Bot-Adresse: " + UcmdMax.konfig().apiUrl()).withStyle(ChatFormatting.GRAY));
		if (!UcmdMax.konfig().verbunden()) {
			nichtVerbunden();
			return 0;
		}
		BotApi api = UcmdMax.api();
		api.danach(api.ich(), antwort -> {
			String mc = BotApi.text(antwort, "mc_name", null);
			UcmdMax.chat(Component.literal("✔ Verbunden mit Discord: ").withStyle(ChatFormatting.GREEN)
					.append(Component.literal(BotApi.text(antwort, "discord", "?")).withStyle(ChatFormatting.WHITE))
					.append(Component.literal(mc != null && !mc.isEmpty() ? " (Minecraft: " + mc + ")" : "")
							.withStyle(ChatFormatting.GRAY)));
			String blatt = BotApi.text(antwort, "blatt", null);
			if (blatt != null) {
				UcmdMax.chat(Component.literal("Akti-Blatt: " + blatt).withStyle(ChatFormatting.GRAY));
			} else {
				UcmdMax.chatFehler("Kein Akti-Blatt gefunden, das genau wie dein Discord-Nickname heißt.");
			}
		}, fehler -> UcmdMax.chatFehler(fehler.getMessage()));
		return 1;
	}

	private static int trennen() {
		if (!UcmdMax.konfig().verbunden()) {
			UcmdMax.chat("Die Mod war nicht verbunden.");
			return 0;
		}
		BotApi api = UcmdMax.api();
		// Erst beim Bot abmelden (die Anfrage nimmt den Token sofort mit), dann lokal loeschen
		api.danach(api.trennen(), antwort -> { }, fehler -> UcmdMax.LOG.info("Trennen beim Bot: {}", fehler.getMessage()));
		UcmdMax.konfig().setToken("");
		UcmdMax.chat(Component.literal("✔ Verbindung getrennt.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int serverAnzeigen() {
		UcmdMax.chat("Bot-Adresse: " + UcmdMax.konfig().apiUrl());
		UcmdMax.chat(Component.literal("Ändern: /ucmdmax server <Adresse>  ·  zurücksetzen: /ucmdmax server standard")
				.withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int serverSetzen(String eingabe) {
		String adresse = eingabe.trim();
		if (adresse.equalsIgnoreCase("standard")) {
			adresse = UcmdKonfig.STANDARD_URL;
		}
		if (!adresse.matches("(?i)https?://[^\\s/?#]+(/\\S*)?")) {
			UcmdMax.chatFehler("Ungültige Adresse. Beispiel: /ucmdmax server " + UcmdKonfig.STANDARD_URL);
			return 0;
		}
		adresse = adresse.replaceAll("/+$", "");
		if (adresse.equals(UcmdMax.konfig().apiUrl())) {
			UcmdMax.chat("Die Bot-Adresse ist schon " + adresse + ".");
			return 1;
		}
		UcmdMax.konfig().setApiUrl(adresse);
		UcmdMax.chat(Component.literal("Bot-Adresse geändert: " + adresse).withStyle(ChatFormatting.GREEN));
		UcmdMax.chat(Component.literal("Bitte neu verbinden: /modverbinden in Discord, dann /ucmdmax verbinden <Code>.")
				.withStyle(ChatFormatting.GRAY));
		return 1;
	}
}
