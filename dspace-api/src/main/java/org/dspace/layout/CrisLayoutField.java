/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.Cacheable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.dspace.content.MetadataField;
import org.dspace.core.ReloadableEntity;
import org.hibernate.annotations.CacheConcurrencyStrategy;


@Entity
@Table(name = "cris_layout_field")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, include = "non-lazy")
public class CrisLayoutField implements ReloadableEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "cris_layout_field_field_id_seq")
    @SequenceGenerator(
        name = "cris_layout_field_field_id_seq",
        sequenceName = "cris_layout_field_field_id_seq",
        allocationSize = 1)
    @Column(name = "field_id", unique = true, nullable = false, insertable = true, updatable = false)
    private Integer id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "metadata_field_id", nullable = false)
    private MetadataField metadataField;
    @Column(name = "rendering")
    private String rendering;
    @Column(name = "row", nullable = false)
    private Integer row;
    @Column(name = "priority", nullable = false)
    private Integer priority;
    @Column(name = "label")
    private String label;
    @Column(name = "style")
    private String style;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private CrisLayoutBox box;
    @Column(name = "style_label")
    private String styleLabel;
    @Column(name = "style_value")
    private String styleValue;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "crisLayoutField", cascade = CascadeType.ALL)
    @OrderBy(value = "priority")
    private List<CrisMetadataGroup> crisMetadataGroupList = new ArrayList<>();
    @Override
    public Integer getID() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public MetadataField getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    public String getRendering() {
        return rendering;
    }

    public void setRendering(String rendering) {
        this.rendering = rendering;
    }

    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public CrisLayoutBox getBox() {
        return box;
    }

    public void setBox(CrisLayoutBox box) {
        this.box = box;
    }

    public String getStyleLabel() {
        return styleLabel;
    }

    public void setStyleLabel(String styleLabel) {
        this.styleLabel = styleLabel;
    }

    public String getStyleValue() {
        return styleValue;
    }

    public void setStyleValue(String styleValue) {
        this.styleValue = styleValue;
    }
    public List<CrisMetadataGroup> getCrisMetadataGroupList() {
        return crisMetadataGroupList;
    }

    public void setCrisMetadataGroupList(List<CrisMetadataGroup> crisMetadataGroupList) {
        this.crisMetadataGroupList = crisMetadataGroupList;
    }
}
