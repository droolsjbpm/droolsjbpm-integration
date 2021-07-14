
    alter table migration_report_logs drop foreign key FKj8bsydiucvs2kygnscp1bt1wy;
    alter table migration_reports drop foreign key FK98ckwvu4fyt55u6sq680xwkmx;
    alter table plan_mappings drop foreign key FKk892t85t9vt1xe6vf9nqwgqoh;
    alter table process_instance_ids drop foreign key FKobucfuy73fgsmkncl9q2rv6ko;
    drop table if exists MIG_REP_ID_SEQ;
    drop table if exists MIGRATION_ID_SEQ;
    drop table if exists migration_report_logs;
    drop table if exists migration_reports;
    drop table if exists migrations;
    drop table if exists PLAN_ID_SEQ;
    drop table if exists plan_mappings;
    drop table if exists plans;
    drop table if exists process_instance_ids;
