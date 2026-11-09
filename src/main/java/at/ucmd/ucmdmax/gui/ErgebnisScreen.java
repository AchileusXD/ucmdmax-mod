package at.ucmd.ucmdmax.gui;

import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import at.ucmd.ucmdmax.akti.AktiFormular;

/** Schritt 3: Akti ist im Sheet. Wie in Discord: weitere Akti eintragen oder direkt ins Blatt springen. */
public final class ErgebnisScreen extends UcmdScreen {
	private final AktiFormular formular;
	private final String blatt;
	private final int zeile;
	private final String url;
	private final List<Component> zusammenfassung;
	private int oben;
	private int sichtbar;

	public ErgebnisScreen(AktiFormular formular, String titel, String blatt, int zeile, String url,
			List<Component> zusammenfassung) {
		super(Component.literal("✔ " + titel + " eingetragen"));
		this.formular = formular;
		this.blatt = blatt;
		this.zeile = zeile;
		this.url = url;
		this.zusammenfassung = List.copyOf(zusammenfassung);
	}

	@Override
	protected void init() {
		sichtbar = Math.min(zusammenfassung.size(), Math.max(0, (this.height - 110) / 11));
		int inhalt = 34 + sichtbar * 11;
		oben = Math.max(12, (this.height - inhalt - 40) / 2);

		int breite = 100;
		int x = this.width / 2 - (3 * breite + 8) / 2;
		int y = oben + inhalt + 10;
		Button sheet = knopf("Im Sheet ansehen", x, y, breite, b -> linkOeffnen(url));
		sheet.active = url.startsWith("https://");
		knopf("Weitere Akti", x + breite + 4, y, breite, b -> this.minecraft.gui.setScreen(new KategorieScreen(formular)));
		knopf("Fertig", x + 2 * (breite + 4), y, breite, b -> onClose());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		mitte(g, this.title, oben, GRUEN);
		mitte(g, "In deinem Blatt " + blatt + ", Zeile " + zeile + ".", oben + 13, GRAU);
		for (int i = 0; i < sichtbar; i++) {
			mitte(g, zusammenfassung.get(i), oben + 34 + i * 11, WEISS);
		}
	}
}
