package at.ucmd.ucmdmax.gui;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Gemeinsame Hilfen fuer alle Bildschirme der Mod. */
abstract class UcmdScreen extends Screen {
	// Farben immer mit Alpha (0xFF......), sonst ist der Text unsichtbar
	static final int WEISS = 0xFFFFFFFF;
	static final int GRAU = 0xFFA8A8A8;
	static final int DUNKELGRAU = 0xFF6E6E6E;
	static final int ROT = 0xFFFF6B6B;
	static final int GRUEN = 0xFF6BFF8A;
	static final int GOLD = 0xFFFFC94D;

	protected UcmdScreen(Component titel) {
		super(titel);
	}

	/** Knopf anlegen und hinzufuegen (Hoehe immer 20, sonst sehen die Texturen komisch aus). */
	protected Button knopf(Component text, int x, int y, int breite, Consumer<Button> aktion) {
		return addRenderableWidget(Button.builder(text, b -> aktion.accept(b)).bounds(x, y, breite, 20).build());
	}

	protected Button knopf(String text, int x, int y, int breite, Consumer<Button> aktion) {
		return knopf(Component.literal(text), x, y, breite, aktion);
	}

	protected void mitte(GuiGraphicsExtractor g, Component text, int y, int farbe) {
		g.centeredText(this.font, text, this.width / 2, y, farbe);
	}

	protected void mitte(GuiGraphicsExtractor g, String text, int y, int farbe) {
		mitte(g, Component.literal(text), y, farbe);
	}

	/** Text auf mehrere Zeilen umbrechen und mittig zeichnen. Gibt die benutzte Hoehe zurueck. */
	protected int mitteUmbrochen(GuiGraphicsExtractor g, Component text, int y, int breite, int farbe) {
		return mitteUmbrochen(g, text, y, breite, farbe, Integer.MAX_VALUE);
	}

	/** Wie oben, aber hoechstens {@code maxZeilen} Zeilen. */
	protected int mitteUmbrochen(GuiGraphicsExtractor g, Component text, int y, int breite, int farbe, int maxZeilen) {
		List<FormattedCharSequence> zeilen = this.font.split(text, breite);
		int anzahl = Math.min(zeilen.size(), maxZeilen);
		int zeilenHoehe = this.font.lineHeight + 2;
		for (int i = 0; i < anzahl; i++) {
			FormattedCharSequence zeile = zeilen.get(i);
			g.text(this.font, zeile, (this.width - this.font.width(zeile)) / 2, y + i * zeilenHoehe, farbe);
		}
		return anzahl * zeilenHoehe;
	}

	/** Wie viele Zeilen der Text umgebrochen braucht. */
	protected int zeilenAnzahl(Component text, int breite) {
		return this.font.split(text, breite).size();
	}

	/** Kuerzt einen Text mit "...", damit er in {@code pixel} Breite passt. */
	protected String kuerzen(String text, int pixel) {
		if (this.font.width(text) <= pixel) {
			return text;
		}
		String kurz = text;
		while (!kurz.isEmpty() && this.font.width(kurz + "...") > pixel) {
			kurz = kurz.substring(0, kurz.length() - 1);
		}
		return kurz + "...";
	}

	/** Link nach Rueckfrage im Browser oeffnen (Minecraft-eigener Bestaetigungsdialog). */
	protected void linkOeffnen(String url) {
		if (url != null && url.startsWith("https://")) {
			ConfirmLinkScreen.confirmLinkNow(this, url, true);
		}
	}
}
