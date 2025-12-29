package me.theannoying.packperregion;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Properties;

import static me.theannoying.packperregion.PackPerRegion.getPlugin;

public class Util {
	public static boolean isWithinRegion(JsonArray firstCoordSet, JsonArray secondCoordSet, Location loc) {
		int minX = Math.min(firstCoordSet.get(0).getAsInt(), secondCoordSet.get(0).getAsInt());
		int minY = Math.min(firstCoordSet.get(1).getAsInt(), secondCoordSet.get(1).getAsInt());
		int minZ = Math.min(firstCoordSet.get(2).getAsInt(), secondCoordSet.get(2).getAsInt());
		int maxX = Math.max(firstCoordSet.get(0).getAsInt(), secondCoordSet.get(0).getAsInt());
		int maxY = Math.max(firstCoordSet.get(1).getAsInt(), secondCoordSet.get(1).getAsInt());
		int maxZ = Math.max(firstCoordSet.get(2).getAsInt(), secondCoordSet.get(2).getAsInt());

		return (loc.getBlockX() >= minX && loc.getBlockX() <= maxX)
				&& (loc.getBlockY() >= minY && loc.getBlockY() <= maxY)
				&& (loc.getBlockZ() >= minZ && loc.getBlockZ() <= maxZ);
	}

	public static String getConfigString(String path) {
		return getPlugin().getConfig().getString(path);
	}

	public static Boolean getConfigBool(String path) {
		return getPlugin().getConfig().getBoolean(path);
	}

	public static JsonObject getJsonObject(String path) {
		JsonObject object = null;
		try {
			object = JsonParser.parseReader(new FileReader(path)).getAsJsonObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return object;
	}

	public static JsonArray getJsonArray(String path) {
		JsonArray array = null;
		try {
			array = JsonParser.parseReader(new FileReader(path)).getAsJsonArray();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return array;
	}

	public static void saveJsonObject(String path, JsonObject json) {
		try (FileWriter file = new FileWriter(path)) {
			file.write(json.toString());
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void saveJsonArray(String path, JsonArray json) {
		try (FileWriter file = new FileWriter(path)) {
			file.write(json.toString());
			file.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String zipToDataURL(String path) {
		String dataURL = null;
		try {
			byte[] zipData = Files.readAllBytes(Paths.get(path));
			String base64Data = Base64.getEncoder().encodeToString(zipData);
			dataURL = "data:application/zip;base64," + base64Data;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return dataURL;
	}

	public static JsonArray getPackList() {
		String path = getPlugin().getDataFolder().getAbsolutePath() + "/packs/list.json";
		if(!(new File(path)).exists()) getPlugin().saveResource("packs/list.json", false);
		return getJsonArray(path);
	}

	public static int getPackIndexBasedOffID(JsonArray packList, String id) {
		int packIndex = -1;
		for (int i = 0; i < packList.size(); i++) {
			JsonObject packObject = packList.get(i).getAsJsonObject();
			if (packObject.get("id").getAsString().equals(id)) packIndex = i;
		}
		return packIndex;
	}
}