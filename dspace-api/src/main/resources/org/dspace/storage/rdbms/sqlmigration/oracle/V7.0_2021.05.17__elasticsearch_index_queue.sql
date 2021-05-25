--
-- The contents of this file are subject to the license and copyright
-- detailed in the LICENSE and NOTICE files at the root of the source
-- tree and available online at
--
-- http://www.dspace.org/license/
--

-----------------------------------------------------------------------------------
-- Create tables for ElasticsearchIndex Queue
-----------------------------------------------------------------------------------

CREATE SEQUENCE elasticsearch_queue_id_seq;

CREATE TABLE elasticsearch_index_queue
(
    id INTEGER NOT NULL,
    item_id uuid NOT NULL,
    operation_type INTEGER,
    timestamp_insertion_date TIMESTAMP,
    CONSTRAINT elasticsearch_index_queue_pkey PRIMARY KEY (id),
    CONSTRAINT elasticsearch_queue_item_id_fkey FOREIGN KEY (item_id) REFERENCES item (uuid)
);

CREATE INDEX elasticsearch_queue_item_id_index on elasticsearch_index_queue(item_id);

CREATE SEQUENCE elasticsearch_index_queue_id_seq;