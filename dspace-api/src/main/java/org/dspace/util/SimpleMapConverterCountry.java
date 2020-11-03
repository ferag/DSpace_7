/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.services.ConfigurationService;
import org.dspace.sunedu.SuneduProvider;

/**
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class SimpleMapConverterCountry {

    private static Logger log = LogManager.getLogger(SuneduProvider.class);

    private String mappingFile;

    private String converterNameFile;

    private ConfigurationService configurationService;

    private Map<String, String> mapping;

    private String defaultValue;

    public void init() {
        this.mappingFile = configurationService.getProperty(
            "dspace.dir") + File.separator + "config" + File.separator + "crosswalks" + File.separator +
            converterNameFile;

        this.mapping = new HashMap<String, String>();

        try (BufferedReader objReader = new BufferedReader(
                new InputStreamReader(new FileInputStream(mappingFile), "ISO-8859-1"))) {

            String strCurrentLine;

            while ((strCurrentLine = objReader.readLine()) != null) {
                String [] record = strCurrentLine.split("=");
                if (record[0] != null && record[1] != null) {
                    String key = record[0].replaceAll("\\s+$", "");
                    String value = record[1].replaceAll("\t", "");
                    mapping.put(key, value);
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public String getValue(String key) {
        String value  = "";
        if (mapping.containsKey(key)) {
            value  = mapping.get(key);
        } else {
            return defaultValue;
        }
        return value ;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public void setConverterNameFile(String converterNameFile) {
        this.converterNameFile = converterNameFile;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }
}