/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

/**
 * Custom implementation of {@link ChoiceAuthority} that represent an authority
 * that is disabled and thus for metadata where it is associated, no authority has
 * indeed to be set.
 *
 * It is used only to make the {@link ChoiceAuthorityServiceImpl#isChoicesConfigured(java.lang.String, java.lang.String)}
 * call performed, for a given form name and field pair, return false in case this authority is defined for this pair.
 *
 * Any call to its methods will end up in an {@link UnsupportedOperationException}.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 *
 */
public class DisabledAuthority implements ChoiceAuthority {

    private String authorityName;

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        return new Choices(Choices.CF_UNSET);
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        return new Choices(Choices.CF_UNSET);
    }

    @Override
    public String getLabel(String key, String locale) {
        return "";
    }

    @Override
    public String getPluginInstanceName() {
        return this.authorityName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.authorityName = name;
    }
}
