package com.santander.dds;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class RenderDocx {

    public static void render(PathsHelper ctx, JsonObject data) throws Exception {
        Map<String, byte[]> zipEntries = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(ctx.getTemplateDocx()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                zipEntries.put(entry.getName(), baos.toByteArray());
                zis.closeEntry();
            }
        }

        String docXml = new String(zipEntries.get("word/document.xml"), StandardCharsets.UTF_8);

        // 1. Tabla de vocabulario
        docXml = processVocabulario(docXml, data.getAsJsonArray("vocabulario"));

        // 2. Tabla de requisitos
        docXml = processRequisitos(docXml, data.getAsJsonArray("tabla_requisitos"));

        // 3. Placeholders simples
        Map<String, String> subs = new HashMap<>();
        JsonObject meta = data.getAsJsonObject("metadata");
        subs.put("{{fecha_actual_ddmmaa}}", escapeXml(meta.get("fecha_actual_ddmmaa").getAsString()));
        subs.put("{{descripcion_de_los_cambios}}", escapeXml(meta.get("descripcion_de_los_cambios").getAsString()));
        subs.put("{{codigo_del_proyecto}}", escapeXml(meta.get("codigo_proyecto").getAsString()));
        subs.put("{{autor}}", escapeXml(meta.get("autor").getAsString()));
        subs.put("{{introduccion}}", multilineToWt(data.get("introduccion").getAsString()));
        subs.put("{{proposito}}", multilineToWt(data.get("proposito").getAsString()));
        subs.put("{{alcance}}", multilineToWt(data.get("alcance").getAsString()));
        subs.put("{{objetivos/beneficios}}", multilineToWt(renderBullets(data.getAsJsonArray("objetivos_beneficios"))));
        subs.put("{{objectivos/beneficios}}", multilineToWt(renderBullets(data.getAsJsonArray("objetivos_beneficios"))));
        subs.put("{{descripcion}}", multilineToWt(data.get("descripcion_del_sistema").getAsString()));
        subs.put("{{situacion_actual}}", multilineToWt(data.get("situacion_actual").getAsString()));
        subs.put("{{analisis_y_definicion_de_requisitos}}", multilineToWt(data.get("analisis_y_definicion_de_requisitos").getAsString()));
        subs.put("{{Principales_conceptos_y_relaciones_entre_ellos}}", multilineToWt(renderConceptos(data.getAsJsonObject("principales_conceptos_relaciones"))));
        subs.put("{{funcionalidades_sistema}}", multilineToWt(renderFuncionalidades(data.getAsJsonArray("funcionalidades_sistema"))));
        subs.put("{{definicion_de_roles}}", multilineToWt(renderRoles(data.getAsJsonArray("definicion_roles"))));
        subs.put("{{division_del_sistema}}", multilineToWt(renderCapas(data.getAsJsonObject("division_sistema").getAsJsonArray("capas"))));
        subs.put("{{caso_de_uso_principal}}", multilineToWt(renderCasoUso(data.getAsJsonObject("caso_uso_principal"))));
        subs.put("{{Informe_bajo_el_estándar_Xbrl}}", multilineToWt(data.get("informes_xbrl").getAsString()));
        subs.put("{{documentacion_a_producir}}", multilineToWt(renderBullets(data.getAsJsonArray("documentacion_a_producir"))));
        subs.put("{{plan_de_iteraciones}}", multilineToWt(renderPlan(data.getAsJsonArray("plan_iteraciones"))));
        subs.put("{{resguardo_de_documentos_por_tipo_documental}}", multilineToWt(data.get("resguardo_documentos_por_tipo_documental").getAsString()));
        subs.put("{{fecha_actual}}", escapeXml(meta.get("fecha_actual_ddmmaa").getAsString()));

        docXml = applySubstitutions(docXml, subs);

        // Fix date artifact
        String dateValue = escapeXml(meta.get("fecha_actual_ddmmaa").getAsString());
        docXml = docXml.replace(dateValue + " }", dateValue);

        zipEntries.put("word/document.xml", docXml.getBytes(StandardCharsets.UTF_8));

        // 4. Footers y headers
        Map<String, String> footerSubs = new HashMap<>();
        footerSubs.put("{{fecha_actual_ddmmaa}}", escapeXml(meta.get("fecha_actual_ddmmaa").getAsString()));
        footerSubs.put("{{fecha_actual}}", escapeXml(meta.get("fecha_actual_ddmmaa").getAsString()));

        String[] headerFooters = {"word/footer1.xml", "word/footer2.xml", "word/footer3.xml",
                                  "word/header1.xml", "word/header2.xml", "word/header3.xml"};
        for (String hf : headerFooters) {
            if (zipEntries.containsKey(hf)) {
                String xml = new String(zipEntries.get(hf), StandardCharsets.UTF_8);
                xml = applySubstitutions(xml, footerSubs);
                zipEntries.put(hf, xml.getBytes(StandardCharsets.UTF_8));
            }
        }

        // 5. Validación
        List<String> checkFiles = new ArrayList<>(Arrays.asList(headerFooters));
        checkFiles.add("word/document.xml");
        boolean hasLeftovers = false;
        for (String f : checkFiles) {
            if (zipEntries.containsKey(f)) {
                String xml = new String(zipEntries.get(f), StandardCharsets.UTF_8);
                Matcher m = Pattern.compile("\\{\\{[^}]+\\}\\}").matcher(xml);
                if (m.find()) {
                    System.err.println("ERROR - Placeholder sin sustituir en " + f + ": " + m.group());
                    hasLeftovers = true;
                }
            }
        }

        if (hasLeftovers) {
            System.err.println("ERROR — Placeholders sin sustituir; el archivo .docx NO se escribe.");
            System.exit(5);
        }

        // 6. Escribir ZIP
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(ctx.getOutputDocx()))) {
            for (Map.Entry<String, byte[]> entry : zipEntries.entrySet()) {
                ZipEntry ze = new ZipEntry(entry.getKey());
                zos.putNextEntry(ze);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }

        System.out.println("DDS DOCX generado: " + ctx.getOutputDocx());
        System.out.println("Todos los placeholders han sido sustituidos correctamente.");
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String multilineToWt(String s) {
        if (s == null) return "";
        String[] lines = s.split("\\r?\\n");
        return Arrays.stream(lines)
                .map(RenderDocx::escapeXml)
                .collect(Collectors.joining("</w:t><w:br/><w:t xml:space=\"preserve\">"));
    }

    private static String renderBullets(JsonArray arr) {
        if (arr == null) return "";
        return StreamSupport.stream(arr.spliterator(), false)
                .map(e -> "• " + e.getAsString())
                .collect(Collectors.joining("\n"));
    }

    private static String renderFuncionalidades(JsonArray items) {
        if (items == null) return "";
        return StreamSupport.stream(items.spliterator(), false)
                .map(e -> {
                    JsonObject f = e.getAsJsonObject();
                    JsonArray comps = f.getAsJsonArray("componentes");
                    String componentes = comps != null ? StreamSupport.stream(comps.spliterator(), false).map(JsonElement::getAsString).collect(Collectors.joining(", ")) : "";
                    return f.get("id").getAsString() + " — " + f.get("nombre").getAsString() + "\n" +
                           "  " + f.get("descripcion").getAsString() + "\n" +
                           "  Componentes: " + componentes + "\n" +
                           "  Fuente: " + f.get("fuente").getAsString();
                })
                .collect(Collectors.joining("\n\n"));
    }

    private static String renderConceptos(JsonObject pcr) {
        if (pcr == null) return "";
        JsonArray entidades = pcr.getAsJsonArray("entidades");
        String eStr = entidades != null ? StreamSupport.stream(entidades.spliterator(), false)
                .map(e -> e.getAsJsonObject().get("nombre").getAsString() + ": " + e.getAsJsonObject().get("descripcion").getAsString())
                .collect(Collectors.joining("\n")) : "";

        JsonArray relaciones = pcr.getAsJsonArray("relaciones");
        String rStr = (relaciones != null && relaciones.size() > 0) ?
                "\n\nRelaciones:\n" + StreamSupport.stream(relaciones.spliterator(), false)
                        .map(r -> "• " + r.getAsString())
                        .collect(Collectors.joining("\n")) : "";
        return eStr + rStr;
    }

    private static String renderRoles(JsonArray items) {
        if (items == null) return "";
        return StreamSupport.stream(items.spliterator(), false)
                .map(e -> e.getAsJsonObject().get("rol").getAsString() + ": " + e.getAsJsonObject().get("responsabilidad").getAsString())
                .collect(Collectors.joining("\n"));
    }

    private static String renderCapas(JsonArray items) {
        if (items == null) return "";
        return StreamSupport.stream(items.spliterator(), false)
                .map(e -> e.getAsJsonObject().get("nombre").getAsString() + ": " + e.getAsJsonObject().get("descripcion").getAsString())
                .collect(Collectors.joining("\n"));
    }

    private static String renderCasoUso(JsonObject cu) {
        if (cu == null) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("Caso de uso: ").append(cu.get("nombre").getAsString()).append("\n");
        sb.append("Actor: ").append(cu.get("actor").getAsString()).append("\n");
        sb.append("Precondición: ").append(cu.get("precondicion").getAsString()).append("\n\n");
        sb.append("Flujo básico:\n");
        JsonArray flujo = cu.getAsJsonArray("flujo_basico");
        if (flujo != null) {
            for (int i = 0; i < flujo.size(); i++) {
                sb.append((i + 1)).append(". ").append(flujo.get(i).getAsString()).append("\n");
            }
        }
        sb.append("\n\nPostcondición: ").append(cu.get("postcondicion").getAsString());
        return sb.toString();
    }

    private static String renderPlan(JsonArray items) {
        if (items == null) return "";
        return StreamSupport.stream(items.spliterator(), false)
                .map(e -> {
                    JsonObject i = e.getAsJsonObject();
                    return i.get("iteracion").getAsString() + ". " + i.get("nombre").getAsString() + " — " + i.get("alcance").getAsString() + " [" + i.get("estado").getAsString() + "]";
                })
                .collect(Collectors.joining("\n"));
    }

    private static String processVocabulario(String docXml, JsonArray vocabulario) {
        Matcher m = Pattern.compile("<w:tr\\b[^>]*>[\\s\\S]*?</w:tr>").matcher(docXml);
        List<int[]> rows = new ArrayList<>();
        List<String> xmlRows = new ArrayList<>();
        while (m.find()) {
            if (m.group().contains("{{#vocabulario}}") && m.group().contains("{{termino}}")) {
                rows.add(new int[]{m.start(), m.end()});
                xmlRows.add(m.group());
            }
        }
        if (rows.isEmpty()) return docXml;

        String templateRow = xmlRows.get(0);
        StringBuilder newRows = new StringBuilder();
        if (vocabulario != null) {
            for (JsonElement el : vocabulario) {
                JsonObject v = el.getAsJsonObject();
                String row = templateRow
                        .replace("{{#vocabulario}}", escapeXml(v.get("termino").getAsString()))
                        .replace("{{termino}}", escapeXml(v.get("definicion").getAsString()));
                newRows.append(row);
            }
        }
        return docXml.substring(0, rows.get(0)[0]) + newRows.toString() + docXml.substring(rows.get(rows.size() - 1)[1]);
    }

    private static String processRequisitos(String docXml, JsonArray requisitos) {
        Matcher m = Pattern.compile("<w:tr\\b[^>]*>[\\s\\S]*?</w:tr>").matcher(docXml);
        List<int[]> rows = new ArrayList<>();
        List<String> xmlRows = new ArrayList<>();
        while (m.find()) {
            if (m.group().contains("{{id_req}}") && m.group().contains("{{tipo_req}}") &&
                m.group().contains("{{descripcion_req}}") && m.group().contains("{{prioridad_req}}")) {
                rows.add(new int[]{m.start(), m.end()});
                xmlRows.add(m.group());
            }
        }
        if (rows.isEmpty()) return docXml;

        String templateRow = xmlRows.get(0);
        StringBuilder newRows = new StringBuilder();
        if (requisitos != null) {
            for (JsonElement el : requisitos) {
                JsonObject r = el.getAsJsonObject();
                String row = templateRow
                        .replace("{{id_req}}", escapeXml(r.get("id").getAsString()))
                        .replace("{{tipo_req}}", escapeXml(r.get("tipo").getAsString()))
                        .replace("{{descripcion_req}}", escapeXml(r.get("requisito").getAsString()))
                        .replace("{{prioridad_req}}", escapeXml(r.get("prioridad").getAsString()));
                newRows.append(row);
            }
        }
        return docXml.substring(0, rows.get(0)[0]) + newRows.toString() + docXml.substring(rows.get(rows.size() - 1)[1]);
    }

    private static Map<String, String> buildPlaceholderAliases(Map<String, String> subs) {
        Map<String, String> out = new HashMap<>(subs);
        for (Map.Entry<String, String> entry : subs.entrySet()) {
            String stripped = entry.getKey().replace("_", "");
            if (!stripped.equals(entry.getKey()) && !out.containsKey(stripped)) {
                out.put(stripped, entry.getValue());
            }
        }
        return out;
    }

    private static class Wt {
        String open, text, close;
        int start, end;
        Wt(String open, String text, String close, int start, int end) {
            this.open = open; this.text = text; this.close = close; this.start = start; this.end = end;
        }
    }

    private static String applySubstitutions(String xml, Map<String, String> subs) {
        Map<String, String> aliases = buildPlaceholderAliases(subs);
        Matcher pMatcher = Pattern.compile("(<w:p\\b[^>]*>)([\\s\\S]*?)(</w:p>)").matcher(xml);
        StringBuffer sb = new StringBuffer();

        while (pMatcher.find()) {
            String pOpen = pMatcher.group(1);
            String pBody = pMatcher.group(2);
            String pClose = pMatcher.group(3);

            List<Wt> wts = new ArrayList<>();
            Matcher wtMatcher = Pattern.compile("(<w:t[^>]*>)([\\s\\S]*?)(</w:t>)").matcher(pBody);
            while (wtMatcher.find()) {
                wts.add(new Wt(wtMatcher.group(1), wtMatcher.group(2), wtMatcher.group(3), wtMatcher.start(), wtMatcher.end()));
            }

            if (wts.isEmpty()) {
                pMatcher.appendReplacement(sb, Matcher.quoteReplacement(pMatcher.group(0)));
                continue;
            }

            StringBuilder fullTextBuilder = new StringBuilder();
            List<Integer> posToWt = new ArrayList<>();
            for (int i = 0; i < wts.size(); i++) {
                Wt wt = wts.get(i);
                fullTextBuilder.append(wt.text);
                for (int j = 0; j < wt.text.length(); j++) {
                    posToWt.add(i);
                }
            }
            String fullText = fullTextBuilder.toString();

            if (!fullText.contains("{{")) {
                pMatcher.appendReplacement(sb, Matcher.quoteReplacement(pMatcher.group(0)));
                continue;
            }

            Matcher phMatcher = Pattern.compile("\\{\\{[^}]*?\\}\\}").matcher(fullText);
            List<int[]> changes = new ArrayList<>();
            List<String> phList = new ArrayList<>();
            List<String> replList = new ArrayList<>();

            while (phMatcher.find()) {
                String ph = phMatcher.group();
                if (aliases.containsKey(ph)) {
                    changes.add(new int[]{phMatcher.start(), phMatcher.end()});
                    phList.add(ph);
                    replList.add(aliases.get(ph));
                }
            }

            if (changes.isEmpty()) {
                pMatcher.appendReplacement(sb, Matcher.quoteReplacement(pMatcher.group(0)));
                continue;
            }

            List<String> newTexts = new ArrayList<>();
            for (Wt wt : wts) newTexts.add(wt.text);

            // Reverse order
            for (int i = changes.size() - 1; i >= 0; i--) {
                int phStart = changes.get(i)[0];
                int phEnd = changes.get(i)[1];
                String ph = phList.get(i);
                String replacement = replList.get(i);

                int firstWt = posToWt.get(phStart);
                int lastWt = posToWt.get(phEnd - 1);

                if (firstWt == lastWt) {
                    newTexts.set(firstWt, newTexts.get(firstWt).replace(ph, replacement));
                } else {
                    int startOff = phStart;
                    for (int k = 0; k < firstWt; k++) startOff -= wts.get(k).text.length();
                    int endOff = phEnd;
                    for (int k = 0; k < lastWt; k++) endOff -= wts.get(k).text.length();

                    String firstText = newTexts.get(firstWt);
                    newTexts.set(firstWt, firstText.substring(0, startOff) + replacement);
                    for (int k = firstWt + 1; k < lastWt; k++) newTexts.set(k, "");
                    String lastText = newTexts.get(lastWt);
                    newTexts.set(lastWt, lastText.substring(endOff));
                }
            }

            String newBody = pBody;
            for (int i = wts.size() - 1; i >= 0; i--) {
                if (newTexts.get(i).equals(wts.get(i).text)) continue;
                Wt wt = wts.get(i);
                newBody = newBody.substring(0, wt.start) + wt.open + newTexts.get(i) + wt.close + newBody.substring(wt.end);
            }

            pMatcher.appendReplacement(sb, Matcher.quoteReplacement(pOpen + newBody + pClose));
        }
        pMatcher.appendTail(sb);
        return sb.toString();
    }
}
