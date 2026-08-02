package com.financepulse.engine.adapters.in.web;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RF-039: serialização CSV própria, sem dependência externa (ver ADR-0021)
 * — RFC 4180 simplificado: separador vírgula, aspas quando o valor contém
 * vírgula/aspas/quebra de linha, {@code CRLF} como terminador de linha.
 * Concerne exclusivamente à camada de adaptador (apresentação); os casos de
 * uso de relatório permanecem agnósticos de formato de exportação.
 *
 * <p>Também neutraliza <a href="https://owasp.org/www-community/attacks/CSV_Injection">CSV/Formula
 * Injection</a>: campos como {@code description}, nome de conta/categoria e
 * tags são texto livre controlado pelo usuário (RF-017) e acabam em um
 * arquivo tipicamente aberto no Excel/Sheets — um valor começando com
 * {@code =}, {@code +}, {@code -}, {@code @} ou tabulação seria interpretado
 * como fórmula por essas ferramentas. Todo valor com esse prefixo recebe um
 * apóstrofo à frente, forçando interpretação como texto.
 */
public final class CsvWriter {

    private static final String DANGEROUS_PREFIX_CHARS = "=+-@\t\r";

    private CsvWriter() {
    }

    public static String write(List<String> header, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        csv.append(row(header));
        for (List<String> row : rows) {
            csv.append(row(row));
        }
        return csv.toString();
    }

    private static String row(List<String> values) {
        return values.stream().map(CsvWriter::escape).collect(Collectors.joining(",")) + "\r\n";
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        String sanitized = neutralizeFormulaInjection(value);
        boolean needsQuoting = sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n") || sanitized.contains("\r");
        String escaped = sanitized.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private static String neutralizeFormulaInjection(String value) {
        if (!value.isEmpty() && DANGEROUS_PREFIX_CHARS.indexOf(value.charAt(0)) >= 0) {
            return "'" + value;
        }
        return value;
    }
}
