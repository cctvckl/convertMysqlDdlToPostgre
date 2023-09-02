CREATE TABLE xxl_job_info (
    id serial PRIMARY KEY,
    job_group int NOT NULL,
    job_desc varchar(255) NOT NULL,
    add_time timestamp NULL,
    update_time timestamp NULL,
    author varchar(64) NULL,
    alarm_email varchar(255) NULL,
    schedule_type varchar(50) NOT NULL DEFAULT 'NONE',
    schedule_conf varchar(128) NULL,
    misfire_strategy varchar(50) NOT NULL DEFAULT 'DO_NOTHING',
    executor_route_strategy varchar(50) NULL,
    executor_handler varchar(255) NULL,
    executor_param varchar(512) NULL,
    executor_block_strategy varchar(50) NULL,
    executor_timeout int NOT NULL DEFAULT '0',
    executor_fail_retry_count int NOT NULL DEFAULT '0',
    glue_type varchar(50) NOT NULL,
    glue_source text null,
    glue_remark varchar(128) NULL,
    glue_updatetime timestamp NULL,
    child_jobid varchar(255) NULL,
    trigger_status smallint NOT NULL DEFAULT '0',
    trigger_last_time bigint NOT NULL DEFAULT '0',
    trigger_next_time bigint NOT NULL DEFAULT '0'
);
COMMENT ON COLUMN xxl_job_info.job_group IS '执行器主键ID';
COMMENT ON COLUMN xxl_job_info.author IS '作者';
COMMENT ON COLUMN xxl_job_info.alarm_email IS '报警邮件';
COMMENT ON COLUMN xxl_job_info.schedule_type IS '调度类型';
COMMENT ON COLUMN xxl_job_info.schedule_conf IS '调度配置，值含义取决于调度类型';
COMMENT ON COLUMN xxl_job_info.misfire_strategy IS '调度过期策略';
COMMENT ON COLUMN xxl_job_info.executor_route_strategy IS '执行器路由策略';
COMMENT ON COLUMN xxl_job_info.executor_handler IS '执行器任务handler';
COMMENT ON COLUMN xxl_job_info.executor_param IS '执行器任务参数';
COMMENT ON COLUMN xxl_job_info.executor_block_strategy IS '阻塞处理策略';
COMMENT ON COLUMN xxl_job_info.executor_timeout IS '任务执行超时时间，单位秒';
COMMENT ON COLUMN xxl_job_info.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN xxl_job_info.glue_type IS 'GLUE类型';
COMMENT ON COLUMN xxl_job_info.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN xxl_job_info.glue_remark IS 'GLUE备注';
COMMENT ON COLUMN xxl_job_info.glue_updatetime IS 'GLUE更新时间';
COMMENT ON COLUMN xxl_job_info.child_jobid IS '子任务ID，多个逗号分隔';
COMMENT ON COLUMN xxl_job_info.trigger_status IS '调度状态：0-停止，1-运行';
COMMENT ON COLUMN xxl_job_info.trigger_last_time IS '上次调度时间';
COMMENT ON COLUMN xxl_job_info.trigger_next_time IS '下次调度时间';

CREATE TABLE xxl_job_log (
    id bigserial PRIMARY KEY,
    job_group int NOT NULL,
    job_id int NOT NULL,
    executor_address varchar(255) NULL,
    executor_handler varchar(255) NULL,
    executor_param varchar(512) NULL,
    executor_sharding_param varchar(20) NULL,
    executor_fail_retry_count int NOT NULL DEFAULT '0',
    trigger_time timestamp NULL,
    trigger_code int NOT NULL,
    trigger_msg text null,
    handle_time timestamp NULL,
    handle_code int NOT NULL,
    handle_msg text null,
    alarm_status smallint NOT NULL DEFAULT '0'
);
COMMENT ON COLUMN xxl_job_log.job_group IS '执行器主键ID';
COMMENT ON COLUMN xxl_job_log.job_id IS '任务，主键ID';
COMMENT ON COLUMN xxl_job_log.executor_address IS '执行器地址，本次执行的地址';
COMMENT ON COLUMN xxl_job_log.executor_handler IS '执行器任务handler';
COMMENT ON COLUMN xxl_job_log.executor_param IS '执行器任务参数';
COMMENT ON COLUMN xxl_job_log.executor_sharding_param IS '执行器任务分片参数，格式如 1/2';
COMMENT ON COLUMN xxl_job_log.executor_fail_retry_count IS '失败重试次数';
COMMENT ON COLUMN xxl_job_log.trigger_time IS '调度-时间';
COMMENT ON COLUMN xxl_job_log.trigger_code IS '调度-结果';
COMMENT ON COLUMN xxl_job_log.trigger_msg IS '调度-日志';
COMMENT ON COLUMN xxl_job_log.handle_time IS '执行-时间';
COMMENT ON COLUMN xxl_job_log.handle_code IS '执行-状态';
COMMENT ON COLUMN xxl_job_log.handle_msg IS '执行-日志';
COMMENT ON COLUMN xxl_job_log.alarm_status IS '告警状态：0-默认、1-无需告警、2-告警成功、3-告警失败';

CREATE TABLE xxl_job_log_report (
    id serial PRIMARY KEY,
    trigger_day timestamp NULL,
    running_count int NOT NULL DEFAULT '0',
    suc_count int NOT NULL DEFAULT '0',
    fail_count int NOT NULL DEFAULT '0',
    update_time timestamp NULL
);
COMMENT ON COLUMN xxl_job_log_report.trigger_day IS '调度-时间';
COMMENT ON COLUMN xxl_job_log_report.running_count IS '运行中-日志数量';
COMMENT ON COLUMN xxl_job_log_report.suc_count IS '执行成功-日志数量';
COMMENT ON COLUMN xxl_job_log_report.fail_count IS '执行失败-日志数量';

CREATE TABLE xxl_job_logglue (
    id serial PRIMARY KEY,
    job_id int NOT NULL,
    glue_type varchar(50) NULL,
    glue_source text null,
    glue_remark varchar(128) NOT NULL,
    add_time timestamp NULL,
    update_time timestamp NULL
);
COMMENT ON COLUMN xxl_job_logglue.job_id IS '任务，主键ID';
COMMENT ON COLUMN xxl_job_logglue.glue_type IS 'GLUE类型';
COMMENT ON COLUMN xxl_job_logglue.glue_source IS 'GLUE源代码';
COMMENT ON COLUMN xxl_job_logglue.glue_remark IS 'GLUE备注';

CREATE TABLE xxl_job_registry (
    id serial PRIMARY KEY,
    registry_group varchar(50) NOT NULL,
    registry_key varchar(255) NOT NULL,
    registry_value varchar(255) NOT NULL,
    update_time timestamp NULL
);

CREATE TABLE xxl_job_group (
    id serial PRIMARY KEY,
    app_name varchar(64) NOT NULL,
    title varchar(12) NOT NULL,
    address_type smallint NOT NULL DEFAULT '0',
    address_list text null,
    update_time timestamp NULL
);
COMMENT ON COLUMN xxl_job_group.app_name IS '执行器AppName';
COMMENT ON COLUMN xxl_job_group.title IS '执行器名称';
COMMENT ON COLUMN xxl_job_group.address_type IS '执行器地址类型：0=自动注册、1=手动录入';
COMMENT ON COLUMN xxl_job_group.address_list IS '执行器地址列表，多地址逗号分隔';

CREATE TABLE xxl_job_user (
    id serial PRIMARY KEY,
    username varchar(50) NOT NULL,
    password varchar(50) NOT NULL,
    role smallint NOT NULL,
    permission varchar(255) NULL
);
COMMENT ON COLUMN xxl_job_user.username IS '账号';
COMMENT ON COLUMN xxl_job_user.password IS '密码';
COMMENT ON COLUMN xxl_job_user.role IS '角色：0-普通用户、1-管理员';
COMMENT ON COLUMN xxl_job_user.permission IS '权限：执行器ID列表，多个逗号分割';

CREATE TABLE xxl_job_lock (
    lock_name varchar (50) PRIMARY KEY,
);
COMMENT ON COLUMN xxl_job_lock.lock_name IS '锁名称';

