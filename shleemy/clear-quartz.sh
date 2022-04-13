#!/bin/bash
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QTASKS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_LOCKS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_SCHEDULER_STATE;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_FIRED_TRIGGERS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_PAUSED_TRIGGER_GRPS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_CALENDARS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_BLOB_TRIGGERS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_SIMPROP_TRIGGERS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_CRON_TRIGGERS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_SIMPLE_TRIGGERS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_TRIGGERS;';
mysql -P 3310 -h 127.0.0.1 -u genny --password=password gennydb  -e 'drop table QRTZ_JOB_DETAILS;';
