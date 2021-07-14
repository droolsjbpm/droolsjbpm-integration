
    alter table migration_report_logs drop constraint FKj8bsydiucvs2kygnscp1bt1wy;
    alter table migration_reports drop constraint FK98ckwvu4fyt55u6sq680xwkmx;
    alter table plan_mappings drop constraint FKk892t85t9vt1xe6vf9nqwgqoh;
    alter table process_instance_ids drop constraint FKobucfuy73fgsmkncl9q2rv6ko;
    drop table migration_report_logs if exists;
    drop table migration_reports if exists;
    drop table migrations if exists;
    drop table plan_mappings if exists;
    drop table plans if exists;
    drop table process_instance_ids if exists;
    drop sequence MIG_REP_ID_SEQ if exists;
    drop sequence MIGRATION_ID_SEQ if exists;
    drop sequence PLAN_ID_SEQ if exists;
