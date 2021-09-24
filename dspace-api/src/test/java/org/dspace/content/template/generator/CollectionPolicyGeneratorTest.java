/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.template.generator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link CollectionPolicyGenerator}
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class CollectionPolicyGeneratorTest {

    private CollectionPolicyGenerator collectionPolicyGenerator;
    private Context context = mock(Context.class);

    @Before
    public void setUp() throws Exception {
        collectionPolicyGenerator = new CollectionPolicyGenerator();
    }

    @Test
    public void malformedParameters() {
        Collection collection = collection(UUID.randomUUID(), Collections.emptyList());
        MetadataValueVO generated = collectionPolicyGenerator
            .generator(context, item(), itemTemplateOf(collection), "foo.bar.baz")
            .get(0);

        assertThat(generated.getValue(), is(""));
    }

    @Test
    public void invalidAction() {
        List<ResourcePolicy> resourcePolicies = resourcePolicy(Constants.DEFAULT_ITEM_READ, null, null);
        Collection collection = collection(UUID.randomUUID(), resourcePolicies);

        when(collection.getResourcePolicies())
            .thenReturn(resourcePolicies);

        List<MetadataValueVO> generated = collectionPolicyGenerator
            .generator(context, item(), itemTemplateOf(collection), "invalid.eperson");

        assertThat(generated.get(0).getValue(), is(""));
    }

    @Test
    public void invalidEpolicyParam() {
        List<ResourcePolicy> resourcePolicies = resourcePolicy(Constants.DEFAULT_ITEM_READ, null, null);
        Collection collection = collection(UUID.randomUUID(), resourcePolicies);

        when(collection.getResourcePolicies())
            .thenReturn(resourcePolicies);

        List<MetadataValueVO> generated = collectionPolicyGenerator
            .generator(context, item(), itemTemplateOf(collection), "default_item_read.invalid");

        assertThat(generated.get(0).getValue(), is(""));
    }

    @Test
    public void actionNotFound() {
        List<ResourcePolicy> resourcePolicies = resourcePolicy(Constants.DEFAULT_BITSTREAM_READ, null, null);
        Collection collection = collection(UUID.randomUUID(), resourcePolicies);

        MetadataValueVO generated = collectionPolicyGenerator
            .generator(context, item(), itemTemplateOf(collection),
                "default_item_read.epersongroup")
            .get(0);

        assertThat(generated.getValue(), is(""));
    }

    @Test
    public void ePersonPolicy() {
        UUID id = UUID.randomUUID();
        EPerson eperson = eperson(id, "epersonName");
        List<ResourcePolicy> resourcePolicies = resourcePolicy(Constants.DEFAULT_ITEM_READ, eperson, null);
        Collection collection = collection(UUID.randomUUID(), resourcePolicies);

        when(collection.getResourcePolicies())
            .thenReturn(resourcePolicies);

        MetadataValueVO generated = collectionPolicyGenerator
            .generator(context, item(), itemTemplateOf(collection),
                "default_item_read.eperson")
            .get(0);

        assertThat(generated.getValue(), is("epersonName"));
        assertThat(generated.getAuthority(), is(id.toString()));
    }

    @Test
    public void groupPolicy() throws SQLException {
        UUID id = UUID.randomUUID();
        Group group = group(id, "groupName");
        List<ResourcePolicy> resourcePolicies = resourcePolicy(Constants.DEFAULT_ITEM_READ, null, group);
        Collection collection = collection(UUID.randomUUID(), resourcePolicies);

        when(collection.getResourcePolicies())
            .thenReturn(resourcePolicies);

        MetadataValueVO generated = collectionPolicyGenerator
            .generator(context, item(), itemTemplateOf(collection),
                "default_item_read.epersongroup")
            .get(0);

        assertThat(generated.getValue(), is("groupName"));
        assertThat(generated.getAuthority(), is(id.toString()));
    }

    private Group group(UUID id, String name) {
        Group group = mock(Group.class);
        when(group.getName()).thenReturn(name);
        when(group.getID()).thenReturn(id);
        return group;
    }

    private EPerson eperson(UUID id, String name) {
        EPerson ePerson = mock(EPerson.class);
        when(ePerson.getName()).thenReturn(name);
        when(ePerson.getID()).thenReturn(id);
        return ePerson;
    }

    private List<ResourcePolicy> resourcePolicy(int actionId, EPerson eperson, Group group) {
        ResourcePolicy resourcePolicy = mock(ResourcePolicy.class);
        when(resourcePolicy.getAction()).thenReturn(actionId);
        when(resourcePolicy.getEPerson()).thenReturn(eperson);
        when(resourcePolicy.getGroup()).thenReturn(group);
        return Collections.singletonList(resourcePolicy);
    }

    private Collection collection(UUID uuid, List<ResourcePolicy> resourcePolicies) {
        Collection collection = mock(Collection.class);
        when(collection.getID()).thenReturn(uuid);
        when(collection.getResourcePolicies())
            .thenReturn(resourcePolicies);
        return collection;
    }

    private Item itemTemplateOf(Collection templateCollection) {
        Item item = mock(Item.class);
        when(item.getTemplateItemOf()).thenReturn(templateCollection);
        return item;
    }

    private Item item() {
        Item item = mock(Item.class);
        return item;
    }
}