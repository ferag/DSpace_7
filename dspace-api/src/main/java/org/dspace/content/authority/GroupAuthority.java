/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.util.UUIDUtils;

/**
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class GroupAuthority implements ChoiceAuthority {
    private static final Logger log = Logger.getLogger(GroupAuthority.class);

    /**
     * the name assigned to the specific instance by the PluginService, @see
     * {@link NameAwarePlugin}
     **/
    private String authorityName;

    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 2, locale);
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Context context = new Context();
        if (limit <= 0) {
            limit = 20;
        }
        List<Group> groups = getGroups(context, text, start, limit);
        List<Choice> choiceList = new ArrayList<Choice>();
        for (Group group : groups) {
            choiceList.add(new Choice(group.getID().toString(), group.getName(), group.getName()));
        }
        Choice[] results = new Choice[choiceList.size()];
        results = choiceList.toArray(results);
        return new Choices(results, start, groups.size(), Choices.CF_AMBIGUOUS, groups.size() > (start + limit), 0);
    }

    @Override
    public String getLabel(String key, String locale) {

        UUID uuid = UUIDUtils.fromString(key);
        if (uuid == null) {
            return null;
        }

        Context context = new Context();
        try {
            Group group = groupService.find(context, uuid);
            return group != null ? group.getName() : null;
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }

    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.authorityName = name;
    }

    protected List<Group> getGroups(Context context, String text, int start, int limit) {
        try {
            return groupService.search(context, text, start, limit);
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}