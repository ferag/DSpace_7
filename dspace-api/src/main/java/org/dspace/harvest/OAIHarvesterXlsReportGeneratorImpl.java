/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.harvest.model.OAIHarvesterReport;
import org.dspace.harvest.model.OAIHarvesterReport.ErrorDetails;
import org.dspace.harvest.service.OAIHarvesterReportGenerator;

/**
 * Implementation of {@link OAIHarvesterReportGenerator} that generate an xls
 * file for the given harvesting report.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterXlsReportGeneratorImpl implements OAIHarvesterReportGenerator {

    @Override
    public InputStream generate(OAIHarvesterReport report, HarvestedCollection harvestRow) {
        try (Workbook workbook = new HSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("errors");
            addRow(sheet, 0, "Record identifier", "Record link", "Error", "Action");
            addErrors(sheet, report, harvestRow.getOaiSource());

            workbook.write(baos);
            return new ByteArrayInputStream(baos.toByteArray());

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addErrors(Sheet sheet, OAIHarvesterReport report, String oaiSource) {
        int currentRow = 1;

        Map<String, ErrorDetails> errors = report.getErrors();
        for (Entry<String, ErrorDetails> entry : errors.entrySet()) {
            String recordIdentifier = entry.getKey();
            ErrorDetails errorDetails = entry.getValue();
            String recordLink = formatLink(oaiSource, recordIdentifier, report.getMetadataFormat());
            for (String message : errorDetails.getMessages()) {
                addRow(sheet, currentRow++, recordIdentifier, recordLink, message, errorDetails.getAction());
            }
        }

    }

    private Row addRow(Sheet sheet, int rowIndex, String identifier, String link, String error, String action) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(identifier);
        row.createCell(1).setCellValue(link);
        row.createCell(2).setCellValue(error);
        row.createCell(3).setCellValue(action);
        return row;
    }

    private String formatLink(String oaiSource, String recordIdentifier, String metadataFormat) {
        return String.format(OAIHarvester.RECORD_LINK_FORMAT, oaiSource, recordIdentifier, metadataFormat);
    }

    @Override
    public String getMimeType() {
        return "application/vnd.ms-excel";
    }

    @Override
    public String getName() {
        return "report.xls";
    }

}
