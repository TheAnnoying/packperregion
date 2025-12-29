package me.theannoying.packperregion;

import com.google.gson.JsonArray;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static me.theannoying.packperregion.Util.getPackList;

public class TabAutocomplete implements TabCompleter {
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> suggestions = new ArrayList<>();

		if(command.getName().equalsIgnoreCase("packperregion")) {
			if(args.length == 1) {
				suggestions.add("reload");
				suggestions.add("list");
				suggestions.add("accept");
				suggestions.add("reject-or-delete");
			}
			if(args.length == 2) {
				JsonArray packList = getPackList();
				if(args[0].equals("accept")) {
					packList.forEach(element -> {
						if(!"Accepted".equals(element.getAsJsonObject().get("pack_status").getAsString())) {
							suggestions.add(element.getAsJsonObject().get("id").getAsString());
						}
					});
				} else if(args[0].equals("reject-or-delete")) {
					packList.forEach(element -> suggestions.add(element.getAsJsonObject().get("id").getAsString()));
				}
			}
		}

		return suggestions;
	}
}
