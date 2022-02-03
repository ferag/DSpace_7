/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices.renacyt;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.Map;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author mykhaylo boychuk (mykhaylo.boychuk at 4science.it)
 */
public class RenacytProvider {

    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(RenacytProvider.class);

    public static final String RENACYT_ID_SYNTAX = "\\d{8}";

    @Autowired
    private RenacytRestConnector renacytRestConnector;

    public RenacytDTO getRenacytObject(String id) {
        InputStream is = getRecords(id);
        if (is != null) {
            return convertToRenacytDTO(is);
        }
        log.error("no RENACYT record found for dni {}", id);
        return new RenacytDTO();
    }

    private InputStream getRecords(String id) {
        if (!isValid(id)) {
            return null;
        }
        return renacytRestConnector.get(id);
    }

    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(RENACYT_ID_SYNTAX);
    }

    @SuppressWarnings("unchecked")
    private RenacytDTO convertToRenacytDTO(InputStream in) {
        Map<String, String> jsonMap = null;
        jsonMap = new Gson().fromJson(new InputStreamReader(in), Map.class);
        String level = jsonMap.get("nivel");
        String group = jsonMap.get("grupo");
        String startDate = jsonMap.get("fechaInicio");
        String endDate = jsonMap.get("fechaFin");

        RenacytDTO dto = new RenacytDTO();
        if (StringUtils.isNotBlank(level)) {
            dto.setLevel(level);
        }
        if (StringUtils.isNotBlank(group)) {
            dto.setGroup(group);
        }
        if (StringUtils.isNotBlank(startDate)) {
            LocalDate sd = parse(startDate);
            dto.setStartDate(sd);
        }
        if (StringUtils.isNotBlank(endDate)) {
            LocalDate ed = parse(endDate);
            dto.setEndDate(ed);
        }
        return dto;
    }

    private LocalDate parse(String date) {
        int split = date.indexOf(" ");
        if (split < 0) {
            return LocalDate.parse(date);
        }
        return LocalDate.parse(date.substring(0, split).trim());
    }
}
