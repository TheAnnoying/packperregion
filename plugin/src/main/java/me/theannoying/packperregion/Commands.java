package me.theannoying.packperregion;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.theannoying.packperregion.EnterRegion.getRegionEntered;
import static me.theannoying.packperregion.PackPerRegion.getPlugin;
import static me.theannoying.packperregion.Util.*;

public class Commands implements CommandExecutor {
	final String packListPath = getPlugin().getDataFolder().getAbsolutePath() + "/packs/list.json";
	final String serverURL = "http://localhost:8080/";
	AtomicInteger taskCounter = new AtomicInteger();
	final int PERIOD = 100;
	final int TIMEOUT = 60 * PERIOD;

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "A player is required to run this command!");
			return true;
		}

		if (command.getName().equalsIgnoreCase("registerarea")) {
			if (args.length == 0) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_coordinates_specified")));
			} else if (Arrays.stream(args).anyMatch(s -> !s.matches("-?(\\d+\\.)?\\d+"))) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.coordinates_must_be_numbers")));
			} else if (args.length != 6) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.not_enough_coordinates_specified")));
			} else {
				JsonObject authToken = new HttpRequest(serverURL + "gettoken?uuid=" + ((Player) sender).getUniqueId(), "GET").execute(sender);
				if (authToken != null) {
					TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pressable_link_text")));

					component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://localhost:5173/" + "?uuid=" + ((Player) sender).getUniqueId() + "&token=" + authToken.get("token").getAsString()));
					component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.link_hover")))));

					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_upload_site")));
					sender.spigot().sendMessage(component);

					AtomicReference<BukkitTask> timer = new AtomicReference<>();
					timer.set(Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
						JsonObject packData = new HttpRequest(serverURL + "getpackdata?token=" + authToken.get("token").getAsString(), "GET").execute(sender);
						JsonElement packName = packData.get("pack_name");

						if (!(packName == null || packName.isJsonNull() || packName.getAsString().isEmpty())) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_uploaded_success")));

							JsonObject packDataObject = new JsonObject();
							packDataObject.addProperty("owner", sender.getName());
							packDataObject.addProperty("token", authToken.get("token").getAsString());
							packDataObject.addProperty("pack_name", packName.getAsString());
							packDataObject.addProperty("pack_url", serverURL + "packs/" + authToken.get("token").getAsString() + ".zip");
							packDataObject.addProperty("pack_status", "Pending Approval");

							JsonArray coordinateArray = getCoordinateArray(args);
							packDataObject.add("coordinates", coordinateArray);

							JsonArray packList = getPackList();
							packList.add(packDataObject);
							saveJsonArray(packListPath, packList);

							timer.get().cancel();
						}

						if (taskCounter.get() * PERIOD >= TIMEOUT) {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_upload_timeout")));
							new HttpRequest(serverURL + "deletepack?token=" + authToken.get("token").getAsString(), "DELETE").execute();
							timer.get().cancel();
						}
						taskCounter.getAndIncrement();
					}, 100, PERIOD));
				}
			}
		}

		if (command.getName().equalsIgnoreCase("packperregion")) {
			if (args.length == 0) return false;

			switch (args[0]) {
				case "reload": {
					getPlugin().reloadConfig();
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.config_reloaded")));
					break;
				}
				case "reject-or-delete": {
					if (args.length == 1) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_token_provided")));
						return true;
					} else {
						JsonArray packList = getPackList();
						int packIndex = getPackIndexBasedOffToken(packList, args[1]);
						if (packIndex != -1) {
							packList.remove(packIndex);
							saveJsonArray(packListPath, packList);

							getRegionEntered().forEach((uuid, token) -> {
								if(token.equals(args[1])) Bukkit.getPlayer(uuid).removeResourcePack(UUID.fromString(args[1]));
							});

							JsonObject deleteRequest = new HttpRequest(serverURL + "deletepack?token=" + args[1], "DELETE").execute();
							if (deleteRequest.get("success").getAsBoolean()) {
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_delete_success")));
							} else {
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_delete_error")));
							}
						} else {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_pack_found")));
						}
					}
					break;
				}
				case "packlist": {
					JsonArray packList = getPackList();
					List<JsonObject> packListPendingApproval = new ArrayList<>();
					List<JsonObject> packListApproved = new ArrayList<>();

					packList.forEach(element -> {
						Map<String, List<JsonObject>> packStatuses = new HashMap<>();
						packStatuses.put("Pending Approval", packListPendingApproval);
						packStatuses.put("Accepted", packListApproved);

						packStatuses.get(element.getAsJsonObject().get("pack_status").getAsString()).add(element.getAsJsonObject());
					});

					List<List<JsonObject>> packLists = new ArrayList<>();
					packLists.add(packListPendingApproval);
					packLists.add(packListApproved);

					if (packList.isEmpty()) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_packs")));
					} else {
						packLists.forEach(list -> {
							if (!list.isEmpty()) {
								list.forEach(element -> {
									JsonObject elementObject = element.getAsJsonObject();
									sender.sendMessage(
											ChatColor.translateAlternateColorCodes('&',
													getConfigString("messages.packlist_command_response")
															.replaceAll("#pack_owner", elementObject.get("owner").getAsString())
															.replaceAll("#pack_name", elementObject.get("pack_name").getAsString())
															.replaceAll("#pack_status", elementObject.get("pack_status").getAsString())
															.replaceAll("#pack_coordinates", elementObject.get("coordinates").getAsJsonArray().get(0).toString().replaceAll(",", ", ") + " - " + elementObject.get("coordinates").getAsJsonArray().get(1).toString().replaceAll(",", ", "))
															.replaceAll("#pack_url", elementObject.get("pack_url").getAsString())
															.replaceAll("#pack_token", elementObject.get("token").getAsString())
											)
									);
								});
							}
						});
					}
					break;
					}
				case "accept": {
					if (args.length == 1) {
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_token_provided")));
						return true;
					} else {
						JsonArray packList = getPackList();
						int packIndex = getPackIndexBasedOffToken(packList, args[1]);
						if (packIndex != -1) {
							JsonObject pack = packList.get(packIndex).getAsJsonObject();
							if("Accepted".equals(pack.get("pack_status").getAsString())) {
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_accept_already_accepted")));
							} else {
								pack.addProperty("pack_status", "Accepted");
								saveJsonArray(packListPath, packList);
								sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_accept_success")));
							}
						} else {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_pack_found")));
						}
					}
					break;
				}
			}
		}
		return true;
	}

	private static JsonArray getCoordinateArray(String[] args) {
		JsonArray coordinateArray = new JsonArray();

		JsonArray firstCoordinateSet = new JsonArray();
		firstCoordinateSet.add(Integer.parseInt(args[0]));
		firstCoordinateSet.add(Integer.parseInt(args[1]));
		firstCoordinateSet.add(Integer.parseInt(args[2]));

		JsonArray secondCoordinateSet = new JsonArray();
		secondCoordinateSet.add(Integer.parseInt(args[3]));
		secondCoordinateSet.add(Integer.parseInt(args[4]));
		secondCoordinateSet.add(Integer.parseInt(args[5]));

		coordinateArray.add(firstCoordinateSet);
		coordinateArray.add(secondCoordinateSet);
		return coordinateArray;
	}
}