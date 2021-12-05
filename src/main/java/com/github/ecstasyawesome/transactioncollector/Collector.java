package com.github.ecstasyawesome.transactioncollector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;

public class Collector extends Task<CollectorEvent> {

  private final ResourceBundle languageBundle = ResourceContainer.LANGUAGE_BUNDLE;
  private final String completeMessage = languageBundle.getString("app.complete");
  private final String errorMessage = languageBundle.getString("app.error");
  private final String dayTitle = languageBundle.getString("excel.day");
  private final String sumTitle = languageBundle.getString("excel.sum");
  private final String totalTitle = languageBundle.getString("excel.total");
  private final Map<LocalDate, Double> data = new TreeMap<>();
  private final List<File> files;
  private final String dateColumnName;
  private final String sumColumnName;
  private final int startRow = 1;
  private final int dateColumn = 1;
  private final int sumColumn = 2;

  public Collector(List<File> files, String dateColumnName, String sumColumnName) {
    this.files = files;
    this.dateColumnName = dateColumnName;
    this.sumColumnName = sumColumnName;
  }

  @Override
  public CollectorEvent call() {
    var result = (File) null;
    try {
      for (var file : files) {
        var fileName = file.getName();
        if (fileName.endsWith(SupportedTypes.XLSX.extension)
            || fileName.endsWith(SupportedTypes.XLS.extension)) {
          readFile(new FileInputStream(file), fileName);
        } else if (fileName.endsWith(SupportedTypes.ZIP.extension)) {
          readFilesFromZip(file);
        }
      }
      result = writeFile();
    } catch (Exception e) {
      return new CollectorEvent(Alert.AlertType.ERROR, e.getMessage());
    }
    var message = String.format(completeMessage, result.getAbsolutePath());
    return new CollectorEvent(Alert.AlertType.INFORMATION, message);
  }

  private void readFilesFromZip(File zip) throws IOException {
    try (var archive = new ZipFile(zip)) {
      var entries = archive.stream()
          .filter(entry -> !entry.isDirectory()
                           && (entry.getName().endsWith(SupportedTypes.XLSX.extension)
                               || entry.getName().endsWith(SupportedTypes.XLS.extension)))
          .toArray(ZipEntry[]::new);
      for (var entry : entries) {
        var name = String.join(File.separator, archive.getName(), entry.getName());
        readFile(archive.getInputStream(entry), name);
      }
    }
  }

  private void readFile(InputStream excel, String fileName) throws IOException {
    try (var workbook = WorkbookFactory.create(excel)) {
      final var dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
      var isEmptyFile = true;
      for (var sheetId = 0; sheetId < workbook.getNumberOfSheets(); sheetId++) {
        final var sheet = workbook.getSheetAt(sheetId);
        var isDateCellDetected = false;
        var isSumCellDetected = false;
        var dateCellId = 0;
        var sumCellId = 0;
        for (var rowId = sheet.getFirstRowNum(); rowId <= sheet.getLastRowNum(); rowId++) {
          final var row = sheet.getRow(rowId);
          if (row != null) {
            if (!isDateCellDetected || !isSumCellDetected) {
              for (var cellId = row.getFirstCellNum(); cellId < row.getLastCellNum(); cellId++) {
                final var cell = row.getCell(cellId);
                if (cell != null && cell.getCellType() == CellType.STRING) {
                  var text = cell.getStringCellValue();
                  if (!isDateCellDetected && text.equalsIgnoreCase(dateColumnName)) {
                    isDateCellDetected = true;
                    dateCellId = cellId;
                  } else if (!isSumCellDetected && text.equalsIgnoreCase(sumColumnName)) {
                    isSumCellDetected = true;
                    sumCellId = cellId;
                  } else if (isDateCellDetected && isSumCellDetected) {
                    break;
                  }
                }
              }
            } else {
              var dateValue = (LocalDate) null;
              var sumValue = (Double) null;
              try {
                dateValue = row.getCell(dateCellId).getLocalDateTimeCellValue().toLocalDate();
                sumValue = row.getCell(sumCellId).getNumericCellValue();
              } catch (Exception exception) {
                final var date = row.getCell(dateCellId);
                final var sum = row.getCell(sumCellId);
                if (date != null && date.getCellType() == CellType.STRING
                    && sum != null && sum.getCellType() == CellType.NUMERIC) {
                  var dateTextValue = date.getStringCellValue();
                  if (dateTextValue.matches("^\\d{2}.\\d{2}.\\d{4}$")) {
                    dateValue = LocalDate.parse(dateTextValue, dateTimeFormatter);
                    sumValue = row.getCell(sumCellId).getNumericCellValue();
                  }
                }
              } finally {
                if (dateValue != null && sumValue != null) {
                  data.merge(dateValue, sumValue, Double::sum);
                  isEmptyFile = false;
                }
              }
            }
          }
        }
      }
      if (isEmptyFile) {
        var message = String.format(errorMessage, fileName);
        throw new IllegalArgumentException(message);
      }
    }
  }

  private File prepareResultFile() {
    var fileName =
        String.format("result_%s%s", System.currentTimeMillis(), SupportedTypes.XLSX.extension);
    return new File(String.join(File.separator, files.get(0).getParent(), fileName));
  }

  private File writeFile() throws IOException {
    final var result = prepareResultFile();
    try (var workbook = WorkbookFactory.create(true)) {
      final var dateTimeFormatter = DateTimeFormatter.ofPattern("MM.yyyy");
      var currentRow = 0;
      for (var entry : data.entrySet()) {
        var sheetName = entry.getKey().format(dateTimeFormatter);
        var sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
          sheet = prepareNewSheet(workbook, sheetName);
          currentRow = startRow + 1;
        }
        writeDataRow(sheet, prepareBaseCellStyle(workbook), currentRow++, entry);
        writeTotalRow(sheet, prepareTotalCellStyle(workbook), currentRow);
      }
      workbook.write(new FileOutputStream(result));
    } catch (IOException exception) {
      result.deleteOnExit();
      throw exception;
    }
    return result;
  }

  private Sheet prepareNewSheet(Workbook workbook, String name) {
    var sheet = workbook.createSheet(name);
    var row = sheet.createRow(startRow);

    var dayCell = row.createCell(dateColumn);
    dayCell.setCellStyle(prepareTitleCellStyle(workbook));
    dayCell.setCellValue(dayTitle);

    var sumCell = row.createCell(sumColumn);
    sumCell.setCellStyle(prepareTitleCellStyle(workbook));
    sumCell.setCellValue(sumTitle);

    return sheet;
  }

  private void writeDataRow(
      Sheet sheet,
      CellStyle style,
      int row,
      Map.Entry<LocalDate, Double> entry) {
    var dataRow = sheet.createRow(row);

    var dayCell = dataRow.createCell(dateColumn);
    dayCell.setCellStyle(style);
    dayCell.setCellValue(entry.getKey().getDayOfMonth());

    var sumCell = dataRow.createCell(sumColumn);
    sumCell.setCellStyle(style);
    sumCell.setCellValue(entry.getValue());
  }

  private void writeTotalRow(Sheet sheet, CellStyle style, int row) {
    var range = new CellRangeAddress(startRow + 1, row - 1, sumColumn, sumColumn);
    var finalRow = sheet.createRow(row);

    var totalCell = finalRow.createCell(dateColumn);
    totalCell.setCellStyle(style);
    totalCell.setCellValue(totalTitle);

    var totalSumCell = finalRow.createCell(sumColumn);
    totalSumCell.setCellStyle(style);
    totalSumCell.setCellFormula(String.format("SUM(%s)", range.formatAsString()));
  }

  private CellStyle prepareTitleCellStyle(Workbook workbook) {
    var style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.CENTER);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
    prepareCellBordersStyle(style);
    return style;
  }

  private CellStyle prepareBaseCellStyle(Workbook workbook) {
    var style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.RIGHT);
    prepareCellBordersStyle(style);
    return style;
  }

  private CellStyle prepareTotalCellStyle(Workbook workbook) {
    var style = workbook.createCellStyle();
    style.setAlignment(HorizontalAlignment.RIGHT);
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.index);
    prepareCellBordersStyle(style);
    return style;
  }

  private void prepareCellBordersStyle(CellStyle style) {
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }
}
