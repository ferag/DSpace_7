/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.elasticsearch;
import java.util.Date;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.dspace.core.ReloadableEntity;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
//@Entity
//@Table(name = "elasticsearch_index_queue")
public class ElasticsearchIndexQueue implements ReloadableEntity<UUID> {

    @Id
    @Column(name = "item_uuid")
    private UUID id;

    @Column(name = "operation_type")
    private Integer operationType;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "timestamp_insertion_date")
    private Date insertionDate;

    @Override
    public UUID getID() {
        return getId();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Integer getOperationType() {
        return operationType;
    }

    public void setOperationType(Integer operationType) {
        this.operationType = operationType;
    }

    public Date getInsertionDate() {
        return insertionDate;
    }

    public void setInsertionDate(Date insertionDate) {
        this.insertionDate = insertionDate;
    }

}