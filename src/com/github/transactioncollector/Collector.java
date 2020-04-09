package com.github.transactioncollector;

import javafx.scene.control.Alert;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Collector {

    private CollectorEvent collectorEvent;
    private ResourceBundle languageBundle = ResourceContainer.LANGUAGE_BUNDLE;
    private final Map<LocalDate, Double> list = new TreeMap<>();

    public static class CollectorEvent {
        private Alert.AlertType type;
        private String message;

        private CollectorEvent(Alert.AlertType type, String message) {
            this.type = type;
            this.message = message;
        }

        public Alert.AlertType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }

    public CollectorEvent processFiles(List<File> fileList) {
        try {
            Objects.requireNonNull(fileList);
            for (File file : fileList) {
                if (file.getName().endsWith(SupportedTypes.XLSX.extension)) readFile(new FileInputStream(file));
                else if (file.getName().endsWith(SupportedTypes.ZIP.extension)) readFilesFromZip(file);
            }
            createResultFile(fileList.get(0));
        } catch (Exception e) {
            collectorEvent = new CollectorEvent(Alert.AlertType.ERROR, e.getMessage());
        }
        return collectorEvent;
    }

    private void readFilesFromZip(File zip) throws IOException {
        try (ZipFile archive = new ZipFile(zip)) {
            ZipEntry[] entries = archive.stream()
                    .filter(entry -> !entry.isDirectory() && entry.getName().endsWith(SupportedTypes.XLSX.extension))
                    .toArray(ZipEntry[]::new);
            for (ZipEntry entry : entries) {
                readFile(archive.getInputStream(entry));
            }
        }
    }

    private void readFile(InputStream excel) throws IOException {
        try (Workbook workbook = WorkbookFactory.create(excel)) {
            for (int sheetId = 0; sheetId < workbook.getNumberOfSheets(); sheetId++) {
                Sheet sheet = workbook.getSheetAt(sheetId);
                boolean isDateCellDetected = false, isSumCellDetected = false;
                int dateCellId = 0, sumCellId = 0;
                for (int rowId = sheet.getFirstRowNum(); rowId <= sheet.getLastRowNum(); rowId++) {
                    Row row = sheet.getRow(rowId);
                    if (Objects.nonNull(row)) {
                        if (!isDateCellDetected || !isSumCellDetected) {
                            for (int cellId = row.getFirstCellNum(); cellId < row.getLastCellNum(); cellId++) {
                                Cell cell = row.getCell(cellId);
                                if (Objects.nonNull(cell)) {
                                    if (cell.getCellType() == CellType.STRING) {
                                        String text = cell.getStringCellValue();
                                        if (!isDateCellDetected && text.equalsIgnoreCase("дата транзакции")) {
                                            isDateCellDetected = true;
                                            dateCellId = cellId;
                                        }
                                        if (!isSumCellDetected && text.equalsIgnoreCase("сумма транзакции")) {
                                            isSumCellDetected = true;
                                            sumCellId = cellId;
                                        }
                                        if (isDateCellDetected && isSumCellDetected) break;
                                    }
                                }
                            }
                        } else {
                            Cell date = row.getCell(dateCellId);
                            Cell sum = row.getCell(sumCellId);
                            if (Objects.nonNull(date) && Objects.nonNull(sum)) {
                                if (date.getCellType() == CellType.STRING && sum.getCellType() == CellType.NUMERIC) {
                                    String dateTextValue = date.getStringCellValue();
                                    if (dateTextValue.matches("^\\d{2}.\\d{2}.\\d{4}$")) {
                                        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                                        LocalDate dateValue = LocalDate.parse(dateTextValue, dateTimeFormatter);
                                        double sumValue = row.getCell(sumCellId).getNumericCellValue();
                                        list.merge(dateValue, sumValue, Double::sum);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void createResultFile(File anyReadFile) throws IOException {
        if (list.isEmpty()) throw new NullPointerException(languageBundle.getString("app.error"));
        final String filePath = anyReadFile.getParent() +
                File.separator +
                "result_" +
                new SimpleDateFormat("kkmmss").format(new Date()) +
                SupportedTypes.XLSX.extension;
        try (Workbook workbook = WorkbookFactory.create(true)) {
            final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy");
            final int firstRow = 1, columnB = 1, columnC = 2;
            int currentRow = 0;
            for (Map.Entry<LocalDate, Double> entry : list.entrySet()) {
                String sheetName = entry.getKey().format(dateTimeFormatter);
                Sheet sheet = workbook.getSheet(sheetName);
                if (Objects.isNull(sheet)) {
                    currentRow = firstRow;
                    sheet = workbook.createSheet(sheetName);
                    Row row = sheet.createRow(currentRow++);
                    Cell dayCell = row.createCell(columnB);
                    dayCell.setCellStyle(getTitleCellStyle(workbook));
                    dayCell.setCellValue(languageBundle.getString("excel.day"));
                    Cell sumCell = row.createCell(columnC);
                    sumCell.setCellStyle(getTitleCellStyle(workbook));
                    sumCell.setCellValue(languageBundle.getString("excel.sum"));
                }
                Row dataRow = sheet.createRow(currentRow);
                Cell dayCell = dataRow.createCell(columnB);
                dayCell.setCellStyle(getBaseCellStyle(workbook));
                dayCell.setCellValue(entry.getKey().getDayOfMonth());
                Cell sumCell = dataRow.createCell(columnC);
                sumCell.setCellStyle(getBaseCellStyle(workbook));
                sumCell.setCellValue(entry.getValue());
                CellRangeAddress range = new CellRangeAddress(firstRow + 1, currentRow, columnC, columnC);
                Row finalRow = sheet.createRow(++currentRow);
                Cell totalCell = finalRow.createCell(columnB);
                totalCell.setCellStyle(getTotalCellStyle(workbook));
                totalCell.setCellValue(languageBundle.getString("excel.total"));
                Cell totalSumCell = finalRow.createCell(columnC);
                totalSumCell.setCellStyle(getTotalCellStyle(workbook));
                totalSumCell.setCellFormula("SUM(" + range.formatAsString() + ")");
            }
            workbook.write(new FileOutputStream(filePath));
        }
        String message = String.format(languageBundle.getString("app.complete"), filePath);
        collectorEvent = new CollectorEvent(Alert.AlertType.INFORMATION, message);
    }

    private CellStyle getTitleCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        setBordersStyle(style);
        return style;
    }

    private CellStyle getBaseCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        setBordersStyle(style);
        return style;
    }

    private CellStyle getTotalCellStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.index);
        setBordersStyle(style);
        return style;
    }

    private void setBordersStyle(CellStyle style) {
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}