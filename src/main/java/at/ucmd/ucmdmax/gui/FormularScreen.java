package at.ucmd.ucmdmax.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonObject;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import at.ucmd.ucmdmax.UcmdMax;
import at.ucmd.ucmdmax.akti.AktiFormular;
import at.ucmd.ucmdmax.akti.AktiFormular.Feld;
import at.ucmd.ucmdmax.akti.AktiFormular.Kategorie;
import at.ucmd.ucmdmax.api.BotApi;

/**
 * Schritt 2: das Formular einer Kategorie. Pro Feld aus AKTI_KATEGORIEN eine Zeile:
 * <ul>
 *   <li>text / lang / datum / link -> Textfeld (Datum ist mit heute vorausgefuellt)</li>
 *   <li>auswahl -> Knopf, der die Liste aus dem Dropdown im Blatt oeffnet</li>
 *   <li>janein -> Knopf Ja/Nein (standardmaessig Nein, wie die Haekchen im Blatt)</li>
 * </ul>
 * Der Bot prueft beim Eintragen alles nochmal (Datum, Link, Liste) und meldet Fehler pro Feld zurueck.
 */
public final class FormularScreen extends UcmdScreen {
	/** Noch nicht gesendete Eingaben pro Kategorie - nach ESC ist nichts weg (bis Minecraft beendet wird). */
	private static final Map<String, Map<String, Object>> ENTWUERFE = new HashMap<>();

	private static final int OBEN = 40;         // erste Feldzeile
	private static final int ZEILE = 24;        // Hoehe einer Feldzeile
	private static final int FEHLER_HOEHE = 11; // zusaetzlicher Platz fuer eine Fehlermeldung unter dem Feld

	private final AktiFormular formular;
	private final Kategorie kategorie;
	private final Map<String, Object> werte; // Feld-Key -> String oder Boolean
	private final Map<String, String> fehler;
	private final String meldung;
	/** true = die Meldung gehoert zu markierten Feldern und verschwindet, sobald alle korrigiert sind. */
	private final boolean meldungZuFeldern;
	private boolean sendet;
	private int scroll;

	private final List<Zeile> zeilen = new ArrayList<>();
	private final List<Button> knoepfe = new ArrayList<>();
	private int labelX;
	private int labelBreite;
	private int feldX;
	private int feldBreite;
	private int unten;
	private int inhaltHoehe;

	private record Zeile(Feld feld, AbstractWidget widget, int abstand) {
	}

	public FormularScreen(AktiFormular formular, Kategorie kategorie) {
		this(formular, kategorie, Map.of(), null, 0);
	}

	private FormularScreen(AktiFormular formular, Kategorie kategorie, Map<String, String> fehler, String meldung,
			int scroll) {
		super(Component.literal(kategorie.titel() + " eintragen"));
		this.formular = formular;
		this.kategorie = kategorie;
		this.fehler = new HashMap<>(fehler);
		this.meldung = meldung;
		this.meldungZuFeldern = !fehler.isEmpty();
		this.scroll = scroll;
		this.werte = ENTWUERFE.computeIfAbsent(kategorie.key(), k -> new HashMap<>());
	}

	/** Hat man in dieser Kategorie schon etwas eingegeben, aber noch nicht eingetragen? */
	public static boolean hatEntwurf(String kategorie) {
		Map<String, Object> entwurf = ENTWUERFE.get(kategorie);
		return entwurf != null && entwurf.values().stream()
				.anyMatch(w -> Boolean.TRUE.equals(w) || (w instanceof String s && !s.isBlank()));
	}

	/** Gleiches Formular neu aufbauen (z.B. nach einer Auswahl oder mit Fehlern vom Bot). */
	private FormularScreen kopie(Map<String, String> neueFehler, String neueMeldung) {
		return new FormularScreen(formular, kategorie, neueFehler, neueMeldung, scroll);
	}

	/** Allgemeine Meldung anzeigen? Feld-Meldungen nur, solange noch ein Feld rot ist. */
	private String aktuelleMeldung() {
		return meldungZuFeldern && fehler.isEmpty() ? null : meldung;
	}

	private Object wert(Feld f) {
		Object w = werte.get(f.key());
		if (w != null) {
			return w;
		}
		if (f.istJaNein()) {
			return Boolean.FALSE;
		}
		if (f.istDatum()) {
			return formular.heute();
		}
		return "";
	}

	private static String beschriftung(Feld f) {
		boolean schonMarkiert = f.label().toLowerCase(Locale.ROOT).contains("optional");
		return f.pflicht() || schonMarkiert ? f.label() : f.label() + " (optional)";
	}

	// ---------- Aufbau ----------

	@Override
	protected void init() {
		zeilen.clear();
		knoepfe.clear();
		unten = this.height - 52;

		// Beschriftung links, Eingabe rechts - zusammen mittig
		int laengste = 0;
		for (Feld f : kategorie.felder()) {
			laengste = Math.max(laengste, this.font.width(beschriftung(f)));
		}
		labelBreite = Math.min(laengste, Math.max(80, this.width * 2 / 5 - 20));
		feldBreite = Math.clamp(this.width - labelBreite - 50, 100, 240);
		labelX = (this.width - (labelBreite + 10 + feldBreite)) / 2;
		feldX = labelX + labelBreite + 10;

		int abstand = 0;
		AbstractWidget erstesTextfeld = null;
		for (Feld f : kategorie.felder()) {
			AbstractWidget w = addRenderableWidget(eingabe(f));
			zeilen.add(new Zeile(f, w, abstand));
			abstand += ZEILE + (fehler.containsKey(f.key()) ? FEHLER_HOEHE : 0);
			if (erstesTextfeld == null && w instanceof EditBox) {
				erstesTextfeld = w;
			}
		}
		inhaltHoehe = abstand;

		int breite = 96;
		int x = this.width / 2 - (3 * breite + 8) / 2;
		int y = this.height - 26;
		knoepfe.add(knopf(Component.literal("✔ Eintragen").withStyle(ChatFormatting.GREEN), x, y, breite, k -> eintragen()));
		knoepfe.add(knopf("Zurück", x + breite + 4, y, breite,
				k -> this.minecraft.gui.setScreen(new KategorieScreen(formular))));
		knopf("Abbrechen", x + 2 * (breite + 4), y, breite, k -> onClose());

		scroll = Math.clamp(scroll, 0, maxScroll());
		positionieren();
		sperren(sendet);
		if (erstesTextfeld != null) {
			setInitialFocus(erstesTextfeld);
		}
	}

	private AbstractWidget eingabe(Feld f) {
		if (f.istJaNein()) {
			return Button.builder(jaNein(Boolean.TRUE.equals(wert(f))), b -> {
				boolean neu = !Boolean.TRUE.equals(wert(f));
				werte.put(f.key(), neu);
				fehler.remove(f.key());
				b.setMessage(jaNein(neu));
			}).bounds(feldX, 0, Math.min(90, feldBreite), 20).build();
		}

		if (f.istAuswahl()) {
			return Button.builder(auswahlText(f), b -> this.minecraft.gui.setScreen(
					new AuswahlScreen(f, String.valueOf(wert(f)), wahl -> {
						werte.put(f.key(), wahl);
						fehler.remove(f.key());
					}, () -> kopie(fehler, aktuelleMeldung()))))
					.bounds(feldX, 0, feldBreite, 20).build();
		}

		EditBox box = new EditBox(this.font, feldX, 0, feldBreite, 20, (EditBox) null, Component.literal(f.label()));
		box.setMaxLength(f.max()); // vor setValue, sonst wird auf 32 Zeichen gekuerzt
		box.setValue(String.valueOf(wert(f)));
		box.setResponder(text -> {
			werte.put(f.key(), text);
			fehler.remove(f.key());
		});
		return box;
	}

	private static Component jaNein(boolean ja) {
		return ja ? Component.literal("Ja").withStyle(ChatFormatting.GREEN) : Component.literal("Nein");
	}

	private Component auswahlText(Feld f) {
		String w = String.valueOf(wert(f)).strip();
		if (w.isEmpty()) {
			return Component.literal("Bitte wählen... ▼").withStyle(ChatFormatting.GRAY);
		}
		return Component.literal(kuerzen(w, feldBreite - 24) + " ▼");
	}

	private int maxScroll() {
		return Math.max(0, inhaltHoehe - (unten - OBEN));
	}

	/** Felder nach dem Scrollen an ihre Stelle setzen. Was nicht ganz in den Bereich passt, wird versteckt. */
	private void positionieren() {
		for (Zeile z : zeilen) {
			int y = OBEN + z.abstand() - scroll;
			z.widget().setY(y);
			z.widget().visible = y >= OBEN && y + 20 <= unten;
		}
	}

	/** Waehrend des Eintragens Knoepfe sperren, damit nichts doppelt eingetragen wird. */
	private void sperren(boolean an) {
		for (Zeile z : zeilen) {
			if (!(z.widget() instanceof EditBox)) {
				z.widget().active = !an;
			}
		}
		for (Button k : knoepfe) {
			k.active = !an;
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		int max = maxScroll();
		if (max > 0 && scrollY != 0) {
			scroll = Math.clamp(scroll - Math.round(scrollY * 20), 0, max);
			positionieren();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	// ---------- Eintragen ----------

	private void eintragen() {
		if (sendet) {
			return;
		}

		// 1) Pflichtfelder pruefen - Datum, Link und Liste prueft danach der Bot genau wie in Discord
		Map<String, String> fehlt = new LinkedHashMap<>();
		JsonObject json = new JsonObject();
		for (Feld f : kategorie.felder()) {
			Object w = wert(f);
			if (w instanceof Boolean ja) {
				json.addProperty(f.key(), ja);
				continue;
			}
			String text = String.valueOf(w);
			if (text.isBlank()) {
				if (f.pflicht()) {
					fehlt.put(f.key(), f.istAuswahl() ? "Bitte auswählen." : "Pflichtfeld - bitte ausfüllen.");
				}
				continue;
			}
			// Auswahl genau so schicken, wie sie im Dropdown steht (auch mit Leerzeichen)
			json.addProperty(f.key(), f.istAuswahl() ? text : text.strip());
		}
		if (!fehlt.isEmpty()) {
			this.minecraft.gui.setScreen(kopie(fehlt, "Bitte die rot markierten Felder ausfüllen."));
			return;
		}

		// 2) An den Bot schicken
		List<Component> zusammenfassung = zusammenfassung();
		sendet = true;
		sperren(true);
		BotApi api = UcmdMax.api();
		api.danach(api.aktiEintragen(kategorie.key(), json), antwort -> {
			sendet = false;
			ENTWUERFE.remove(kategorie.key());
			String titel = BotApi.text(antwort, "titel", kategorie.titel());
			String blatt = BotApi.text(antwort, "blatt", formular.blatt());
			int zeile = BotApi.zahl(antwort, "zeile", 0);
			String url = BotApi.text(antwort, "url", formular.url());
			UcmdMax.chat(Component.literal("✔ " + titel + " eingetragen").withStyle(ChatFormatting.GREEN)
					.append(Component.literal(" - Blatt " + blatt + ", Zeile " + zeile).withStyle(ChatFormatting.GRAY)));
			if (this.minecraft.gui.screen() == this) {
				this.minecraft.gui.setScreen(new ErgebnisScreen(formular, titel, blatt, zeile, url, zusammenfassung));
			}
		}, f -> {
			sendet = false;
			if (f.felder().isEmpty()) {
				UcmdMax.chatFehler("Akti nicht eingetragen: " + f.getMessage());
			}
			if (this.minecraft.gui.screen() == this) {
				this.minecraft.gui.setScreen(kopie(f.felder(), f.getMessage()));
			} else if (!f.felder().isEmpty()) {
				UcmdMax.chatFehler("Akti nicht eingetragen: " + f.getMessage()
						+ " - /ucmdmax newakti öffnet deine Eingaben wieder.");
			}
		});
	}

	private List<Component> zusammenfassung() {
		List<Component> liste = new ArrayList<>();
		for (Feld f : kategorie.felder()) {
			Object w = wert(f);
			String text = w instanceof Boolean ja ? (ja ? "Ja" : "Nein") : String.valueOf(w).strip();
			if (!text.isEmpty()) {
				liste.add(Component.literal(f.label() + ": ").withStyle(ChatFormatting.GRAY)
						.append(Component.literal(kuerzen(text, 220)).withStyle(ChatFormatting.WHITE)));
			}
		}
		return liste;
	}

	// ---------- Zeichnen ----------

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
		super.extractRenderState(g, mouseX, mouseY, delta);
		mitte(g, this.title, 10, GOLD);
		mitte(g, "Blatt: " + formular.blatt(), 22, GRAU);

		for (Zeile z : zeilen) {
			AbstractWidget w = z.widget();
			if (!w.visible) {
				continue;
			}
			Feld f = z.feld();
			int y = w.getY();
			String fehlerText = fehler.get(f.key());
			g.text(this.font, Component.literal(kuerzen(beschriftung(f), labelBreite)), labelX, y + 6,
					fehlerText != null ? ROT : WEISS);
			// Platzhalter (z.B. "https://...") im leeren Textfeld - wie bei Minecraft nur, solange man nicht drin tippt
			if (w instanceof EditBox box && box.getValue().isEmpty() && !box.isFocused() && !f.platzhalter().isEmpty()) {
				g.text(this.font, Component.literal(kuerzen(f.platzhalter(), feldBreite - 8)), box.getX() + 4, y + 6,
						DUNKELGRAU);
			}
			if (fehlerText != null && y + 22 + this.font.lineHeight <= unten) {
				g.text(this.font, Component.literal(kuerzen(fehlerText, this.width - feldX - 6)), feldX, y + 22, ROT);
			}
		}

		// Scrollbalken, wenn nicht alle Felder auf den Bildschirm passen
		int max = maxScroll();
		if (max > 0) {
			int x = Math.min(feldX + feldBreite + 6, this.width - 4);
			int hoehe = unten - OBEN;
			int balken = Math.max(16, hoehe * hoehe / (hoehe + max));
			int pos = OBEN + (hoehe - balken) * scroll / max;
			g.fill(x, OBEN, x + 3, unten, 0x40FFFFFF);
			g.fill(x, pos, x + 3, pos + balken, 0xC0FFFFFF);
		}

		int breite = Math.min(380, this.width - 20);
		String hinweis = aktuelleMeldung();
		if (sendet) {
			mitte(g, "Wird eingetragen...", unten + 6, GRAU);
		} else if (hinweis != null) {
			mitteUmbrochen(g, Component.literal(hinweis), unten + 2, breite, ROT, 2);
		} else if (max > 0) {
			mitte(g, "Mausrad: weitere Felder", unten + 6, DUNKELGRAU);
		}
	}
}
