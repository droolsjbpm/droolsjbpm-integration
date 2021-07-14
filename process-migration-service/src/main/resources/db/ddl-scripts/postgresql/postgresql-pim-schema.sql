    create sequence MIG_REP_ID_SEQ start 1 increment 50;
    create sequence MIGRATION_ID_SEQ start 1 increment 50;
    create sequence PLAN_ID_SEQ start 1 increment 50;

    create table migration_report_logs (
       report_id int8 not null,
        log text
    );

    create table migration_reports (
       id int8 not null,
        end_date timestamp,
        migration_id int8,
        process_instance_id int8,
        start_date timestamp,
        success boolean,
        primary key (id)
    );

    create table migrations (
       id int8 not null,
        cancelled_at timestamp,
        created_at timestamp,
        callback_url bytea,
        scheduled_start_time timestamp,
        execution_type int4,
        kieServerId varchar(255),
        plan_id int8,
        requester varchar(255),
        error_message text,
        finished_at timestamp,
        started_at timestamp,
        status int4,
        primary key (id)
    );

    create table plan_mappings (
       plan_id int8 not null,
        target varchar(255),
        source varchar(255) not null,
        primary key (plan_id, source)
    );

    create table plans (
       id int8 not null,
        description varchar(255),
        name varchar(255),
        source_container_id varchar(255),
        source_process_id varchar(255),
        target_container_id varchar(255),
        target_process_id varchar(255),
        primary key (id)
    );

    create table process_instance_ids (
       migration_definition_id int8 not null,
        processInstanceIds int8
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