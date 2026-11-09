package at.ucmd.ucmdmax.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import at.ucmd.ucmdmax.UcmdMax;
import at.ucmd.ucmdmax.akti.AktiFormular;
import at.ucmd.ucmdmax.akti.AktiFormular.Kategorie;

/** Schritt 1: Roleplay-Akti / Großakti / Teamakti / SubFrak-Akti - wie die Knoepfe bei /newakti in Discord. */
public final class KategorieScreen extends UcmdScreen {
	private final AktiFormular formular;
	private int knopfOben;

	public KategorieScreen(AktiFormular formular) {
		super(Component.literal("Neue Akti eintragen"));
		this.formular = formular;
	}

	@Override
	protected void init() {
		int breite = 200;
		int x = (this.width - breite) / 2;
		int hoehe = formular.kategorien().size() * 24 + 30;
		knopfOben = Math.max(48, (this.height - hoehe) / 2 + 10);

		String letzte = UcmdMax.konfig().letzteKategorie();
		Button zuletzt = null;
		int y = knopfOben;
		for (Kategorie k : formular.kategorien()) {
			String text = k.titel();
			if (FormularScreen.hatEntwurf(k.key())) {
				text += " (Entwurf)";
			} else if (k.key().equals(letzte)) {
				text += " (zuletzt)";
			}
			Button b = knopf(text, x, y, breite, btn -> {
				UcmdMax.konfig().setLetzteKategorie(k.key());
				this.minecraft.gui.setScreen(new FormularScreen(formular, k));
			});
			if (!k.nutzbar()) {
				b.active = false;
				b.setTooltip(Tooltip.create(Component.literal("In deinem Blatt fehlt das Dropdown für: "
						+ String.join(", ", k.fehlt()) + ". Bitte bei der Leitung melden.")));
			} else if (k.key().equals(letzte)) {
				zuletzt = b;
			}
			y += 24;
		}

		y += 10;
		Button blatt = knopf("Blatt ansehen", x, y, breite / 2 - 2, b -> linkOeffnen(formular.url()));
		blatt.active = formular.url().startsWith("https://");
		knopf("Abbrechen", x + breite / 2 + 2, y, breite / 2 - 2, b -> onClose());

		// Die zuletzt benutzte Kategorie ist gleich ausgewaehlt - Enter oeffnet sie
		if (zuletzt != null) {
			setInitialFocus(zuletzt);
		}
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		mitte(g, this.title, knopfOben - 38, GOLD);
		mitte(g, "Blatt: " + formular.blatt(), knopfOben - 24, GRAU);
		mitte(g, "Welche Akti möchtest du eintragen?", knopfOben - 12, WEISS);
	}
}
