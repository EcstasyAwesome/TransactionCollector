package com.github.transactioncollector;

import com.github.transactioncollector.ui.GraphicInterface;
import com.github.transactioncollector.ui.UserInterface;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.ZipFile;

public class Collector {

    private Map<LocalDate, Double> list = new TreeMap<>();
    private UserInterface userInterface = new GraphicInterface();
    private String message;

    public void run() {
        File[] files = userInterface.fileChooser();
        if (files.length > 0) {
            userInterface.showBar();
            for (File file : files) {
                try {
                    if (file.getName().endsWith(SupportedTypes.XLSX.getSuffix())) readFile(new FileInputStream(file));
                    else if (file.getName().endsWith(SupportedTypes.ZIP.getSuffix())) readFilesFromZip(file);
                } catch (IOException e) {
                    message = e.getMessage();
                }
            }
            createResultFile(files[0]);
            userInterface.closeBar(message);
        }
    }

    private void createResultFile(File anyProcessedFile) {
        String filePath = anyProcessedFile.getParent() +
                File.separator +
                "result_" +
                new SimpleDateFormat("kkmmss").format(new Date()) +
                SupportedTypes.XLSX.getSuffix();
        try (Workbook workbook = WorkbookFactory.create(true)) {
            if (list.isEmpty()) throw new NullPointerException("Invalid input data!");
            int x = 0;
            for (Map.Entry<LocalDate, Double> entry : list.entrySet()) {
                DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy");
                String sheetName = entry.getKey().format(dateTimeFormatter);
                Sheet sheet = workbook.getSheet(sheetName);
                if (Objects.isNull(sheet)) {
                    x = 0;
                    sheet = workbook.createSheet(sheetName);
                    Row row = sheet.createRow(x++);
                    row.createCell(0).setCellValue("Day");
                    row.createCell(1).setCellValue("Sum");
                }
                Row row = sheet.createRow(x);
                row.createCell(0).setCellValue(entry.getKey().getDayOfMonth());
                row.createCell(1).setCellValue(entry.getValue());
                CellRangeAddress range = new CellRangeAddress(1, x, 1, 1);
                row = sheet.createRow(++x);
                row.createCell(0).setCellValue("Total:");
                row.createCell(1).setCellFormula("SUM(" + range.formatAsString() + ")");
            }
            workbook.write(new FileOutputStream(filePath));
            message = String.format("'%s' is successfully created!", filePath);
        } catch (Exception e) {
            message = e.getMessage();
        }
    }

    private void readFilesFromZip(File zip) throws IOException {
        try (ZipFile archive = new ZipFile(zip)) {
            archive.stream()
                    .filter(file -> !file.isDirectory() && file.getName().endsWith(SupportedTypes.XLSX.getSuffix()))
                    .forEach(file -> {
                        try {
                            readFile(archive.getInputStream(file));
                        } catch (IOException e) {
                            message = e.getMessage();
                        }
                    });

        }
    }

    private void readFile(InputStream excel) {
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
        } catch (IOException e) {
            message = e.getMessage();
        }
    }
}