package at.ucmd.ucmdmax;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import net.fabricmc.loader.api.FabricLoader;

/**
 * Einstellungen in .minecraft/config/ucmdmax.json:
 * <pre>
 * {
 *   "apiUrl": "http://130.61.21.127:8095",   // Adresse des Bots
 *   "token": "..."                            // geheimer Schluessel nach /ucmdmax verbinden - nicht weitergeben!
 * }
 * </pre>
 */
public final class UcmdKonfig {
	/** Adresse des Bots auf dem Oracle-Server. Ingame aenderbar mit /ucmdmax server &lt;Adresse&gt;. */
	public static final String STANDARD_URL = "http://130.61.21.127:8095";

	private static final Path DATEI = FabricLoader.getInstance().getConfigDir().resolve("ucmdmax.json");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

	// Werden von Gson gelesen und geschrieben
	private String apiUrl = STANDARD_URL;
	private String token = "";
	private String letzteKategorie = "";

	public static UcmdKonfig laden() {
		if (Files.exists(DATEI)) {
			try (Reader leser = Files.newBufferedReader(DATEI, StandardCharsets.UTF_8)) {
				UcmdKonfig konfig = GSON.fromJson(leser, UcmdKonfig.class);
				if (konfig != null) {
					if (konfig.apiUrl == null || konfig.apiUrl.isBlank()) {
						konfig.apiUrl = STANDARD_URL;
					}
					if (konfig.token == null) {
						konfig.token = "";
					}
					if (konfig.letzteKategorie == null) {
						konfig.letzteKategorie = "";
					}
					return konfig;
				}
			} catch (IOException | JsonParseException e) {
				UcmdMax.LOG.warn("config/ucmdmax.json konnte nicht gelesen werden, nehme Standardwerte", e);
			}
		}
		UcmdKonfig konfig = new UcmdKonfig();
		konfig.speichern();
		return konfig;
	}

	public synchronized void speichern() {
		try {
			Files.createDirectories(DATEI.getParent());
			Path tmp = DATEI.resolveSibling("ucmdmax.json.tmp");
			Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, DATEI, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, DATEI, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			UcmdMax.LOG.error("config/ucmdmax.json konnte nicht gespeichert werden", e);
		}
	}

	public synchronized String apiUrl() {
		return apiUrl;
	}

	public synchronized String token() {
		return token;
	}

	public synchronized boolean verbunden() {
		return !token.isBlank();
	}

	/** Key der zuletzt gewaehlten Akti-Kategorie (z.B. "roleplay"), leer = noch keine. */
	public synchronized String letzteKategorie() {
		return letzteKategorie;
	}

	public synchronized void setLetzteKategorie(String kategorie) {
		if (kategorie != null && !kategorie.equals(letzteKategorie)) {
			letzteKategorie = kategorie;
			speichern();
		}
	}

	public synchronized void setToken(String neuerToken) {
		token = neuerToken == null ? "" : neuerToken;
		speichern();
	}

	/** Neue Bot-Adresse. Der alte Token wird dabei geloescht, damit er nie an eine fremde Adresse geht. */
	public synchronized void setApiUrl(String url) {
		apiUrl = url;
		token = "";
		speichern();
	}
}
