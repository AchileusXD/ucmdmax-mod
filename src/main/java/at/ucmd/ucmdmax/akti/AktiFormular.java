package at.ucmd.ucmdmax.akti;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import at.ucmd.ucmdmax.api.BotApi;

/**
 * Was der Bot unter /api/v1/akti/formular schickt: das eigene Akti-Blatt und alle Kategorien
 * mit ihren Feldern. Die Felder kommen 1:1 aus AKTI_KATEGORIEN in sheets_service.py -
 * aendert man dort etwas, zeigt die Mod es automatisch an.
 */
public record AktiFormular(String blatt, String url, String heute, List<Kategorie> kategorien) {

	/** z.B. "roleplay" / "Roleplay-Akti". {@code fehlt} = Dropdowns, die im Blatt fehlen. */
	public record Kategorie(String key, String titel, List<Feld> felder, List<String> fehlt) {
		public boolean nutzbar() {
			return fehlt.isEmpty();
		}
	}

	/** Typ: text | lang | datum | link | auswahl | janein (unbekannte Typen werden wie Text behandelt). */
	public record Feld(String key, String label, String typ, boolean pflicht, String platzhalter, int max,
			List<String> optionen) {
		public boolean istAuswahl() {
			return "auswahl".equals(typ);
		}

		public boolean istJaNein() {
			return "janein".equals(typ);
		}

		public boolean istDatum() {
			return "datum".equals(typ);
		}
	}

	public static AktiFormular aus(JsonObject json) {
		List<Kategorie> kategorien = new ArrayList<>();
		for (JsonObject k : objekte(json.get("kategorien"))) {
			List<Feld> felder = new ArrayList<>();
			for (JsonObject f : objekte(k.get("felder"))) {
				JsonElement pflicht = f.get("pflicht");
				felder.add(new Feld(
						BotApi.text(f, "key", ""),
						BotApi.text(f, "label", "?"),
						BotApi.text(f, "typ", "text"),
						pflicht == null || !pflicht.isJsonPrimitive() || pflicht.getAsBoolean(),
						BotApi.text(f, "platzhalter", ""),
						Math.max(1, BotApi.zahl(f, "max", 100)),
						texte(f.get("optionen"))));
			}
			kategorien.add(new Kategorie(BotApi.text(k, "key", ""), BotApi.text(k, "titel", "?"),
					List.copyOf(felder), texte(k.get("fehlt"))));
		}
		return new AktiFormular(BotApi.text(json, "blatt", "?"), BotApi.text(json, "url", ""),
				BotApi.text(json, "heute", ""), List.copyOf(kategorien));
	}

	private static List<JsonObject> objekte(JsonElement e) {
		List<JsonObject> liste = new ArrayList<>();
		if (e != null && e.isJsonArray()) {
			for (JsonElement x : (JsonArray) e) {
				if (x.isJsonObject()) {
					liste.add(x.getAsJsonObject());
				}
			}
		}
		return liste;
	}

	private static List<String> texte(JsonElement e) {
		List<String> liste = new ArrayList<>();
		if (e != null && e.isJsonArray()) {
			for (JsonElement x : (JsonArray) e) {
				if (x.isJsonPrimitive()) {
					liste.add(x.getAsString());
				}
			}
		}
		return List.copyOf(liste);
	}
}
