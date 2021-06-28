/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.app.util.SubmissionConfig;
import org.dspace.app.util.SubmissionConfigReader;
import org.dspace.authority.service.FormNameLookup;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.EPersonAuthority;
import org.dspace.content.authority.GroupAuthority;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.security.service.CrisSecurityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.layout.LayoutSecurity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
@RunWith(MockitoJUnitRunner.class)
public class LayoutSecurityServiceImplTest {

    @Mock
    private AuthorizeService authorizeService;
    @Mock
    private ItemService itemService;
    @Mock
    private GroupService groupService;
    @Mock
    private CrisSecurityService crisSecurityService;
    @Mock
    private ChoiceAuthorityService choiceAuthorityService;
    @Mock
    private SubmissionConfigReader submissionConfigReader;

    @Mock
    private FormNameLookup formNameLookup;

    private LayoutSecurityServiceImpl securityService;

    @Before
    public void setUp() throws Exception {
        securityService = new LayoutSecurityServiceImpl(authorizeService, itemService, groupService,
                                                        crisSecurityService, choiceAuthorityService,
                                                        submissionConfigReader, formNameLookup
        );
        when(submissionConfigReader.getSubmissionConfigByCollection(any()))
            .thenReturn(new SubmissionConfig(false, "test-config", Collections.emptyList()));
        when(formNameLookup.formContainingField(eq("test-config"), any()))
            .thenReturn(Collections.emptyList());
    }

    /**
     * PUBLIC {@link LayoutSecurity} set, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void publicAccessReturnsTrue() throws SQLException {

        boolean granted =
            securityService.hasAccess(LayoutSecurity.PUBLIC,
                                      mock(Context.class),
                                      ePerson(UUID.randomUUID()),
                                      emptySet(),
                                      mock(Item.class));

        assertThat(granted, is(true));
    }

    /**
     * OWNER_ONLY {@link LayoutSecurity} set, accessed by item's owner, access is granted.
     *
     * @throws SQLException
     */
    @Test
    public void ownerOnlyAccessedByItemOwner() throws SQLException {
        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);
        EPerson ownerEperson = ePerson(userUuid);

        when(crisSecurityService.isOwner(ownerEperson, item))
            .thenReturn(true);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_ONLY,
                           mock(Context.class),
                           ownerEperson,
                           emptySet(),
                           item);

        assertThat(granted, is(true));
    }

    /**
     * OWNER_ONLY {@link LayoutSecurity} set, accessed different owner, access forbidden.
     *
     * @throws SQLException
     */
    @Test
    public void ownerOnlyAccessedByOtherUser() throws SQLException {
        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        EPerson userEperson = ePerson(userUuid);
        when(crisSecurityService.isOwner(userEperson, item))
            .thenReturn(false);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_ONLY,
                           mock(Context.class), userEperson,
                           emptySet(),
                           item);

        assertThat(granted, is(false));
    }

    /**
     * OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by administrator user, grant given
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByAdminUser() throws SQLException {

        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(true);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                           context, mock(EPerson.class), emptySet(),
                           item);

        assertThat(granted, is(true));
    }

    /**
     * OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by item's owner user, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByOwnerUser() throws SQLException {

        UUID ownerUuid = UUID.randomUUID();
        EPerson ownerEperson = ePerson(ownerUuid);
        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(crisSecurityService.isOwner(ownerEperson, item))
            .thenReturn(true);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                           context, ownerEperson, emptySet(), item);

        assertThat(granted, is(true));
    }

    /**
     * OWNER_AND_ADMINISTRATOR {@link LayoutSecurity} set, accessed by different user, access NOT granted
     *
     * @throws SQLException
     */
    @Test
    public void ownerAndAdminAccessedByDifferentNotAdminUser() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        EPerson userEperson = ePerson(userUuid);
        Context context = mock(Context.class);
        Item item = mock(Item.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);
        when(crisSecurityService.isOwner(userEperson, item))
            .thenReturn(false);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                           context, userEperson, emptySet(), item);

        assertThat(granted, is(false));
    }

    /**
     * ADMINISTRATOR {@link LayoutSecurity} set, accessed by administrator eperson, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void adminAccessedByAdmin() throws SQLException {

        Context context = mock(Context.class);

        when(authorizeService.isAdmin(context)).thenReturn(true);

        boolean granted =
            securityService
                .hasAccess(LayoutSecurity.ADMINISTRATOR,
                           context, mock(EPerson.class), emptySet(),
                           mock(Item.class));

        assertThat(granted, is(true));
    }

    /**
     * ADMINISTRATOR {@link LayoutSecurity} set, accessed by not administrator eperson, access is NOT  granted
     *
     * @throws SQLException
     */
    @Test
    public void adminAccessedByNotAdmin() throws SQLException {

        Context context = mock(Context.class);

        when(authorizeService.isAdmin(context)).thenReturn(false);

        boolean granted = securityService.hasAccess(LayoutSecurity.ADMINISTRATOR,
                                                    context, mock(EPerson.class), emptySet(), mock(Item.class));

        assertThat(granted, is(false));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id having authority on metadata
     * contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        MetadataField securityMetadataField = securityMetadataField();

        List<MetadataValue> metadataValueList = Arrays.asList(
            metadataValueWithAuthority(securityMetadataField, userUuid.toString()),
            metadataValueWithAuthority(securityMetadataField, UUID.randomUUID().toString()));

        when(itemService.getMetadata(item, securityMetadataField().getMetadataSchema().getName(),
                                     securityMetadataField().getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        when(choiceAuthorityService.getChoiceAuthorityName(any(), any(), any(), any()))
            .thenReturn("EPersonAuthority");

        when(choiceAuthorityService.getChoiceAuthorityByAuthorityName("EPersonAuthority"))
            .thenReturn(mock(EPersonAuthority.class));

        boolean granted =
            securityService.hasAccess(LayoutSecurity.CUSTOM_DATA, mock(Context.class), ePerson(userUuid),
                Set.of(securityMetadataField), item);

        assertThat(granted, is(true));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id having authority on metadata
     * contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityMissingSecurityMetadata() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        List<MetadataValue> metadataValueList = null;

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(
            securityMetadataField()));

        when(itemService.getMetadata(item, securityMetadataField().getMetadataSchema().getName(),
                                     securityMetadataField().getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted =
            securityService.hasAccess(LayoutSecurity.CUSTOM_DATA, mock(Context.class), ePerson(userUuid),
                                      securityMetadataFieldSet, item);

        assertThat(granted, is(false));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, field having null metadata authority, access not granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityNullAuthorityInMetadata() throws SQLException {

        UUID userUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        MetadataField securityMetadataField = securityMetadataField();

        List<MetadataValue> metadataValueList = Arrays.asList(
            metadataValueWithAuthority(securityMetadataField, null),
            metadataValueWithAuthority(securityMetadataField, UUID.randomUUID().toString()));

        when(itemService.getMetadata(item, securityMetadataField().getMetadataSchema().getName(),
            securityMetadataField().getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted =
            securityService.hasAccess(LayoutSecurity.CUSTOM_DATA, mock(Context.class), ePerson(userUuid),
                Set.of(securityMetadataField), item);

        assertThat(granted, is(false));
    }


    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user belonging to a group with id having
     * authority on metadata contained in the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserGroupAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID groupUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        Item item = mock(Item.class);
        Context context = mock(Context.class);

        EPerson user = ePerson(userUuid, UUID.randomUUID(), groupUuid);
        Group userGroup = group(groupUuid);

        when(groupService.allMemberGroupsSet(any(Context.class), eq(user)))
            .thenReturn(new HashSet<>(Collections.singletonList(userGroup)));

        MetadataField securityMetadataField = securityMetadataField();

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(securityMetadataField));

        List<MetadataValue> metadataValueList =
            Arrays.asList(metadataValueWithAuthority(securityMetadataField, securityAuthorityUuid.toString()),
                metadataValueWithAuthority(securityMetadataField, groupUuid.toString()));

        when(choiceAuthorityService.getChoiceAuthorityName(any(), any(), any(), any()))
            .thenReturn("GroupAuthority");

        when(choiceAuthorityService.getChoiceAuthorityByAuthorityName("GroupAuthority"))
            .thenReturn(mock(GroupAuthority.class));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted = securityService.hasAccess(LayoutSecurity.CUSTOM_DATA,
                                                    context, user,
                                                    securityMetadataFieldSet,
                                                    item);

        assertThat(granted, is(true));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by null user.
     * Group is treated as anonymous, anonymous group has grants for the box, access is granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityNullUserAsAnonymousAndGroupAllowed() throws SQLException {

        UUID anonymousGroupUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        Item item = mock(Item.class);
        Context context = mock(Context.class);

        EPerson user = null;
        Group anonymousGroup = group(anonymousGroupUuid);

        when(groupService.findByName(any(Context.class), eq(Group.ANONYMOUS)))
            .thenReturn(anonymousGroup);

        MetadataField securityMetadataField = securityMetadataField();

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(securityMetadataField));

        when(choiceAuthorityService.getChoiceAuthorityName(securityMetadataField.getMetadataSchema().getName(),
                                                           securityMetadataField.getElement(),
                                                           securityMetadataField.getQualifier(), ""))
            .thenReturn("GroupAuthority");

        final GroupAuthority groupAuthority = mock(GroupAuthority.class);
        when(choiceAuthorityService.getChoiceAuthorityByAuthorityName("GroupAuthority"))
            .thenReturn(groupAuthority);

        List<MetadataValue> metadataValueList =
            Arrays.asList(metadataValueWithAuthority(securityMetadataField, anonymousGroupUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted = securityService.hasAccess(LayoutSecurity.CUSTOM_DATA,
                                                    context, user,
                                                    securityMetadataFieldSet,
                                                    item);

        assertThat(granted, is(true));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by null user.
     * Group is threated as anonymous, anonymous group hasn't grants for the box, access is forbidden
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityNullUserAsAnonymousAndGroupNotAllowed() throws SQLException {

        UUID allowedGroupUuid = UUID.randomUUID();

        Item item = mock(Item.class);
        Context context = mock(Context.class);

        EPerson user = null;

        MetadataField securityMetadataField = securityMetadataField();

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(securityMetadataField));


        List<MetadataValue> metadataValueList =
            Arrays.asList(metadataValueWithAuthority(securityMetadataField, allowedGroupUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted = securityService.hasAccess(LayoutSecurity.CUSTOM_DATA,
                                                    context, user,
                                                    securityMetadataFieldSet,
                                                    item);

        assertThat(granted, is(false));
    }

    /**
     * CUSTOM_DATA {@link LayoutSecurity} set, accessed by user with id that does not have any authority on
     * metadata contained in the box, access is NOT  granted
     *
     * @throws SQLException
     */
    @Test
    public void customSecurityUserNotAllowed() throws SQLException {

        UUID userUuid = UUID.randomUUID();
        UUID securityAuthorityUuid = UUID.randomUUID();

        Item item = mock(Item.class);

        Context context = mock(Context.class);
        EPerson user = ePerson(userUuid);

        MetadataField securityMetadataField = securityMetadataField();

        HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(
            singletonList(securityMetadataField));

        List<MetadataValue> metadataValueList =
            singletonList(metadataValueWithAuthority(securityMetadataField, securityAuthorityUuid.toString()));


        when(itemService.getMetadata(item, securityMetadataField.getMetadataSchema().getName(),
                                     securityMetadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        boolean granted = securityService.hasAccess(
            LayoutSecurity.CUSTOM_DATA,
            context, user,
            securityMetadataFieldSet, item);

        assertThat(granted, is(false));
    }

    /**
     * Tests layout security layers with null user object
     *
     * @throws SQLException
     */
    @Test
    public void nullUserHasOnlyPublicAccess() throws SQLException {

        final Context context = mock(Context.class);
        final Item item = mock(Item.class);
        final EPerson user = null;

        final UUID anonymousGroupId = UUID.randomUUID();


        final MetadataField metadataField = securityMetadataField();

        final HashSet<MetadataField> securityMetadataFieldSet = new HashSet<>(singletonList(metadataField));

        List<MetadataValue> metadataValueList =
            singletonList(metadataValueWithAuthority(metadataField, UUID.randomUUID().toString()));


        final Group anonymousGroup = group(anonymousGroupId);

        when(itemService.getMetadata(item, metadataField.getMetadataSchema().getName(),
                                     metadataField.getElement(), null, Item.ANY, true))
            .thenReturn(metadataValueList);

        final boolean publicAccess = securityService.hasAccess(LayoutSecurity.PUBLIC,
                                                                   context, user, securityMetadataFieldSet, item);

        final boolean customDataAccess = securityService.hasAccess(LayoutSecurity.CUSTOM_DATA,
                                                    context, user, securityMetadataFieldSet, item);

        final boolean adminAccess = securityService.hasAccess(LayoutSecurity.ADMINISTRATOR,
                                                              context, user, securityMetadataFieldSet, item);

        final boolean adminOwnerAccess = securityService.hasAccess(LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                                                                   context, user, securityMetadataFieldSet, item);

        assertThat(publicAccess, is(true));
        assertThat(customDataAccess, is(false));
        assertThat(adminAccess, is(false));
        assertThat(adminOwnerAccess, is(false));
    }

    private EPerson ePerson(UUID userUuid, UUID... groupsUuid) throws SQLException {
        EPerson currentUser = mock(EPerson.class);

        when(currentUser.getID()).thenReturn(userUuid);

        return currentUser;
    }

    private Group group(UUID groupUuid) {
        Group group = mock(Group.class);
        when(group.getID()).thenReturn(groupUuid);
        return group;
    }

    private MetadataField securityMetadataField() {
        MetadataSchema ms = mock(MetadataSchema.class);
        when(ms.getName()).thenReturn("schemaname");
        MetadataField msf = mock(MetadataField.class);
        when(msf.getMetadataSchema()).thenReturn(ms);
        when(msf.getElement()).thenReturn("element");
        return msf;
    }

    private MetadataValue metadataValueWithAuthority(MetadataField metadataField, String authority) {
        MetadataValue metadataValue = mock(MetadataValue.class);

        when(metadataValue.getAuthority()).thenReturn(authority);
        when(metadataValue.getMetadataField()).thenReturn(metadataField);

        return metadataValue;

    }

}
