package com.santander.dds;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.nio.file.Files;

public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java -jar tools/target/dds-tools.jar <proyecto> [modo]");
            System.err.println("  modo: md (solo Markdown), docx (solo Word). Por defecto ambos.");
            System.exit(1);
        }

        String projectArg = args[0];
        String mode = args.length > 1 ? args[1].toLowerCase() : "both";

        PathsHelper ctx = new PathsHelper(projectArg);

        try {
            String jsonContent = new String(Files.readAllBytes(ctx.getJsonPath()), "UTF-8");
            Gson gson = new Gson();
            JsonObject data = gson.fromJson(jsonContent, JsonObject.class);

            if (mode.equals("both") || mode.equals("md")) {
                RenderMd.render(ctx, data);
            }
            if (mode.equals("both") || mode.equals("docx")) {
                RenderDocx.render(ctx, data);
            }
        } catch (Exception e) {
            System.err.println("ERROR - Hubo un problema al renderizar:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
