package com.santander.dds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RenderMd {
    public static void render(PathsHelper ctx, JsonObject data) throws Exception {
        String tpl = new String(Files.readAllBytes(ctx.getTemplateMd()), "UTF-8");

        // Normalización de escapes de Google Docs
        tpl = tpl.replace("\\_", "_").replace("\\#", "#");
        
        // Normalizar * dentro de cada placeholder {{...}}
        Matcher m = Pattern.compile("\\{\\{([^}]*?)\\}\\}").matcher(tpl);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String inner = m.group(1);
            String clean = inner.replace("*", "_").replaceAll("__+", "_").replaceAll("^_|_$", "");
            m.appendReplacement(sb, "{{" + clean + "}}");
        }
        m.appendTail(sb);
        tpl = sb.toString();

        // simple substitutions
        JsonObject meta = data.getAsJsonObject("metadata");
        tpl = replaceSimple(tpl, "fecha_actual_ddmmaa", meta.get("fecha_actual_ddmmaa").getAsString());
        tpl = replaceSimple(tpl, "descripcion_de_los_cambios", meta.get("descripcion_de_los_cambios").getAsString());
        tpl = replaceSimple(tpl, "codigo_del_proyecto", meta.get("codigo_proyecto").getAsString());
        tpl = replaceSimple(tpl, "autor", meta.get("autor").getAsString());
        tpl = replaceSimple(tpl, "introduccion", data.get("introduccion").getAsString());
        tpl = replaceSimple(tpl, "proposito", data.get("proposito").getAsString());
        tpl = replaceSimple(tpl, "alcance", data.get("alcance").getAsString());
        tpl = replaceSimple(tpl, "objetivos/beneficios", renderObjetivos(data.getAsJsonArray("objetivos_beneficios")));
        tpl = replaceSimple(tpl, "descripcion", data.get("descripcion_del_sistema").getAsString());
        tpl = replaceSimple(tpl, "situacion_actual", data.get("situacion_actual").getAsString());
        tpl = replaceSimple(tpl, "analisis_y_definicion_de_requisitos", data.get("analisis_y_definicion_de_requisitos").getAsString());
        tpl = replaceSimple(tpl, "Principales_conceptos_y_relaciones_entre_ellos", renderConceptos(data.getAsJsonObject("principales_conceptos_relaciones")));
        tpl = replaceSimple(tpl, "funcionalidades_sistema", renderFuncionalidades(data.getAsJsonArray("funcionalidades_sistema")));
        tpl = replaceSimple(tpl, "definicion_de_roles", renderRoles(data.getAsJsonArray("definicion_roles")));
        tpl = replaceSimple(tpl, "division_del_sistema", renderCapas(data.getAsJsonObject("division_sistema").getAsJsonArray("capas")));
        tpl = replaceSimple(tpl, "caso_de_uso_principal", renderCasoUso(data.getAsJsonObject("caso_uso_principal")));
        tpl = replaceSimple(tpl, "Informe_bajo_el_estándar_Xbrl", data.get("informes_xbrl").getAsString());
        tpl = replaceSimple(tpl, "documentacion_a_producir", renderList(data.getAsJsonArray("documentacion_a_producir")));
        tpl = replaceSimple(tpl, "plan_de_iteraciones", renderPlan(data.getAsJsonArray("plan_iteraciones")));
        tpl = replaceSimple(tpl, "resguardo_de_documentos_por_tipo_documental", data.get("resguardo_documentos_por_tipo_documental").getAsString());

        // tabla de vocabulario
        String vocabRegex = "\\| Término \\| Definición \\|[\\s\\S]*?\\| \\*?\\{\\{#vocabulario\\}\\}\\* ?\\| \\*?\\{\\{termino\\}\\}\\*?[^\\n]*\\n(?:\\| \\*?\\{\\{#vocabulario\\}\\}\\* ?\\| \\*?\\{\\{termino\\}\\}\\*?[^\\n]*\\n)+";
        tpl = tpl.replaceAll(vocabRegex, Matcher.quoteReplacement(renderVocabulario(data.getAsJsonArray("vocabulario")) + "\n"));

        // tabla de referencias
        String refRegex = "\\| Referencia \\| Documento \\|\\n\\| ----- \\| ----- \\|\\n(?:\\|\\s*\\|\\s*\\|\\n)+";
        tpl = tpl.replaceAll(refRegex, Matcher.quoteReplacement(renderReferencias(data.getAsJsonArray("referencias")) + "\n"));

        // tabla de requisitos
        String reqRegex = "\\| ID \\| Tipo \\| Descripción del Requisito \\| Prioridad \\|[\\s\\S]*?(?=\\n#|\\n\\*\\*Anexo|$)";
        tpl = tpl.replaceAll(reqRegex, Matcher.quoteReplacement(renderTablaRequisitos(data.getAsJsonArray("tabla_requisitos")) + "\n"));

        // Anexo 1
        String anexoRegex = "\\*\\*Anexo 1: Lista de Requisitos a satisfacer por todas las aplicaciones\\*\\*[\\s\\S]*$";
        String anexoReplacement = "**Anexo 1: Lista de Requisitos a satisfacer por todas las aplicaciones**\n\n" +
                "Todas las aplicaciones y sistemas desarrollados cumplirán los requisitos estructurales descritos en este anexo.\n\n" +
                renderAnexo(data.getAsJsonArray("anexo_requisitos")) +
                "\n\n---\n\n" +
                "## Apéndice técnico — Pendientes y trazabilidad\n\n" +
                "### Pendientes\n\n" +
                renderList(data.getAsJsonArray("pendientes")) +
                "\n";
        tpl = tpl.replaceAll(anexoRegex, Matcher.quoteReplacement(anexoReplacement));

        // write
        Files.write(ctx.getOutputMd(), tpl.getBytes("UTF-8"));
        System.out.println("DDS Markdown generado: " + ctx.getOutputMd());
    }

    private static String replaceSimple(String tpl, String key, String value) {
        String escapedKey = "\\{\\{\\s*" + key.replace("/", "\\/") + "\\s*\\}\\}";
        return tpl.replaceAll(escapedKey, Matcher.quoteReplacement(value));
    }

    private static String renderObjetivos(JsonArray items) {
        if(items == null) return "";
        return StreamSupport.stream(items.spliterator(), false)
                .map(e -> "- " + e.getAsString())
                .collect(Collectors.joining("\n"));
    }
    
    private static String renderList(JsonArray items) {
        return renderObjetivos(items);
    }

    private static String renderConceptos(JsonObject pcr) {
        if(pcr == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("| Entidad | Descripción |\n| ----- | ----- |\n");
        JsonArray entidades = pcr.getAsJsonArray("entidades");
        if(entidades != null) {
            sb.append(StreamSupport.stream(entidades.spliterator(), false)
                .map(e -> "| " + e.getAsJsonObject().get("nombre").getAsString() + " | " + e.getAsJsonObject().get("descripcion").getAsString() + " |")
                .collect(Collectors.joining("\n")));
        }
        
        JsonArray relaciones = pcr.getAsJsonArray("relaciones");
        if(relaciones != null && relaciones.size() > 0) {
            sb.append("\n\n**Relaciones:**\n\n");
            sb.append(StreamSupport.stream(relaciones.spliterator(), false)
                .map(e -> "- " + e.getAsString())
                .collect(Collectors.joining("\n")));
        }
        return sb.toString();
    }

    private static String renderFuncionalidades(JsonArray items) {
        if(items == null) return "";
        return StreamSupport.stream(items.spliterator(), false)
            .map(e -> {
                JsonObject f = e.getAsJsonObject();
                JsonArray comps = f.getAsJsonArray("componentes");
                String componentes = comps != null ? StreamSupport.stream(comps.spliterator(), false).map(JsonElement::getAsString).collect(Collectors.joining(", ")) : "";
                return "**" + f.get("id").getAsString() + " — " + f.get("nombre").getAsString() + "**\n\n" +
                       f.get("descripcion").getAsString() + "\n\n" +
                       "- Componentes: " + componentes + "\n" +
                       "- Fuente: `" + f.get("fuente").getAsString() + "`";
            })
            .collect(Collectors.joining("\n\n---\n\n"));
    }

    private static String renderRoles(JsonArray items) {
        if(items == null) return "";
        return "| Rol | Responsabilidad |\n| ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> "| " + e.getAsJsonObject().get("rol").getAsString() + " | " + e.getAsJsonObject().get("responsabilidad").getAsString() + " |")
                .collect(Collectors.joining("\n"));
    }

    private static String renderCapas(JsonArray items) {
        if(items == null) return "";
        return "| Capa | Descripción |\n| ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> "| " + e.getAsJsonObject().get("nombre").getAsString() + " | " + e.getAsJsonObject().get("descripcion").getAsString() + " |")
                .collect(Collectors.joining("\n"));
    }

    private static String renderCasoUso(JsonObject cu) {
        if(cu == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("**Caso de uso:** ").append(cu.get("nombre").getAsString()).append("\n\n");
        sb.append("- **Actor:** ").append(cu.get("actor").getAsString()).append("\n");
        sb.append("- **Precondición:** ").append(cu.get("precondicion").getAsString()).append("\n\n");
        sb.append("**Flujo básico:**\n\n");
        JsonArray flujo = cu.getAsJsonArray("flujo_basico");
        if (flujo != null) {
            for (int i = 0; i < flujo.size(); i++) {
                sb.append((i + 1)).append(". ").append(flujo.get(i).getAsString()).append("\n");
            }
        }
        sb.append("\n\n**Postcondición:** ").append(cu.get("postcondicion").getAsString());
        return sb.toString();
    }

    private static String renderPlan(JsonArray items) {
        if(items == null) return "";
        return "| # | Nombre | Alcance | Estado |\n| ----- | ----- | ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> {
                    JsonObject i = e.getAsJsonObject();
                    return "| " + i.get("iteracion").getAsString() + " | " + i.get("nombre").getAsString() + " | " + i.get("alcance").getAsString() + " | " + i.get("estado").getAsString() + " |";
                })
                .collect(Collectors.joining("\n"));
    }

    private static String renderVocabulario(JsonArray items) {
        if(items == null) return "";
        return "| Término | Definición |\n| ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> "| " + e.getAsJsonObject().get("termino").getAsString() + " | " + e.getAsJsonObject().get("definicion").getAsString() + " |")
                .collect(Collectors.joining("\n"));
    }

    private static String renderReferencias(JsonArray items) {
        if(items == null) return "";
        return "| Referencia | Documento |\n| ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> "| " + e.getAsJsonObject().get("referencia").getAsString() + " | " + e.getAsJsonObject().get("documento").getAsString() + " |")
                .collect(Collectors.joining("\n"));
    }

    private static String renderTablaRequisitos(JsonArray items) {
        if(items == null) return "";
        return "| ID | Tipo | Descripción del Requisito | Prioridad |\n| ----- | ----- | ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> {
                    JsonObject r = e.getAsJsonObject();
                    return "| " + r.get("id").getAsString() + " | " + r.get("tipo").getAsString() + " | " + r.get("requisito").getAsString() + " | " + r.get("prioridad").getAsString() + " |";
                })
                .collect(Collectors.joining("\n"));
    }

    private static String renderAnexo(JsonArray items) {
        if(items == null) return "";
        return "| Identificador | Requisito | Categoría | Prioridad | Aplica | Justificación |\n| ----- | ----- | ----- | ----- | ----- | ----- |\n" +
            StreamSupport.stream(items.spliterator(), false)
                .map(e -> {
                    JsonObject r = e.getAsJsonObject();
                    String aplica = r.get("aplica_al_sistema").getAsBoolean() ? "Sí" : "No";
                    return "| " + r.get("identificador").getAsString() + " | " + r.get("requisito").getAsString() + " | " + 
                           r.get("categoria").getAsString() + " | " + r.get("prioridad").getAsString() + " | " + 
                           aplica + " | " + r.get("justificacion").getAsString() + " |";
                })
                .collect(Collectors.joining("\n"));
    }
}
