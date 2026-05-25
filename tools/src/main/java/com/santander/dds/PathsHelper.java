package com.santander.dds;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathsHelper {
    private final Path rootPath;
    private final Path skillHome;
    private final Path templateRoot;
    
    private final String projectName;
    private final Path jsonPath;
    private final Path outputDir;
    private final Path templateMd;
    private final Path templateDocx;
    private final Path outputMd;
    private final Path outputDocx;

    public PathsHelper(String projectArg) {
        // Detect skill home - assuming this runs from tools/target/dds-tools.jar or tools/
        // Skill home is the parent of the tools directory
        Path currentDir = Paths.get("").toAbsolutePath();
        if (currentDir.endsWith("tools")) {
            skillHome = currentDir.getParent();
        } else if (Files.exists(currentDir.resolve("tools"))) {
            skillHome = currentDir;
        } else {
            skillHome = currentDir.getParent(); // fallback
        }

        templateRoot = skillHome.resolve("Plantillas");

        // Detect Data Root
        String envDataRoot = System.getenv("DDS_DATA_ROOT");
        if (envDataRoot != null && !envDataRoot.trim().isEmpty()) {
            rootPath = Paths.get(envDataRoot);
        } else if (Files.exists(currentDir.resolve("data"))) {
            rootPath = currentDir;
        } else {
            rootPath = skillHome;
        }

        if (projectArg == null || projectArg.trim().isEmpty()) {
            System.err.println("Uso: java -jar tools/target/dds-tools.jar <proyecto>");
            System.err.println("Ejemplo: java -jar tools/target/dds-tools.jar mi-proyecto");
            System.exit(1);
        }

        // Normalize project name
        String normalized = projectArg
                .replaceAll("^data[/\\\\]", "")
                .replaceAll("[/\\\\]agente\\.json$", "")
                .replaceAll("[/\\\\]$", "");

        this.projectName = normalized;
        Path dataDir = rootPath.resolve("data").resolve(normalized);
        this.jsonPath = dataDir.resolve("agente.json");

        if (!Files.exists(this.jsonPath)) {
            System.err.println("ERROR - No existe " + this.jsonPath);
            System.err.println("   Crea data/" + normalized + "/agente.json o usa /add-new-project " + normalized);
            System.exit(2);
        }

        this.outputDir = rootPath.resolve("output").resolve(normalized);
        try {
            Files.createDirectories(this.outputDir);
        } catch (Exception e) {
            System.err.println("ERROR al crear output dir: " + e.getMessage());
        }

        this.templateMd = templateRoot.resolve("DDS_Plantilla - formato santander.md");
        this.templateDocx = templateRoot.resolve("DDS_Plantilla - formato santander.docx");

        if (!Files.exists(this.templateDocx)) {
            System.err.println("ERROR - No se encontró la plantilla en " + this.templateDocx);
            System.err.println("   Configura DDS_DATA_ROOT o coloca las plantillas en Plantillas/.");
            System.exit(6);
        }

        this.outputMd = outputDir.resolve("DDS_" + normalized + ".md");
        this.outputDocx = outputDir.resolve("DDS_" + normalized + ".docx");
    }

    public Path getJsonPath() { return jsonPath; }
    public Path getTemplateMd() { return templateMd; }
    public Path getTemplateDocx() { return templateDocx; }
    public Path getOutputMd() { return outputMd; }
    public Path getOutputDocx() { return outputDocx; }
}
