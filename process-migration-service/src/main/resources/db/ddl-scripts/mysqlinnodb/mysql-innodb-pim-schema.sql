
    create table MIG_REP_ID_SEQ (
       next_val bigint
    ) type=InnoDB;

    insert into MIG_REP_ID_SEQ values ( 1 );

    create table MIGRATION_ID_SEQ (
       next_val bigint
    ) type=InnoDB;

    insert into MIGRATION_ID_SEQ values ( 1 );

    create table PLAN_ID_SEQ (
       next_val bigint
    ) type=InnoDB;

    insert into PLAN_ID_SEQ values ( 1 );

    create table migration_report_logs (
       report_id bigint not null,
        log longtext
    ) type=InnoDB;

    create table migration_reports (
       id bigint not null,
        end_date datetime,
        migration_id bigint,
        process_instance_id bigint,
        start_date datetime,
        success bit,
        primary key (id)
    ) type=InnoDB;

    create table migrations (
       id bigint not null,
        cancelled_at datetime,
        created_at datetime,
        callback_url tinyblob,
        scheduled_start_time datetime,
        execution_type integer,
        kieServerId varchar(255),
        plan_id bigint,
        requester varchar(255),
        error_message longtext,
        finished_at datetime,
        started_at datetime,
        status integer,
        primary key (id)
    ) type=InnoDB;

    create table plan_mappings (
       plan_id bigint not null,
        target varchar(255),
        source varchar(255) not null,
        primary key (plan_id, source)
    ) type=InnoDB;

    create table plans (
       id bigint not null,
        description varchar(255),
        name varchar(255),
        source_container_id varchar(255),
        source_process_id varchar(255),
        target_container_id varchar(255),
        target_process_id varchar(255),
        primary key (id)
    ) type=InnoDB;

    create table process_instance_ids (
       migration_definition_id bigint not null,
        processInstanceIds bigint
    ) type=InnoDB;

    alter table migration_report_logs 
       add constraint FKj8bsydiucvs2kygnscp1bt1wy 
       foreign key (report_id) 
       references migration_reports (id);

    alter table migration_reports 
       add constraint FK98ckwvu4fyt55u6sq680xwkmx 
       foreign key (migration_id) 
       references migrations (id);

    alter table plan_mappings 
       add constraint FKk892t85t9vt1xe6vf9nqwgqoh 
       foreign key (plan_id) 
       references plans (id);

    alter table process_instance_ids 
       add constraint FKobucfuy73fgsmkncl9q2rv6ko 
       foreign key (migration_definition_id) 
       references migrations (id);

    create index IDXgiy3fyawbd9nt7mymgd9qd61h on migration_reports (migration_id);