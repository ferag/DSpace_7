package org.dspace.app.profile.importproviders.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.dspace.app.profile.importproviders.model.ConfiguredResearcherProfileProvider;
import org.dspace.content.MetadataValue;
import org.dspace.eperson.EPerson;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ResearcherProfileCtiProviderTest {

    @Mock
    EPerson ePerson;

    @Mock
    MetadataValue metadataValue;

    List<MetadataValue> personMetadata;

    ResearcherProfileCtiProvider ctiProvider;


    @Before
    public void setUp() throws Exception {
       ctiProvider = new ResearcherProfileCtiProvider(null);
       when(ePerson.getID()).thenReturn(UUID.randomUUID());

       personMetadata = new ArrayList<>();
       when(ePerson.getMetadata()).thenReturn(personMetadata);

    }

    @Test
    public void whenDniMetadataIsPresent_shouldConfigureTheProvider() {
        addMetadataValue("perucris", "eperson", "dni", "01234567");

        Optional<ConfiguredResearcherProfileProvider> configuredProvider =
                ctiProvider.configureProvider(ePerson, new ArrayList<>());

        assertTrue(configuredProvider.isPresent());
        assertTrue(configuredProvider.get().getSource().anyMatchSource("dni"));
        assertEquals(configuredProvider.get().getSource().selectSource("dni").get().getId(), "01234567");
    }

    @Test
    public void whenDniMetadataIsNotPresent_shouldNotConfigureTheProvider() {

        Optional<ConfiguredResearcherProfileProvider> configuredProvider =
                ctiProvider.configureProvider(ePerson, new ArrayList<>());

        assertTrue(configuredProvider.isEmpty());
    }

    @Test
    public void whenNetIdMatchValidDni_shouldConfigureTheProvider() {
        when(ePerson.getNetid()).thenReturn("01234567");

        Optional<ConfiguredResearcherProfileProvider> configuredProvider =
                ctiProvider.configureProvider(ePerson, new ArrayList<>());

        assertTrue(configuredProvider.isPresent());
        assertTrue(configuredProvider.get().getSource().anyMatchSource("dni"));
        assertEquals(configuredProvider.get().getSource().selectSource("dni").get().getId(), "01234567");
    }

    @Test
    public void whenNetIdNotMatchValidDni_shouldNotConfigureTheProvider() {
        when(ePerson.getNetid()).thenReturn("aaa");

        Optional<ConfiguredResearcherProfileProvider> configuredProvider =
                ctiProvider.configureProvider(ePerson, new ArrayList<>());

        assertTrue(configuredProvider.isEmpty());
    }

    @Test
    public void whenBothMetadataAndNetIdArePresent_shouldGivePriorityToMetadataValue() {
        addMetadataValue("perucris", "eperson", "dni", "01234567");
        when(ePerson.getNetid()).thenReturn("88888888");

        Optional<ConfiguredResearcherProfileProvider> configuredProvider =
                ctiProvider.configureProvider(ePerson, new ArrayList<>());

        assertTrue(configuredProvider.isPresent());
        assertTrue(configuredProvider.get().getSource().anyMatchSource("dni"));
        assertEquals(configuredProvider.get().getSource().selectSource("dni").get().getId(), "01234567");
    }


    private void addMetadataValue(String schema, String element, String qualifier, String value) {
        when(metadataValue.getSchema()).thenReturn(schema);
        when(metadataValue.getElement()).thenReturn(element);
        when(metadataValue.getQualifier()).thenReturn(qualifier);
        when(metadataValue.getValue()).thenReturn(value);
        personMetadata.add(metadataValue);
    }
}
