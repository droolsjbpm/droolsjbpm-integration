
    create table MIG_REP_ID_SEQ (
       next_val numeric(19,0) null
    ) lock datarows
    go

    insert into MIG_REP_ID_SEQ values ( 1 )
    go

    create table MIGRATION_ID_SEQ (
       next_val numeric(19,0) null
    ) lock datarows
    go

    insert into MIGRATION_ID_SEQ values ( 1 )
    go

    create table PLAN_ID_SEQ (
       next_val numeric(19,0) null
    ) lock datarows
    go

    insert into PLAN_ID_SEQ values ( 1 )
    go

    create table migration_report_logs (
       report_id numeric(19,0) not null,
        log text null
    ) lock datarows
    go

    create table migration_reports (
       id numeric(19,0) not null,
        end_date datetime null,
        migration_id numeric(19,0) null,
        process_instance_id numeric(19,0) null,
        start_date datetime null,
        success boolean null,
        primary key (id)
    ) lock datarows
    go

    create table migrations (
       id numeric(19,0) not null,
        cancelled_at datetime null,
        created_at datetime null,
        callback_url varbinary(255) null,
        scheduled_start_time datetime null,
        execution_type int null,
        kieServerId varchar(255) null,
        plan_id numeric(19,0) null,
        requester varchar(255) null,
        error_message text null,
        finished_at datetime null,
        started_at datetime null,
        status int null,
        primary key (id)
    ) lock datarows
    go

    create table plan_mappings (
       plan_id numeric(19,0) not null,
        target varchar(255) null,
        source varchar(255) not null,
        primary key (plan_id, source)
    ) lock datarows
    go

    create table plans (
       id numeric(19,0) not null,
        description varchar(255) null,
        name varchar(255) null,
        source_container_id varchar(255) null,
        source_process_id varchar(255) null,
        target_container_id varchar(255) null,
        target_process_id varchar(255) null,
        primary key (id)
    ) lock datarows
    go

    create table process_instance_ids (
       migration_definition_id numeric(19,0) not null,
        processInstanceIds numeric(19,0) null
    ) lock datarows
    go

    alter table migration_report_logs
       add constraint FKj8bsydiucvs2kygnscp1bt1wy 
       foreign key (report_id) 
       references migration_reports
    go

    alter table migration_reports 
       add constraint FK98ckwvu4fyt55u6sq680xwkmx 
       foreign key (migration_id) 
       references migrations
    go

    alter table plan_mappings 
       add constraint FKk892t85t9vt1xe6vf9nqwgqoh 
       foreign key (plan_id) 
       references plans
    go

    alter table process_instance_ids 
       add constraint FKobucfuy73fgsmkncl9q2rv6ko 
       foreign key (migration_definition_id) 
       references migrations
    go

    create index IDXgiy3fyawbd9nt7mymgd9qd61h on migration_reports (migration_id)
    go