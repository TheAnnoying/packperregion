package me.theannoying.packperregion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static me.theannoying.packperregion.Util.*;

public final class PackPerRegion extends JavaPlugin {
	private static PackPerRegion plugin;
	public static PackPerRegion getPlugin() { return plugin; }
	public final FileConfiguration config = getConfig();

    public static String pluginPath;
    public static String packDirectory;
    public static String packListPath;
    public static String serverURL;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		saveResource("packs/list.json", false);
        saveResource("web/upload.html", false);

        plugin = this;

        pluginPath = getPlugin().getDataFolder().getAbsolutePath();
        packDirectory = pluginPath + "/packs/";
        packListPath = packDirectory + "list.json";

		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create("http://checkip.amazonaws.com")).GET().build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			serverURL = "https://" + response.body().trim() + ":" + getConfigInt("settings.port") + "/";
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

        PackServer.startServer(getConfigInt("settings.port"));
        getServer().getLogger().info("Enabling...");

		getCommand("registerarea").setExecutor(new Commands());
		getCommand("packperregion").setExecutor(new Commands());
		getCommand("packperregion").setTabCompleter(new TabAutocomplete());

		getServer().getPluginManager().registerEvents(new EnterRegion(), this);
	}

	@Override
	public void onDisable() {
		getServer().getLogger().info("Disabling...");
	}
}