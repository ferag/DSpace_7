/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.dspace.core.service.LicenseService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulate the deposit license.
 *
 * @author mhwood
 */
public class LicenseServiceImpl implements LicenseService {
    private final Logger log = LoggerFactory.getLogger(LicenseServiceImpl.class);

    /**
     * The default license
     */
    protected String license;

    private Map<Locale, String> localisedLicenses;

    protected LicenseServiceImpl() {

    }

    @Override
    public void writeLicenseFile(String licenseFile,
                                 String newLicense) {
        try {
            FileOutputStream fos = new FileOutputStream(licenseFile);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(newLicense);
            out.close();
        } catch (IOException e) {
            log.warn("license_write: " + e.getLocalizedMessage());
        }
        license = newLicense;
    }

    @Override
    public String getLicenseText(String licenseFile) {
        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try {
            is = new FileInputStream(licenseFile);
            ir = new InputStreamReader(is, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";
            while ((lineIn = br.readLine()) != null) {
                license = license + lineIn + '\n';
            }
        } catch (IOException e) {
            log.error("Can't load configuration", e);
            throw new IllegalStateException("Failed to read default license.", e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
        return license;
    }

    /**
     * Get the site-wide default license that submitters need to grant
     *
     * @return the default license
     */
    @Override
    public String getDefaultSubmissionLicense() {
        if (null == license) {
            init();
        }
        return license;
    }

    @Override
    public String getDefaultSubmissionLicense(final Locale locale) {
        if (Objects.isNull(localisedLicenses)) {
            initLicenseMap();
        }
        return localisedLicenses.containsKey(locale) ?
                   localisedLicenses.get(locale) : localisedLicenses.get(I18nUtil.getDefaultLocale());
    }

    private void initLicenseMap() {
        localisedLicenses = Arrays.stream(I18nUtil.getSupportedLocales())
              .collect(Collectors.toMap(l -> l, this::readLicense));
    }

    private String readLicense(final Locale locale) {
        String fileName = "";
        /** Name of the default license */
        String defsFilename = DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
                                  + File.separator + "config" + File.separator + "default";

        fileName = getFilename(locale, defsFilename, ".license");


        try {
            return FileUtils.readFileToString(FileUtils.getFile(fileName), Charset.defaultCharset());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load in the default license.
     */
    protected void init() {
        File licenseFile = new File(
            DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "default.license");

        FileInputStream fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try {

            fir = new FileInputStream(licenseFile);
            ir = new InputStreamReader(fir, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";

            while ((lineIn = br.readLine()) != null) {
                license = license + lineIn + '\n';
            }

            br.close();

        } catch (IOException e) {
            log.error("Can't load license: " + licenseFile.toString(), e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new IllegalStateException("Cannot load license: "
                                                + licenseFile.toString(), e);
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }

            if (ir != null) {
                try {
                    ir.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }

            if (fir != null) {
                try {
                    fir.close();
                } catch (IOException ioe) {
                    // ignore
                }
            }
        }
    }

    /**
     * Get the appropriate localized version of a file according to language settings
     * e. g. help files in jsp/help/
     *
     * @param locale   Locale to get the file for
     * @param fileName String fileName, to get the localized file for
     * @param fileType String file extension
     * @return localizedFileName
     * String - localized filename
     */
    private static String getFilename(Locale locale, String fileName, String fileType) {
        String localizedFileName = null;
        boolean fileFound = false;
        // with Language, Country, Variant
        String fileNameLCV = null;
        // with Language, Country
        String fileNameLC = null;
        // with Language
        String fileNameL = null;
        fileNameL = fileName + "_" + locale.getLanguage();

        if (fileType == null) {
            fileType = "";
        }

        if (!("".equals(locale.getCountry()))) {
            fileNameLC = fileName + "_" + locale.getLanguage() + "_"
                             + locale.getCountry();

            if (!("".equals(locale.getVariant()))) {
                fileNameLCV = fileName + "_" + locale.getLanguage() + "_"
                                  + locale.getCountry() + "_" + locale.getVariant();
            }
        }

        if (fileNameLCV != null && !fileFound) {
            File fileTmp = new File(fileNameLCV + fileType);
            if (fileTmp.exists()) {
                fileFound = true;
                localizedFileName = fileNameLCV + fileType;
            }
        }

        if (fileNameLC != null && !fileFound) {
            File fileTmp = new File(fileNameLC + fileType);
            if (fileTmp.exists()) {
                fileFound = true;
                localizedFileName = fileNameLC + fileType;
            }
        }

        if (fileNameL != null && !fileFound) {
            File fileTmp = new File(fileNameL + fileType);
            if (fileTmp.exists()) {
                fileFound = true;
                localizedFileName = fileNameL + fileType;
            }
        }
        if (!fileFound) {
            localizedFileName = fileName + fileType;
        }
        return localizedFileName;
    }
}
