package at.ucmd.ucmdmax.api;

import java.net.ConnectException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;

import at.ucmd.ucmdmax.UcmdMax;

/**
 * Fehler bei einer Anfrage an den Bot - mit einer Meldung, die man direkt anzeigen kann.
 *
 * <p>{@code felder} enthaelt Fehler zu einzelnen Formularfeldern (Feld-Key -> Meldung),
 * z.B. {"datum": "Das Datum liegt in der Zukunft."}.
 */
public final class ApiFehler extends RuntimeException {
	private final int status;
	private final Map<String, String> felder;

	public ApiFehler(int status, String text, Map<String, String> felder) {
		super(text, null, false, false);
		this.status = status;
		this.felder = felder == null ? Map.of() : Map.copyOf(felder);
	}

	public ApiFehler(int status, String text) {
		this(status, text, null);
	}

	/** HTTP-Status vom Bot, 0 = Bot gar nicht erreicht. */
	public int status() {
		return status;
	}

	public Map<String, String> felder() {
		return felder;
	}

	/** Macht aus jeder Ausnahme (Netzwerk, Zeitueberschreitung, ...) eine verstaendliche Meldung. */
	public static ApiFehler aus(Throwable t, String adresse) {
		while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
			t = t.getCause();
		}
		if (t instanceof ApiFehler fehler) {
			return fehler;
		}
		if (t instanceof HttpConnectTimeoutException || t instanceof ConnectException) {
			return new ApiFehler(0, "Der UCMD-Bot ist nicht erreichbar (" + adresse + "). "
					+ "Läuft der Bot? Stimmt die Adresse? (/ucmdmax server)");
		}
		if (t instanceof HttpTimeoutException) {
			return new ApiFehler(0, "Der UCMD-Bot antwortet nicht (Zeitüberschreitung). Bitte später nochmal versuchen.");
		}
		UcmdMax.LOG.warn("Unerwarteter Fehler bei der Anfrage an den Bot", t);
		String text = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
		return new ApiFehler(0, "Verbindung zum UCMD-Bot fehlgeschlagen: " + text);
	}
}
