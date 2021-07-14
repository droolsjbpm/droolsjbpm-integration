
    alter table migration_report_logs drop constraint FKj8bsydiucvs2kygnscp1bt1wy;
    alter table migration_reports drop constraint FK98ckwvu4fyt55u6sq680xwkmx;
    alter table plan_mappings drop constraint FKk892t85t9vt1xe6vf9nqwgqoh;
    alter table process_instance_ids drop constraint FKobucfuy73fgsmkncl9q2rv6ko;
    drop table if exists migration_report_logs cascade;
    drop table if exists migration_reports cascade;
    drop table if exists migrations cascade;
    drop table if exists plan_mappings cascade;
    drop table if exists plans cascade;
    drop table if exists process_instance_ids cascade;
    drop sequence if exists MIG_REP_ID_SEQ;
    drop sequence if exists MIGRATION_ID_SEQ;
    drop sequence if exists PLAN_ID_SEQ;
