package at.ucmd.ucmdmax.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import at.ucmd.ucmdmax.akti.AktiFormular.Feld;

/**
 * Auswahl aus einem Dropdown des Akti-Blatts (z.B. "Art" oder "Eingeteilt von").
 * Lange Listen bekommen ein Suchfeld und werden auf Seiten verteilt.
 */
public final class AuswahlScreen extends UcmdScreen {
	private final List<String> alle;
	private final String aktuell;
	private final Consumer<String> gewaehlt;
	private final Supplier<Screen> zurueck;

	private final List<Button> plaetze = new ArrayList<>();
	private List<String> treffer;
	private String suchtext = "";
	private boolean ersteAnzeige = true;
	private int seite;
	private int seiten = 1;
	private int listeOben;
	private Button vorher;
	private Button weiter;

	public AuswahlScreen(Feld feld, String aktuell, Consumer<String> gewaehlt, Supplier<Screen> zurueck) {
		super(Component.literal(feld.label() + " auswählen"));
		this.alle = feld.optionen();
		this.treffer = this.alle;
		this.aktuell = aktuell;
		this.gewaehlt = gewaehlt;
		this.zurueck = zurueck;
	}

	@Override
	protected void init() {
		plaetze.clear();
		int y = 30;
		if (alle.size() > 12) {
			EditBox suche = new EditBox(this.font, this.width / 2 - 100, y, 200, 20, (EditBox) null,
					Component.literal("Suchen"));
			suche.setValue(suchtext);
			suche.setResponder(text -> {
				suchtext = text;
				seite = 0;
				filtern();
			});
			addRenderableWidget(suche);
			setInitialFocus(suche);
			y += 26;
		}
		listeOben = y;

		int spalten = this.width >= 460 ? 3 : (this.width >= 310 ? 2 : 1);
		int breite = Math.min(140, (this.width - 30) / spalten - 4);
		int reihen = Math.max(1, (this.height - listeOben - 46) / 22);
		int startX = (this.width - (spalten * (breite + 4) - 4)) / 2;
		for (int i = 0; i < spalten * reihen; i++) {
			int platz = i;
			plaetze.add(knopf("", startX + (i % spalten) * (breite + 4), listeOben + (i / spalten) * 22, breite,
					b -> waehlen(platz)));
		}

		int unten = this.height - 26;
		vorher = knopf("◀", this.width / 2 - 106, unten, 40, b -> blaettern(-1));
		knopf("Zurück", this.width / 2 - 62, unten, 124, b -> onClose());
		weiter = knopf("▶", this.width / 2 + 66, unten, 40, b -> blaettern(1));

		if (ersteAnzeige) { // gleich die Seite mit der aktuellen Auswahl zeigen
			ersteAnzeige = false;
			int index = alle.indexOf(aktuell);
			seite = index < 0 ? 0 : index / plaetze.size();
		}
		filtern();
	}

	private void filtern() {
		String suche = suchtext.strip().toLowerCase(Locale.ROOT);
		treffer = suche.isEmpty() ? alle
				: alle.stream().filter(o -> o.toLowerCase(Locale.ROOT).contains(suche)).toList();
		anzeigen();
	}

	private void anzeigen() {
		int proSeite = plaetze.size();
		seiten = Math.max(1, (treffer.size() + proSeite - 1) / proSeite);
		seite = Math.clamp(seite, 0, seiten - 1);
		for (int i = 0; i < proSeite; i++) {
			Button b = plaetze.get(i);
			int index = seite * proSeite + i;
			b.visible = index < treffer.size();
			if (b.visible) {
				String option = treffer.get(index);
				String text = option.strip().isEmpty() ? "?" : option.strip();
				b.setMessage(option.equals(aktuell)
						? Component.literal("» " + text + " «").withStyle(ChatFormatting.GREEN)
						: Component.literal(text));
			}
		}
		vorher.visible = seiten > 1;
		weiter.visible = seiten > 1;
		vorher.active = seite > 0;
		weiter.active = seite < seiten - 1;
	}

	private void blaettern(int richtung) {
		seite += richtung;
		anzeigen();
	}

	private void waehlen(int platz) {
		int index = seite * plaetze.size() + platz;
		if (index < treffer.size()) {
			gewaehlt.accept(treffer.get(index));
			this.minecraft.gui.setScreen(zurueck.get());
		}
	}

	@Override
	public void onClose() {
		this.minecraft.gui.setScreen(zurueck.get());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (seiten > 1 && scrollY != 0) {
			blaettern(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		mitte(g, this.title, 12, GOLD);
		if (treffer.isEmpty()) {
			mitte(g, alle.isEmpty() ? "Diese Liste ist leer." : "Keine Treffer.", listeOben + 8, GRAU);
		}
		if (seiten > 1) {
			mitte(g, "Seite " + (seite + 1) + " / " + seiten, this.height - 38, GRAU);
		}
	}
}
