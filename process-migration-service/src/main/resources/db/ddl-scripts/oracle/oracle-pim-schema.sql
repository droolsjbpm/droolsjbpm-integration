    create sequence MIG_REP_ID_SEQ start with 1 increment by  50;
    create sequence MIGRATION_ID_SEQ start with 1 increment by  50;
    create sequence PLAN_ID_SEQ start with 1 increment by  50;

    create table migration_report_logs (
       report_id number(19,0) not null,
        log clob
    );

    create table migration_reports (
       id number(19,0) not null,
        end_date timestamp,
        migration_id number(19,0),
        process_instance_id number(19,0),
        start_date timestamp,
        success number(1,0),
        primary key (id)
    );

    create table migrations (
       id number(19,0) not null,
        cancelled_at timestamp,
        created_at timestamp,
        callback_url raw(255),
        scheduled_start_time timestamp,
        execution_type number(10,0),
        kieServerId varchar2(255 char),
        plan_id number(19,0),
        requester varchar2(255 char),
        error_message clob,
        finished_at timestamp,
        started_at timestamp,
        status number(10,0),
        primary key (id)
    );

    create table plan_mappings (
       plan_id number(19,0) not null,
        target varchar2(255 char),
        source varchar2(255 char) not null,
        primary key (plan_id, source)
    );

    create table plans (
       id number(19,0) not null,
        description varchar2(255 char),
        name varchar2(255 char),
        source_container_id varchar2(255 char),
        source_process_id varchar2(255 char),
        target_container_id varchar2(255 char),
        target_process_id varchar2(255 char),
        primary key (id)
    );

    create table process_instance_ids (
       migration_definition_id number(19,0) not null,
        processInstanceIds number(19,0)
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