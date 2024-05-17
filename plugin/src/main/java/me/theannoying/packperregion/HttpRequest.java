package me.theannoying.packperregion;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import static me.theannoying.packperregion.Util.getConfigString;

public class HttpRequest {
	private final String url;
	private final String requestMethod;

	public HttpRequest(String url, String requestMethod) {
		this.url = url;
		this.requestMethod = requestMethod;
	}

	public JsonObject execute(CommandSender sender) {
		try {
			URL reqURL = new URL(url);

			HttpURLConnection conn = (HttpURLConnection) reqURL.openConnection();
			conn.setRequestMethod(requestMethod);
			conn.connect();

			int responseCode = conn.getResponseCode();

			if(responseCode != 200) {
				throw new RuntimeException("HttpResponseCode: " + responseCode);
			} else {
				StringBuilder informationString = new StringBuilder();
				Scanner scanner = new Scanner(conn.getInputStream());

				while (scanner.hasNext()) {
					informationString.append(scanner.nextLine());
				}

				scanner.close();
				return JsonParser.parseString(informationString.toString()).getAsJsonObject();
			}
		} catch (Exception e) {
			if(sender != null) sender.sendMessage(ChatColor.translateAlternateColorCodes('&', getConfigString("messages.error_occurred")));
			e.printStackTrace();
		}

		return null;
	}

	public JsonObject execute() {
		return this.execute(null);
	}
}