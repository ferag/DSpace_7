/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;


/**
 *
 * Implementation of {@link BulkImportValueTransformer} that parses entries in format
 * Ciiu::CODE, i.e. Ciiu::A0150 and transforms them in their controlled vocabulary entries
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class BulkImportCiiuValueTransformer extends BulkImportURIControlledVocabularyValueTransformer {

    private static final String PATTERN = "^Ciiu::(.+)$";

    @Override
    protected String keyPattern() {
        return PATTERN;
    }
}
