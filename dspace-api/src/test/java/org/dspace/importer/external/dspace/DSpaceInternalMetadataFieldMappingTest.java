/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.dspace;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.services.RequestService;
import org.dspace.services.model.Request;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for internal metadata mapping.
 *
 * @author Corrado Lombardi (corrado.lombardi at 4science.it)
 */
public class DSpaceInternalMetadataFieldMappingTest {

    private DSpaceInternalMetadataFieldMapping metadataFieldMapping;

    private final ItemService itemService = mock(ItemService.class);
    private final CrisLayoutBoxService crisLayoutBoxService = mock(CrisLayoutBoxService.class);
    private final Context context = mock(Context.class);

    @Before
    public void setUp() throws Exception {
        RequestService requestService = requestService();

        metadataFieldMapping = new DSpaceInternalMetadataFieldMapping(itemService, requestService,
                crisLayoutBoxService);
    }

    @Test
    public void onlyPublicMetadataAreImported() throws Exception {

        String entityType = "Person";
        Item item = item();
        List<CrisLayoutBox> boxes = Arrays.asList(
                boxWithMetadata(
                        LayoutSecurity.PUBLIC,
                        metadataField(1, 11, "metadata1", "foo"),
                        metadataField(2, 22, "metadata2", "foo"),
                        metadataField(3, 33, "metadata3", "foo"),
                        metadataField(4, 44, "metadata4", "foo")
                ),
                boxWithMetadata(
                        LayoutSecurity.ADMINISTRATOR,
                        metadataField(11, 1111, "metadata11", "foo"),
                        metadataField(22, 2222, "metadata22", "foo"),
                        metadataField(33, 3333, "metadata33", "foo"),
                        metadataField(44, 4444, "metadata44", "foo")
                ),
                boxWithMetadata(
                        LayoutSecurity.OWNER_ONLY,
                        metadataField(111, 111111, "metadata111", "foo")
                ),
                boxWithMetadata(
                        LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                        metadataField(222, 222222, "metadata222", "foo")
                )
        );
        List<MetadataValue> itemMetadata = Arrays.asList(
                metadataValue(metadataField(1, 11, "metadata1", "foo"), "metadata1Value"),
                metadataValue(metadataField(4, 44, "metadata4", "foo"), "metadata4Value"),
                metadataValue(metadataField(11, 1111, "metadata11", "foo"), "metadata11Value"),
                metadataValue(metadataField(111, 111111, "metadata111", "foo"), "metadata111Value"),
                metadataValue(metadataField(222, 222222, "metadata222", "foo"), "metadata222Value")
        );

        when(itemService.getMetadataFirstValue(item, MetadataSchemaEnum.RELATIONSHIP.getName(),
                "type", null, Item.ANY)).thenReturn(entityType);

        when(crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0))
                .thenReturn(boxes);
        when(itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY, false))
                .thenReturn(itemMetadata);

        Collection<MetadatumDTO> metadatumDtoList = metadataFieldMapping.resultToDCValueMapping(item);

        assertThat(metadatumDtoList, Matchers.containsInAnyOrder(
                MetadatumDTOMatcher.of("metadata1", "foo", "metadata1Value"),
                MetadatumDTOMatcher.of("metadata4", "foo", "metadata4Value")
        ));
    }

    @Test
    public void noPublicMetadataToImport() throws Exception {

        String entityType = "Person";
        Item item = item();
        List<CrisLayoutBox> boxes = Arrays.asList(
                boxWithMetadata(
                        LayoutSecurity.ADMINISTRATOR,
                        metadataField(1, 11, "metadata1", "foo"),
                        metadataField(2, 22, "metadata2", "foo"),
                        metadataField(3, 33, "metadata3", "foo"),
                        metadataField(4, 44, "metadata4", "foo")
                ),
                boxWithMetadata(
                        LayoutSecurity.ADMINISTRATOR,
                        metadataField(11, 1111, "metadata11", "foo"),
                        metadataField(22, 2222, "metadata22", "foo"),
                        metadataField(33, 3333, "metadata33", "foo"),
                        metadataField(44, 4444, "metadata44", "foo")
                ),
                boxWithMetadata(
                        LayoutSecurity.OWNER_ONLY,
                        metadataField(111, 111111, "metadata111", "foo")
                ),
                boxWithMetadata(
                        LayoutSecurity.OWNER_AND_ADMINISTRATOR,
                        metadataField(222, 222222, "metadata222", "foo")
                )
        );
        List<MetadataValue> itemMetadata = Arrays.asList(
                metadataValue(metadataField(1, 11, "metadata1", "foo"), "metadata1Value"),
                metadataValue(metadataField(4, 44, "metadata4", "foo"), "metadata4Value"),
                metadataValue(metadataField(11, 1111, "metadata11", "foo"), "metadata11Value"),
                metadataValue(metadataField(111, 111111, "metadata111", "foo"), "metadata111Value"),
                metadataValue(metadataField(222, 222222, "metadata222", "foo"), "metadata222Value")
        );

        when(itemService.getMetadataFirstValue(item, MetadataSchemaEnum.RELATIONSHIP.getName(),
                "type", null, Item.ANY)).thenReturn(entityType);

        when(crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0))
                .thenReturn(boxes);
        when(itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY, false))
                .thenReturn(itemMetadata);

        Collection<MetadatumDTO> metadatumDtoList = metadataFieldMapping.resultToDCValueMapping(item);

        assertThat(metadatumDtoList, is(Collections.emptyList()));
    }

    @Test(expected = RuntimeException.class)
    public void exceptionWhileGettingPublicFields() {

        Item item = item();

        doThrow(new SQLException("sql exception")).when(itemService)
                .getMetadataFirstValue(item, MetadataSchemaEnum.RELATIONSHIP.getName(),
                        "type", null, Item.ANY);


        metadataFieldMapping.resultToDCValueMapping(item);
    }

    private MetadataField metadataField(Integer schemaId, Integer fieldId, String schema, String element) {
        return new MockMetadataField(schemaId, fieldId, schema, element);
    }

    private MetadataValue metadataValue(MetadataField metadataField, String value) {
        return new MockMetadataValue(metadataField, value);
    }

    private CrisLayoutBox boxWithMetadata(LayoutSecurity security, MetadataField... metadata) {

        CrisLayoutBox box = mock(CrisLayoutBox.class);

        List<CrisLayoutField> layoutFields = Arrays.stream(metadata)
                .map(this::toCrisLayoutField)
                .collect(Collectors.toList());

        when(box.getSecurity()).thenReturn(security.getValue());
        when(box.getLayoutFields())
                .thenReturn(layoutFields);

        return box;
    }

    private CrisLayoutField toCrisLayoutField(MetadataField metadataField) {
        CrisLayoutField field = mock(CrisLayoutField.class);

        when(field.getMetadataField()).thenReturn(metadataField);
        return field;
    }

    private Item item() {
        return mock(Item.class);
    }

    private RequestService requestService() {
        RequestService requestService = mock(RequestService.class);
        Request currentRequest = mock(Request.class);
        when(requestService.getCurrentRequest())
                .thenReturn(currentRequest);
        when(currentRequest.getAttribute("context")).thenReturn(context);
        return requestService;
    }


    /**
     * Custom matcher for {@link MetadatumDTO} class
     */
    private static class MetadatumDTOMatcher extends TypeSafeMatcher<MetadatumDTO> {

        private final String schema;
        private final String element;
        private final String qualifier;
        private final String value;

        private MetadatumDTOMatcher(String schema, String element, String qualifier, String value) {
            this.schema = schema;
            this.element = element;
            this.qualifier = qualifier;
            this.value = value;
        }

        static MetadatumDTOMatcher of(String schema, String element, String value) {
            return new MetadatumDTOMatcher(schema, element, null, value);
        }

        @Override
        protected boolean matchesSafely(MetadatumDTO metadatumDTO) {
            return StringUtils.equals(metadatumDTO.getSchema(), schema)
                    && StringUtils.equals(metadatumDTO.getElement(), element)
                    && StringUtils.equals(metadatumDTO.getValue(), value)
                    && nullOrEqual(metadatumDTO.getQualifier(), qualifier);
        }

        private boolean nullOrEqual(String qualifier, String originalQualifier) {

            return (Objects.isNull(qualifier) && Objects.isNull(originalQualifier))
                    || StringUtils.equals(qualifier, originalQualifier);
        }

        @Override
        public void describeTo(Description description) {

            StringBuilder stringBuilder = new StringBuilder(schema).append(".")
                    .append(element);
            Optional.ofNullable(qualifier).ifPresent(q -> stringBuilder.append(".").append(q));

            description.appendText(stringBuilder.append("::")
                    .append(value).toString());
        }
    }

    /**
     * Mock class for {@link MetadataValue} so that an instance with a subset of data, only needed for this test
     * can be created.
     */
    private static class MockMetadataValue extends MetadataValue {

        public MockMetadataValue(MetadataField metadataField, String value) {
            setMetadataField(metadataField);
            setValue(value);
        }
    }

    /**
     * Mock class for {@link MetadataField} so that an instance with a subset of data, only needed for this test
     * can be created.
     */
    private static class MockMetadataField extends MetadataField {

        private final Integer fieldId;

        public MockMetadataField(Integer schemaId, Integer fieldId, String schema, String element) {
            this.fieldId = fieldId;
            super.setElement(element);
            super.setMetadataSchema(new MockMetadataSchema(schemaId, schema));
        }

        @Override
        public Integer getID() {
            return fieldId;
        }
    }

    /**
     * Mock class for {@link MetadataSchema} so that an instance with a subset of data, only needed for this test
     * can be created.
     */
    private static class MockMetadataSchema extends MetadataSchema {
        private final Integer schemaId;

        public MockMetadataSchema(Integer schemaId, String schema) {
            this.schemaId = schemaId;
            setName(schema);
            setNamespace(schema);
        }

        @Override
        public Integer getID() {
            return schemaId;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            final MockMetadataSchema other = (MockMetadataSchema) obj;
            if (!this.schemaId.equals(other.schemaId)) {
                return false;
            }
            if ((this.getNamespace() == null) ?
                    (other.getNamespace() != null) : !this.getNamespace().equals(other.getNamespace())) {
                return false;
            }
            return true;
        }
    }
}