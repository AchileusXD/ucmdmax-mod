package at.ucmd.ucmdmax.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import at.ucmd.ucmdmax.UcmdMax;
import at.ucmd.ucmdmax.api.ApiFehler;
import at.ucmd.ucmdmax.api.BotApi;

/** Schritt 0: Formular (Kategorien, Felder, Dropdowns) vom Bot laden. Zeigt Fehler verstaendlich an. */
public final class LadeScreen extends UcmdScreen {
	private boolean gestartet;
	private ApiFehler fehler;
	private Button nochmal;

	public LadeScreen() {
		super(Component.literal("Neue Akti eintragen"));
	}

	@Override
	protected void init() {
		int y = this.height / 2 + 34;
		nochmal = knopf("Erneut versuchen", this.width / 2 - 104, y, 100, b -> laden());
		knopf("Schließen", this.width / 2 + 4, y, 100, b -> onClose());
		nochmal.visible = fehler != null;
		if (!gestartet) {
			gestartet = true;
			laden();
		}
	}

	private void laden() {
		fehler = null;
		nochmal.visible = false;
		BotApi api = UcmdMax.api();
		api.danach(api.aktiFormular(), formular -> {
			if (this.minecraft.gui.screen() == this) {
				this.minecraft.gui.setScreen(new KategorieScreen(formular));
			}
		}, f -> {
			fehler = f;
			nochmal.visible = true;
		});
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		mitte(g, this.title, this.height / 2 - 46, GOLD);
		if (fehler == null) {
			String punkte = ".".repeat(1 + (int) (System.currentTimeMillis() / 400 % 3));
			mitte(g, "Lade dein Akti-Blatt" + punkte, this.height / 2 - 10, GRAU);
		} else {
			mitteUmbrochen(g, Component.literal(fehler.getMessage()), this.height / 2 - 24,
					Math.min(340, this.width - 40), ROT);
		}
	}
}
