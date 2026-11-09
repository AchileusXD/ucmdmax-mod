package at.ucmd.ucmdmax.api;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;

import at.ucmd.ucmdmax.UcmdKonfig;
import at.ucmd.ucmdmax.akti.AktiFormular;

/**
 * Spricht per HTTP mit dem UCMD-Bot (mod_api.py). Alle Anfragen laufen im Hintergrund,
 * damit Minecraft nicht haengt. Ergebnisse mit {@link #danach} wieder im Spiel-Thread verarbeiten.
 */
public final class BotApi {
	private static final Gson GSON = new Gson();
	private static final String NICHT_VERBUNDEN = "Die Mod ist noch nicht mit Discord verbunden. Hol dir in Discord "
			+ "mit /modverbinden einen Code und gib hier /ucmdmax verbinden <Code> ein.";

	private final UcmdKonfig konfig;
	private final String userAgent;
	private final HttpClient http = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NEVER) // den Token nie an eine andere Adresse weiterreichen
			.build();

	public BotApi(UcmdKonfig konfig, String version) {
		this.konfig = konfig;
		this.userAgent = "ucmdmax/" + version;
	}

	// ---------- Endpunkte (siehe mod_api.py im Bot) ----------

	public CompletableFuture<JsonObject> verbinden(String code, String mcName, String mcUuid) {
		JsonObject body = new JsonObject();
		body.addProperty("code", code);
		body.addProperty("mc_name", mcName);
		body.addProperty("mc_uuid", mcUuid);
		return senden("POST", "/api/v1/verbinden", body, false);
	}

	public CompletableFuture<JsonObject> ich() {
		return senden("GET", "/api/v1/ich", null, true);
	}

	public CompletableFuture<JsonObject> trennen() {
		return senden("POST", "/api/v1/trennen", new JsonObject(), true);
	}

	public CompletableFuture<AktiFormular> aktiFormular() {
		return senden("GET", "/api/v1/akti/formular", null, true).thenApply(AktiFormular::aus);
	}

	public CompletableFuture<JsonObject> aktiEintragen(String kategorie, JsonObject werte) {
		JsonObject body = new JsonObject();
		body.addProperty("kategorie", kategorie);
		body.add("werte", werte);
		return senden("POST", "/api/v1/akti", body, true);
	}

	/**
	 * Wartet im Hintergrund auf die Antwort und ruft dann {@code ok} oder {@code fehler}
	 * im Minecraft-Hauptthread auf (nur dort darf man GUI und Chat anfassen).
	 */
	public <T> void danach(CompletableFuture<T> anfrage, Consumer<T> ok, Consumer<ApiFehler> fehler) {
		String adresse = konfig.apiUrl();
		anfrage.whenComplete((wert, ausnahme) -> Minecraft.getInstance().execute(() -> {
			if (ausnahme == null) {
				ok.accept(wert);
			} else {
				fehler.accept(ApiFehler.aus(ausnahme, adresse));
			}
		}));
	}

	// ---------- intern ----------

	private CompletableFuture<JsonObject> senden(String methode, String pfad, JsonObject body, boolean mitToken) {
		String token = konfig.token();
		if (mitToken && token.isBlank()) {
			return CompletableFuture.failedFuture(new ApiFehler(401, NICHT_VERBUNDEN));
		}
		HttpRequest anfrage;
		try {
			HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(konfig.apiUrl().replaceAll("/+$", "") + pfad))
					.timeout(Duration.ofSeconds(30))
					.header("Accept", "application/json")
					.header("User-Agent", userAgent);
			if (mitToken) {
				builder.header("Authorization", "Bearer " + token);
			}
			if (body != null) {
				builder.header("Content-Type", "application/json; charset=utf-8")
						.method(methode, HttpRequest.BodyPublishers.ofString(GSON.toJson(body), StandardCharsets.UTF_8));
			} else {
				builder.method(methode, HttpRequest.BodyPublishers.noBody());
			}
			anfrage = builder.build();
		} catch (IllegalArgumentException e) { // z.B. Adresse ohne http:// oder mit Leerzeichen
			return CompletableFuture.failedFuture(new ApiFehler(0, "Ungültige Bot-Adresse: " + konfig.apiUrl()
					+ " - ändern mit /ucmdmax server <Adresse>"));
		}
		return http.sendAsync(anfrage, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
				.thenApply(BotApi::auswerten);
	}

	private static JsonObject auswerten(HttpResponse<String> antwort) {
		int status = antwort.statusCode();
		JsonObject json = null;
		try {
			JsonElement element = JsonParser.parseString(antwort.body());
			if (element.isJsonObject()) {
				json = element.getAsJsonObject();
			}
		} catch (JsonParseException e) {
			// bleibt null
		}
		if (json == null) {
			throw new ApiFehler(status, "Unerwartete Antwort vom UCMD-Bot (HTTP " + status + ").");
		}

		JsonElement ok = json.get("ok");
		boolean erfolgreich = ok != null && ok.isJsonPrimitive() && ok.getAsJsonPrimitive().isBoolean() && ok.getAsBoolean();
		if (status >= 400 || !erfolgreich) {
			Map<String, String> felder = new HashMap<>();
			JsonElement roh = json.get("felder");
			if (roh != null && roh.isJsonObject()) {
				for (Map.Entry<String, JsonElement> e : roh.getAsJsonObject().entrySet()) {
					if (e.getValue().isJsonPrimitive()) {
						felder.put(e.getKey(), e.getValue().getAsString());
					}
				}
			}
			throw new ApiFehler(status, text(json, "fehler", "Unbekannter Fehler (HTTP " + status + ")."), felder);
		}
		return json;
	}

	/** Text aus dem JSON lesen, {@code standard} wenn es fehlt oder null ist. */
	public static String text(JsonObject json, String key, String standard) {
		JsonElement e = json.get(key);
		return e != null && e.isJsonPrimitive() ? e.getAsString() : standard;
	}

	public static int zahl(JsonObject json, String key, int standard) {
		JsonElement e = json.get(key);
		return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : standard;
	}
}
