package com.company.orderapproval.product.service;

import com.company.orderapproval.common.exception.BadRequestException;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnClass(name = "org.apache.poi.ss.usermodel.WorkbookFactory")
class PoiProductExcelReader implements ProductExcelReader {

    @Override
    public List<BulkProductRow> readRows(MultipartFile file) {
        List<BulkProductRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) {
                return rows;
            }

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isExcelRowBlank(row, formatter, evaluator)) {
                    continue;
                }

                int rowNumber = rowIndex + 1;
                int meaningfulColumns = countMeaningfulColumns(row, formatter, evaluator);
                if (meaningfulColumns < 5 || meaningfulColumns > 6) {
                    throw new BadRequestException("Invalid Excel format at row " + rowNumber);
                }

                rows.add(new BulkProductRow(
                        cellValue(row, 0, formatter, evaluator),
                        cellValue(row, 1, formatter, evaluator),
                        cellValue(row, 2, formatter, evaluator),
                        cellValue(row, 3, formatter, evaluator),
                        cellValue(row, 4, formatter, evaluator),
                        meaningfulColumns == 6 ? cellValue(row, 5, formatter, evaluator) : null,
                        rowNumber
                ));
            }
        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new BadRequestException("Failed to read Excel file");
        }
        return rows;
    }

    private boolean isExcelRowBlank(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        return countMeaningfulColumns(row, formatter, evaluator) == 0;
    }

    private int countMeaningfulColumns(Row row, DataFormatter formatter, FormulaEvaluator evaluator) {
        int meaningfulColumns = 0;
        short lastCellNum = row.getLastCellNum();
        for (int columnIndex = 0; columnIndex < lastCellNum; columnIndex++) {
            if (!cellValue(row, columnIndex, formatter, evaluator).isBlank()) {
                meaningfulColumns = columnIndex + 1;
            }
        }
        return meaningfulColumns;
    }

    private String cellValue(Row row, int columnIndex, DataFormatter formatter, FormulaEvaluator evaluator) {
        return formatter.formatCellValue(row.getCell(columnIndex), evaluator).trim();
    }
}
