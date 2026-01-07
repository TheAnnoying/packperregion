package me.theannoying.packperregion;

import com.google.gson.JsonArray;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
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
    public static final HashMap<UUID, String> resourcePackApplied = new HashMap<>();
	public static final HashMap<UUID, String> regionEntered = new HashMap<>();

	public static void applyPack(Player player, Location playerLocation, boolean force) {
		getPackList().forEach(pack -> {
			if (pack.getAsJsonObject().get("pack_status").getAsString().equals("Accepted")) {
				JsonArray coordArray = pack.getAsJsonObject().get("coordinates").getAsJsonArray();
                String id = pack.getAsJsonObject().get("id").getAsString();

				boolean isWithinRegion = isWithinRegion(coordArray.get(0).getAsJsonArray(), coordArray.get(1).getAsJsonArray(), playerLocation);
                boolean hasEnteredRegion = id.equals(regionEntered.get(player.getUniqueId()));
                boolean hasResourcePackApplied = id.equals(resourcePackApplied.get(player.getUniqueId()));

				if (isWithinRegion && (!hasEnteredRegion || (force && !hasResourcePackApplied))) {
                    regionEntered.put(player.getUniqueId(), id);
                    if(getConfigBool("settings.make_region_packs_required") || force) {
                        resourcePackApplied.put(player.getUniqueId(), id);
                        player.addResourcePack(UUID.fromString(pack.getAsJsonObject().get("id").getAsString()), pack.getAsJsonObject().get("pack_url").getAsString(), null, null, getConfigBool("settings.make_region_packs_required"));
                    } else {
                        TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.request_allow_pack_clickable")));
                        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/packperregion applypack"));

                        player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.request_allow_pack")));
                        player.spigot().sendMessage(component);
                    }
				} else if(hasEnteredRegion && !isWithinRegion) {
                    regionEntered.remove(player.getUniqueId());
                    resourcePackApplied.remove(player.getUniqueId());
					player.removeResourcePack(UUID.fromString(pack.getAsJsonObject().get("id").getAsString()));
				} else if(force && !isWithinRegion) {
                    player.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.unable_to_apply_pack")));
                }
			}
		});
	}

	@EventHandler
	public void onEnterRegion(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location to = event.getTo();

        applyPack(player, to, false);
	}

	@EventHandler
	public void onServerLeave(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Location location = player.getLocation();

        applyPack(player, location, false);
	}

	@EventHandler
	public void onServerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Location location = player.getLocation();

        applyPack(player, location, false);
	}
}