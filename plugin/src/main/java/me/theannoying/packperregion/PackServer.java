package me.theannoying.packperregion;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import com.google.gson.*;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import io.javalin.plugin.bundled.CorsPluginConfig;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

import static me.theannoying.packperregion.PackPerRegion.*;
import static me.theannoying.packperregion.Util.*;

public class PackServer {
    public static void startServer(int port) {
        Gson gson = new GsonBuilder().create();
        JsonMapper gsonMapper = new JsonMapper() {
            @NotNull
            @Override
            public String toJsonString(@NotNull Object obj, @NotNull Type type) {
                return gson.toJson(obj, type);
            }

            @NotNull
            @Override
            public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
                return gson.fromJson(json, targetType);
            }
        };

        Javalin app = Javalin.create(config -> {
            config.jsonMapper(gsonMapper);
            config.bundledPlugins.enableCors(cors -> {
                cors.addRule(CorsPluginConfig.CorsRule::anyHost);
            });

            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = packDirectory;
                staticFiles.location = Location.EXTERNAL;
                staticFiles.hostedPath = "/packs";
            });

            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = pluginPath + "/web/";
                staticFiles.location = Location.EXTERNAL;
                staticFiles.hostedPath = "/web";
            });

            config.showJavalinBanner = false;
        }).start(port);

        // POST /uploadpack?id=...
        app.post("/uploadpack", ctx -> {
            String id = ctx.queryParam("id");
            File file = new File(packDirectory + id + ".zip");
            UploadedFile uploadedFile = ctx.uploadedFile("pack");

             if (uploadedFile == null) {
                 ctx.status(413).json(Map.of("success", false, "error", "No file"));
                 return;
             }

            if (uploadedFile.size() > (long) getConfigInt("settings.pack_file_size_limit_mb") * 1024 * 1024 ) {
                ctx.status(413).json(Map.of("success", false, "error", "File too large"));
                return;
            }

            if(file.exists()) {
                ctx.json(Map.of("success", false));
                return;
            }

            JsonArray packList = getPackList();
            for (JsonElement el : packList) {
                JsonObject obj = el.getAsJsonObject();
                if (obj.get("id").getAsString().equals(id) && !obj.has("pack_name")) {
                    Files.copy(uploadedFile.content(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    obj.addProperty("pack_name", uploadedFile.filename());
                    saveJsonArray(packListPath, packList);

                    ctx.json(Map.of("success", true));
                    return;
                }
            }

            ctx.json(Map.of("success", false));
        });
    }
}