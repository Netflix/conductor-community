ALTER TABLE public.workflow_archive ADD COLUMN parent_workflow_id varchar(255);

update workflow_archive set parent_workflow_id=json_data::json ->> 'parentWorkflowId' where true;

CREATE INDEX IF NOT EXISTS workflow_archive_parent_workflow_id_idx ON public.workflow_archive(parent_workflow_id ASC NULLS LAST);