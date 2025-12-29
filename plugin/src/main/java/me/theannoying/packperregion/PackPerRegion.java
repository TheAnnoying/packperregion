package me.theannoying.packperregion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PackPerRegion extends JavaPlugin {
	private static PackPerRegion plugin;
	public static PackPerRegion getPlugin() { return plugin; }
	public final FileConfiguration config = getConfig();

    public static String packDirectory;
    public static String packListPath;

	@Override
	public void onEnable() {
		saveDefaultConfig();
		saveResource("packs/list.json", false);

        plugin = this;
        packDirectory = getPlugin().getDataFolder().getAbsolutePath() + "/packs/";
        packListPath = packDirectory + "list.json";

        PackServer.startServer(8080);
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