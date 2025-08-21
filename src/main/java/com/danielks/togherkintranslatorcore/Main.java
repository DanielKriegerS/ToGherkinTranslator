package com.danielks.togherkintranslatorcore;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Selecionar planilha de testes:");
        int resultado = fileChooser.showOpenDialog(null);

        if (resultado != JFileChooser.APPROVE_OPTION) {
            System.out.println("Nenhum arquivo selecionado!");
            return;
        }

        File arquivoSelecionado = fileChooser.getSelectedFile();
        String caminhoArquivo = arquivoSelecionado.getAbsolutePath();
        String pastaSaida = "saida_gherkin";

        try {
            Map<String, List<GherkinStep>> testesAgrupados = new HashMap<>();

            FileInputStream fis = new FileInputStream(caminhoArquivo);
            Workbook workbook = new XSSFWorkbook(fis);
            Sheet sheet = workbook.getSheetAt(0);

            int nomeTesteCol = 0;
            int preRequisitoCol = 1;
            int descricaoCol = 3;
            int tipoCol = 5;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String nomeTeste = getMergedCellValue(sheet, row, nomeTesteCol);
                String preRequisito = getMergedCellValue(sheet, row, preRequisitoCol);
                String descricao = getCellValue(row.getCell(descricaoCol));
                String tipo = getCellValue(row.getCell(tipoCol));

                if (nomeTeste.isEmpty() || descricao.isEmpty() || tipo.isEmpty()) continue;

                testesAgrupados
                        .computeIfAbsent(nomeTeste, k -> new ArrayList<>())
                        .add(new GherkinStep(tipo.trim(), descricao.trim(), preRequisito.trim()));
            }

            workbook.close();
            fis.close();

            Files.createDirectories(Path.of(pastaSaida));

            for (Map.Entry<String, List<GherkinStep>> entry : testesAgrupados.entrySet()) {
                String nomeTeste = entry.getKey();
                List<GherkinStep> passos = entry.getValue();

                StringBuilder gherkin = new StringBuilder();
                gherkin.append("Feature: ").append(nomeTeste).append("\n\n");

                Optional<String> primeiroPreReq = passos.stream()
                        .map(s -> s.preRequisito)
                        .filter(p -> p != null && !p.isEmpty())
                        .findFirst();

                primeiroPreReq.ifPresent(pre -> gherkin.append("Given ").append(pre).append("\n"));

                for (GherkinStep step : passos) {
                    String tipoFormatado = formatTipo(step.tipo);
                    gherkin.append(tipoFormatado).append(" ").append(step.descricao).append("\n");
                }

                String nomeArquivo = nomeTeste.replaceAll("[^a-zA-Z0-9\\-_ ]", "").replace(" ", "") + ".txt";
                Path caminhoSaida = Path.of(pastaSaida, nomeArquivo);

                try (BufferedWriter writer = Files.newBufferedWriter(caminhoSaida)) {
                    writer.write(gherkin.toString());
                }

                System.out.println("Arquivo gerado: " + caminhoSaida);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static String getCellValue(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    static String getMergedCellValue(Sheet sheet, Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell != null && cell.getCellType() != CellType.BLANK) {
            return getCellValue(cell);
        }

        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if(range.isInRange(row.getRowNum(), columnIndex)){
                Row firstRow = sheet.getRow(range.getFirstRow());
                if(firstRow != null) {
                    Cell firstCell = firstRow.getCell(range.getFirstColumn());
                    return getCellValue(firstCell);
                }
            }
        }

        return "";
    }

    static String formatTipo(String tipo) {
        return switch (tipo.toUpperCase()) {
            case "WHEN" -> "When";
            case "THEN" -> "Then";
            case "AND" -> "And";
            default -> tipo;
        };
    }
}