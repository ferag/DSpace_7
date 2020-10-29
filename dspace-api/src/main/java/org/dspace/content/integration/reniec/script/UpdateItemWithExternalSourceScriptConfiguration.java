/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.reniec.script;

import org.apache.commons.cli.Options;
import org.dspace.core.Context;
import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * {@link ScriptConfiguration} for the {@link UpdateItemWithExternalSource}.
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 *
 */
public class UpdateItemWithExternalSourceScriptConfiguration<T extends UpdateItemWithExternalSource>
        extends ScriptConfiguration<T> {

    private Class<T> dspaceRunnableClass;

    @Override
    public boolean isAllowedToExecute(Context context) {
        return context.getCurrentUser() != null;
    }

    @Override
    public Options getOptions() {
        if (options == null) {
            Options options = new Options();

            options.addOption("i", "id", true, "the UUID of the collection to performe update");
            options.getOption("i").setType(String.class);
            options.getOption("i").setRequired(false);

            options.addOption("s","service", true, "the name of the external service to use");
            options.getOption("s").setType(String.class);
            options.getOption("s").setRequired(false);
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