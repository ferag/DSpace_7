/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.perucris.externalservices;

import java.sql.SQLException;

import org.apache.commons.cli.Options;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link ScriptConfiguration} for the {@link UpdateItemWithExternalSource}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class UpdateItemWithExternalSourceScriptConfiguration<T extends UpdateItemWithExternalSource>
        extends ScriptConfiguration<T> {

    private static final Logger log = LoggerFactory.getLogger(UpdateItemWithExternalSourceScriptConfiguration.class);

    private Class<T> dspaceRunnableClass;

    @Autowired
    private AuthorizeService authorizeService;

    @Override
    public boolean isAllowedToExecute(Context context) {
        try {
            return authorizeService.isAdmin(context);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("s", "service", true,
                "the name of the external service to use, i.e. RENIEC, SUNEDU, SUNAT or RENACYT");
            options.getOption("s").setType(String.class);
            options.getOption("s").setRequired(true);

            options.addOption("i", "id", true, "the UUID of the collection to performe update");
            options.getOption("i").setType(String.class);
            options.getOption("i").setRequired(false);

            options.addOption("b", "lastCompleted", true, "only updates items from the last completed process");
            options.getOption("b").setType(Boolean.class);
            options.getOption("b").setRequired(false);

            options.addOption("l", "limit", true, "the limit of items to update");
            options.getOption("l").setType(Integer.class);
            options.getOption("l").setRequired(false);

            options.addOption("u", "itemUuid", true, "update only the item passed on this param");
            options.getOption("u").setType(String.class);
            options.getOption("u").setRequired(false);

            super.options = options;
        }
        return options;
    }

    @Override
    public Class<T> getDspaceRunnableClass() {
        return dspaceRunnableClass;
    }

    @Override
    public void setDspaceRunnableClass(Class<T> dspaceRunnableClass) {
        this.dspaceRunnableClass = dspaceRunnableClass;
    }
}
