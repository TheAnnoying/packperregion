package me.theannoying.packperregion;

import com.google.gson.JsonArray;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;

import static me.theannoying.packperregion.Util.*;

public class EnterRegion implements Listener {
	private static final HashMap<UUID, String> regionEntered = new HashMap<>();
	private void applyPack(Player player, Location playerLocation) {
		getPackList().forEach(pack -> {
			if (pack.getAsJsonObject().get("pack_status").getAsString().equals("Accepted")) {
				JsonArray coordArray = pack.getAsJsonObject().get("coordinates").getAsJsonArray();
				boolean isWithinRegion = isWithinRegion(coordArray.get(0).getAsJsonArray(), coordArray.get(1).getAsJsonArray(), playerLocation);
				String id = pack.getAsJsonObject().get("id").getAsString();

				if (!id.equals(regionEntered.get(player.getUniqueId())) && isWithinRegion) {
					regionEntered.put(player.getUniqueId(), id);
					player.addResourcePack(UUID.fromString(pack.getAsJsonObject().get("id").getAsString()), pack.getAsJsonObject().get("pack_url").getAsString(), null, null, getConfigBool("settings.make_region_packs_optional"));
				} else if(id.equals(regionEntered.get(player.getUniqueId())) && !isWithinRegion) {
					regionEntered.remove(player.getUniqueId());
					player.removeResourcePack(UUID.fromString(pack.getAsJsonObject().get("id").getAsString()));
				}
			}
		});
	}
	public static HashMap<UUID, String> getRegionEntered() {
		return regionEntered;
	}

	@EventHandler
	public void onEnterRegion(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location to = event.getTo();

		applyPack(player, to);
	}

	@EventHandler
	public void onServerLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Location location = player.getLocation();

		applyPack(player, location);
	}

	@EventHandler
	public void onServerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Location location = player.getLocation();

		applyPack(player, location);
	}
}