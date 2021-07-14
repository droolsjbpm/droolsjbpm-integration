
    create table MIG_REP_ID_SEQ (
       next_val numeric(19,0)
    );

    insert into MIG_REP_ID_SEQ values ( 1 );

    create table MIGRATION_ID_SEQ (
       next_val numeric(19,0)
    );

    insert into MIGRATION_ID_SEQ values ( 1 );

    create table PLAN_ID_SEQ (
       next_val numeric(19,0)
    );

    insert into PLAN_ID_SEQ values ( 1 );

    create table migration_report_logs (
       report_id numeric(19,0) not null,
        log text
    );

    create table migration_reports (
       id numeric(19,0) not null,
        end_date datetime,
        migration_id numeric(19,0),
        process_instance_id numeric(19,0),
        start_date datetime,
        success bit,
        primary key (id)
    );

    create table migrations (
       id numeric(19,0) not null,
        cancelled_at datetime,
        created_at datetime,
        callback_url varbinary(255),
        scheduled_start_time datetime,
        execution_type int,
        kieServerId varchar(255),
        plan_id numeric(19,0),
        requester varchar(255),
        error_message text,
        finished_at datetime,
        started_at datetime,
        status int,
        primary key (id)
    );

    create table plan_mappings (
       plan_id numeric(19,0) not null,
        target varchar(255),
        source varchar(255) not null,
        primary key (plan_id, source)
    );

    create table plans (
       id numeric(19,0) not null,
        description varchar(255),
        name varchar(255),
        source_container_id varchar(255),
        source_process_id varchar(255),
        target_container_id varchar(255),
        target_process_id varchar(255),
        primary key (id)
    );

    create table process_instance_ids (
       migration_definition_id numeric(19,0) not null,
        processInstanceIds numeric(19,0)
    );

    alter table migration_report_logs 
       add constraint FKj8bsydiucvs2kygnscp1bt1wy 
       foreign key (report_id) 
       references migration_reports;

    alter table migration_reports 
       add constraint FK98ckwvu4fyt55u6sq680xwkmx 
       foreign key (migration_id) 
       references migrations;

    alter table plan_mappings 
       add constraint FKk892t85t9vt1xe6vf9nqwgqoh 
       foreign key (plan_id) 
       references plans;

    alter table process_instance_ids 
       add constraint FKobucfuy73fgsmkncl9q2rv6ko 
       foreign key (migration_definition_id) 
       references migrations;

    create index IDXgiy3fyawbd9nt7mymgd9qd61h on migration_reports (migration_id);