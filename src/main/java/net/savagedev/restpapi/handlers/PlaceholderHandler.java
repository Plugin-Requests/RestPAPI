package net.savagedev.restpapi.handlers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import me.clip.placeholderapi.PlaceholderAPI;
import net.savagedev.restpapi.RestPapiPlugin;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PlaceholderHandler implements HttpHandler {
    private static final String HYPHENATED_UUID_PATTERN = "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)";

    private static final String METHOD_NOT_ALLOWED = "{\"status\":\"405\",\"message\":\"Method not allowed\"}";
    private static final String PLAYER_NOT_FOUND = "{\"status\":\"404\",\"message\":\"Player not found\"}";
    private static final String UNAUTHORIZED = "{\"status\":\"401\",\"message\":\"Unauthorized\"}";
    private static final String INVALID_URI = "{\"status\":\"400\",\"message\":\"Invalid URI\"}";
    private static final String PLACEHOLDER = "{\"status\":\"200\",\"message\":\"%s\"}";

    private final RestPapiPlugin plugin;

    public PlaceholderHandler(RestPapiPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("GET")) {
            exchange.sendResponseHeaders(405, METHOD_NOT_ALLOWED.length());
            this.writeString(exchange.getResponseBody(), METHOD_NOT_ALLOWED);
            return;
        }

        exchange.getResponseHeaders().set("Content-Type", "application/json");

        if (this.isUnauthorized(exchange)) {
            return;
        }

        final String[] pathParts = exchange.getRequestURI().getPath().substring(1).split("/");
        if (pathParts.length < 2) {
            exchange.sendResponseHeaders(400, INVALID_URI.length());
            this.writeString(exchange.getResponseBody(), INVALID_URI);
            return;
        }

        final UUID playerUuid = this.tryParseUuid(pathParts[0]);
        if (playerUuid == null) {
            exchange.sendResponseHeaders(400, INVALID_URI.length());
            this.writeString(exchange.getResponseBody(), INVALID_URI);
            return;
        }

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
        if (!offlinePlayer.hasPlayedBefore()) {
            exchange.sendResponseHeaders(404, PLAYER_NOT_FOUND.length());
            this.writeString(exchange.getResponseBody(), PLAYER_NOT_FOUND);
            return;
        }

        final String response = String.format(PLACEHOLDER,
                PlaceholderAPI.setPlaceholders(offlinePlayer, "%" + pathParts[1] + "%")
        );
        exchange.sendResponseHeaders(200, response.length());
        this.writeString(exchange.getResponseBody(), response);
    }

    // This will handle responding to the unauthorized request.
    private boolean isUnauthorized(HttpExchange exchange) throws IOException {
        final String accessToken = exchange.getRequestHeaders().getFirst("Authorization");
        final boolean unauthorized = !this.plugin.getConfig().getStringList("access-tokens")
                .contains(accessToken);
        if (unauthorized) {
            this.plugin.getLogger().info("Unauthorized request from " + exchange.getRemoteAddress().getHostString() + " (" + accessToken + ")");
            exchange.sendResponseHeaders(401, UNAUTHORIZED.length());
            this.writeString(exchange.getResponseBody(), UNAUTHORIZED);
        }
        return unauthorized;
    }

    // Will try to parse a UUID from a String with or without hyphens.
    private UUID tryParseUuid(String uuidString) {
        UUID uuid = null;
        try {
            uuid = UUID.fromString(uuidString);
        } catch (IllegalArgumentException ignored) {
            try {
                uuid = UUID.fromString(uuidString
                        .replaceFirst(HYPHENATED_UUID_PATTERN, "$1-$2-$3-$4-$5"));
            } catch (IllegalArgumentException ignore) {
            }
        }
        return uuid;
    }

    private void writeString(OutputStream outputStream, String response) {
        try (outputStream) {
            outputStream.write(response.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            this.plugin.getLogger().warning("Failed to write HTTP response: " + e.getMessage());
        }
    }
}
