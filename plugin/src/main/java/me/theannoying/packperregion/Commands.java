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
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static me.theannoying.packperregion.EnterRegion.*;
import static me.theannoying.packperregion.PackPerRegion.*;
import static me.theannoying.packperregion.Util.*;

public class Commands implements CommandExecutor {
	AtomicInteger taskCounter = new AtomicInteger();
	final int PERIOD = 100;
	final int TIMEOUT = 60 * PERIOD;

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
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

                JsonArray packList = getPackList();
                JsonObject packDataObject = new JsonObject();
                packDataObject.addProperty("id", id);
                packDataObject.addProperty("owner", ((Player) sender).getUniqueId().toString());
                packDataObject.addProperty("pack_status", "Pending Upload");

                JsonArray coordinateArray = getCoordinateArray(args);
                packDataObject.add("coordinates", coordinateArray);

                packList.add(packDataObject);
                saveJsonArray(packListPath, packList);

                component.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, serverURL + "web/upload.html?uuid=" + ((Player) sender).getUniqueId() + "&id=" + id));
                component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.link_hover")))));

                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_upload_site")));
                sender.spigot().sendMessage(component);

                AtomicReference<BukkitTask> timer = new AtomicReference<>();
                timer.set(Bukkit.getScheduler().runTaskTimer(getPlugin(), () -> {
                    JsonArray updatedPackList = getPackList();
                    for (JsonElement el : updatedPackList) {
                        JsonObject obj = el.getAsJsonObject();
                        if (obj.get("id").getAsString().equals(id) && obj.has("pack_name")) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_uploaded_success")));
                            timer.get().cancel();
                            return;
                        }
                    }

                    if (taskCounter.get() * PERIOD >= TIMEOUT) {
                        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.pack_upload_timeout")));

                        File file = new File(packDirectory + id + ".zip");
                        if (file.exists()) file.delete();
                        int packIndex = getPackIndexBasedOffID(packList, id);
                        if (packIndex != -1) packList.remove(packIndex);

                        timer.get().cancel();
                    }
                    taskCounter.getAndIncrement();
                }, 100, PERIOD));
			}
		}

		if (command.getName().equalsIgnoreCase("packperregion")) {
			if (args.length == 0) return false;

			switch (args[0].toLowerCase()) {
				case "reload": {
					getPlugin().reloadConfig();
					sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.config_reloaded")));
					break;
				}
				case "delete": {
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

                            resourcePackApplied.forEach((uuid, id) -> {
								if(id.equals(args[1])) Objects.requireNonNull(Bukkit.getPlayer(uuid)).removeResourcePack(UUID.fromString(args[1]));
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
						packStatuses.put("Approved", packListApproved);

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

                                    String packID = elementObject.get("id").getAsString();
                                    String url = serverURL + "packs/" + packID + ".zip";
                                    String[] parts = getConfigString("messages.packlist_command_response").split("#pack_url", 2);

                                    TextComponent message = new TextComponent(
                                        ChatColor.translateAlternateColorCodes('&',
                                            parts[0]
                                                .replaceAll("#pack_owner", elementObject.get("owner").getAsString())
                                                .replaceAll("#pack_name", elementObject.has("pack_name") ? elementObject.get("pack_name").getAsString() : getConfigString("messages.pack_not_uploaded_yet"))
                                                .replaceAll("#pack_status", elementObject.get("pack_status").getAsString())
                                                .replaceAll("#pack_coordinates", elementObject.get("coordinates").getAsJsonArray().get(0).toString().replaceAll(",", ", ") + " - " + elementObject.get("coordinates").getAsJsonArray().get(1).toString().replaceAll(",", ", "))
                                                .replaceAll("#pack_url", elementObject.has("pack_name") ? url : getConfigString("messages.pack_not_uploaded_yet"))
                                                .replaceAll("#pack_id", packID)
                                        )
                                    );

                                    TextComponent urlComponent = new TextComponent(ChatColor.translateAlternateColorCodes('&', url));
                                    urlComponent.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                                    message.addExtra(urlComponent);

                                    if (parts.length > 1) message.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', parts[1])));
                                    sender.spigot().sendMessage(message);
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
                case "applypack": {
                    applyPack(((Player) sender).getPlayer(), ((Player) sender).getLocation(), true);
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