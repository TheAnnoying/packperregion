package me.theannoying.packperregion;

import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import com.google.gson.*;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JsonMapper;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;

import static me.theannoying.packperregion.PackPerRegion.packDirectory;
import static me.theannoying.packperregion.PackPerRegion.packListPath;
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
                cors.addRule(it -> it.anyHost());
            });
            // Equivalent to static_folder="packs"
            config.staticFiles.add(staticFiles -> {
                staticFiles.directory = packDirectory;
                staticFiles.location = Location.EXTERNAL;
                staticFiles.hostedPath = "/packs";
            });
        }).start(port);

        // POST /uploadpack?id=...
        app.post("/uploadpack", ctx -> {
            String id = ctx.queryParam("id");
            File file = new File(packDirectory + id + ".zip");

            if (!file.exists()) {
                UploadedFile uploadedFile = ctx.uploadedFile("pack");
                if (uploadedFile != null) {
                    // Save the file
                    Files.copy(uploadedFile.content(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);

                    JsonArray packList = getPackList();
                    JsonObject packDataObject = new JsonObject();
                    packDataObject.addProperty("id", id);
                    packDataObject.addProperty("pack_name", uploadedFile.filename());

                    packList.add(packDataObject);
                    saveJsonArray(packListPath, packList);
                    ctx.json(Collections.singletonMap("success", true));
                }
            } else {
                ctx.json(Collections.singletonMap("success", false));
            }
        });
    }
}