package net.savagedev.restpapi;

import com.sun.net.httpserver.HttpServer;
import net.savagedev.restpapi.commands.RestPapiCommand;
import net.savagedev.restpapi.handlers.PlaceholderHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.UUID;

public class RestPapiPlugin extends JavaPlugin {
    // No point for this variable... I just wanted an excuse to say "stop(NOW)"
    private static final int NOW = 0;

    private HttpServer httpServer;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.genAccessToken(true);
        this.initCommands();
        this.setupHttpServer();
        this.httpServer.start();
        this.getLogger().info("HTTP server listening on " + this.httpServer.getAddress().toString());
    }

    @Override
    public void onDisable() {
        this.httpServer.stop(this.getConfig().getInt("shutdown-delay"));
    }

    public void restartHttpServer() {
        this.getLogger().info("Stopping HTTP server...");
        this.httpServer.stop(NOW);
        this.setupHttpServer();
        this.httpServer.start();
        this.getLogger().info("HTTP server listening on " + this.httpServer.getAddress().toString());
    }

    private void initCommands() {
        this.getCommand("restpapi").setExecutor(new RestPapiCommand(this));
    }

    private void setupHttpServer() {
        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(
                    this.getConfig().getString("address", "0.0.0.0"),
                    this.getConfig().getInt("port", 3000)
            ), 0);
        } catch (IOException e) {
            this.getLogger().warning("Failed to setup HTTP server: " + e.getMessage());
        }
        this.httpServer.createContext("/", new PlaceholderHandler(this));
    }

    public String genAccessToken(boolean firstToken) {
        final List<String> accessTokens = this.getConfig().getStringList("access-tokens");
        final String token = UUID.randomUUID().toString();
        if (accessTokens.isEmpty() || !firstToken) {
            accessTokens.add(token);
        }
        this.getConfig().set("access-tokens", accessTokens);
        this.saveConfig();
        return token;
    }
}