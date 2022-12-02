-- Workflow
CREATE TABLE workflow_archive (
  workflow_id varchar(255) NOT NULL,
  created_on bigint,
  modified_on bigint,
  created_by varchar(255),
  correlation_id varchar(255),
  workflow_name varchar(255),
  status varchar(255),
  json_data TEXT,
  index_data     text[],
  PRIMARY KEY (workflow_id)
) with (autovacuum_vacuum_scale_factor = 0.0, autovacuum_vacuum_threshold = 10000);

CREATE INDEX workflow_archive_workflow_name_index ON workflow_archive (workflow_name, status, correlation_id, created_on);
CREATE INDEX workflow_archive_search_index ON workflow_archive USING gin(index_data);
CREATE INDEX workflow_archive_created_on_index ON workflow_archive (created_on desc);

-- Task Logs
CREATE TABLE task_logs (
  task_id varchar(255) NOT NULL,
  seq bigserial,
  log TEXT,
  created_on bigint,
  PRIMARY KEY (task_id, seq)
);