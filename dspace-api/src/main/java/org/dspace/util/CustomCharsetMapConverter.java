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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this extension of {@link org.dspace.submit.lookup.MapConverterModifier}
 * allows the usage of mapping files with charsets different than the system default one
 *
 * Charset to be used should be injected into the instance, otherwise system default one
 * will be used
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class CustomCharsetMapConverter extends SimpleMapConverter {

    private static final Logger log = LoggerFactory.getLogger(CustomCharsetMapConverter.class);

    private String charset;

    @Override
    public void init() {
        String mappingFile = configurationService.getProperty(
            "dspace.dir") + File.separator + "config" + File.separator + "crosswalks" + File.separator +
            converterNameFile;

        this.mapping = new HashMap<String, String>();

        try (BufferedReader objReader = new BufferedReader(
            new InputStreamReader(new FileInputStream(mappingFile), Optional.ofNullable(charset).orElse(
                Charset.defaultCharset().name())))) {

            String strCurrentLine;

            while ((strCurrentLine = objReader.readLine()) != null) {
                String [] record = strCurrentLine.split("=");
                if (record[0] != null && record[1] != null) {
                    String key = record[0].replaceAll("\\s+$", "")
                        .replace("\\", "");
                    String value = record[1].replaceAll("\t", "").trim();
                    mapping.put(key, value);
                }
            }

        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }
}
