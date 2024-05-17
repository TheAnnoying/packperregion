package me.theannoying.packperregion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class PackPerRegion extends JavaPlugin {
	private static PackPerRegion plugin;
	public static PackPerRegion getPlugin() { return plugin; }
	public final FileConfiguration config = getConfig();

	@Override
	public void onEnable() {
		saveDefaultConfig();
		saveResource("packs/list.json", false);

		plugin = this;
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