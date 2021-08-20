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

CREATE TABLE elasticsearch_index_queue
(
    item_uuid UUID NOT NULL primary key,
    operation_type INTEGER,
    timestamp_insertion_date TIMESTAMP
);