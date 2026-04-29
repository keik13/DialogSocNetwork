create table soc_dialog
(
	id bigint GENERATED ALWAYS AS IDENTITY,
    user_id uuid not null,
    to_user_id uuid not null,
    dialog_id varchar(72) not null,
    message text not null,
    created_at bigint not null,
    PRIMARY KEY (dialog_id, id)
);

-- CREATE INDEX idx_soc_dialog_user_id_to_user_id ON soc_dialog(dialog_id, created_at);

select create_distributed_table('soc_dialog', 'dialog_id');