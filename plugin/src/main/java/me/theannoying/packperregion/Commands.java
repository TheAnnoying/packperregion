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

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.theannoying.packperregion.EnterRegion.getRegionEntered;
import static me.theannoying.packperregion.PackPerRegion.getPlugin;
import static me.theannoying.packperregion.PackPerRegion.packDirectory;
import static me.theannoying.packperregion.PackPerRegion.packListPath;
import static me.theannoying.packperregion.Util.*;

public class Commands implements CommandExecutor {
	private final String serverURL = "http://localhost:8080/";
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
				String id = UUID.randomUUID().toString();
                TextComponent component = new TextComponent(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pressable_link_text")));

                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "http://localhost:5173/" + "?uuid=" + ((Player) sender).getUniqueId() + "&id=" + id));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.link_hover")))));

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_upload_site")));
                sender.spigot().sendMessage(component);

                AtomicReference<BukkitTask> timer = new AtomicReference<>();
                timer.set(Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
                    JsonArray packList = getPackList();
                    for (JsonElement el : packList) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.get("id").getAsString().equals(id)) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_uploaded_success")));

                            obj.addProperty("owner", ((Player) sender).getUniqueId().toString());
                            obj.addProperty("pack_url", serverURL + "packs/" + id + ".zip");
                            obj.addProperty("pack_status", "Pending Approval");

                            JsonArray coordinateArray = getCoordinateArray(args);
                            obj.add("coordinates", coordinateArray);

                            saveJsonArray(packListPath, packList);
                            timer.get().cancel();
                            return;
                        }
                    }

                    if (taskCounter.get() * PERIOD >= TIMEOUT) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_upload_timeout")));

                        File file = new File(packDirectory + id + ".zip");
                        if (file.exists()) file.delete();

                        timer.get().cancel();
                    }
                    taskCounter.getAndIncrement();
                }, 100, PERIOD));
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
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_id_provided")));
						return true;
					} else {
						JsonArray packList = getPackList();
						int packIndex = getPackIndexBasedOffID(packList, args[1]);
						if (packIndex != -1) {
							packList.remove(packIndex);
							saveJsonArray(packListPath, packList);
                            File file = new File(packDirectory + args[1] + ".zip");
                            if (file.exists()) file.delete();

							getRegionEntered().forEach((uuid, id) -> {
								if(id.equals(args[1])) Bukkit.getPlayer(uuid).removeResourcePack(UUID.fromString(args[1]));
							});

                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_delete_success")));
						} else {
							sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_pack_found")));
						}
					}
					break;
				}
				case "list": {
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
															.replaceAll("#pack_id", elementObject.get("id").getAsString())
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
						sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.no_id_provided")));
						return true;
					} else {
						JsonArray packList = getPackList();
						int packIndex = getPackIndexBasedOffID(packList, args[1]);
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