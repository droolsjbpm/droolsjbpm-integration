
    alter table migration_report_logs drop constraint FKj8bsydiucvs2kygnscp1bt1wy
        go
    alter table migration_reports drop constraint FK98ckwvu4fyt55u6sq680xwkmx
        go
    alter table plan_mappings drop constraint FKk892t85t9vt1xe6vf9nqwgqoh
        go
    alter table process_instance_ids drop constraint FKobucfuy73fgsmkncl9q2rv6ko
        go
    drop table MIG_REP_ID_SEQ
        go
    drop table MIGRATION_ID_SEQ
        go
    drop table migration_report_logs
        go
    drop table migration_reports
        go
    drop table migrations
        go
    drop table PLAN_ID_SEQ
        go
    drop table plan_mappings
        go
    drop table plans
        go
    drop table process_instance_ids
        go
