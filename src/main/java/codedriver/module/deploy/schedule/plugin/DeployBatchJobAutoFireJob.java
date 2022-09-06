/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.schedule.plugin;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.dao.mapper.UserMapper;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.dto.UserVo;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.scheduler.core.JobBase;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.service.AuthenticationInfoService;
import codedriver.module.deploy.service.DeployBatchJobService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 在组合工具保存的作业，设置为自动触发后，创建本Job，到达计划时间后自动执行作业
 *
 * @author lvzk
 * @since 2022/7/18 17:42
 **/
@Component
@DisallowConcurrentExecution
public class DeployBatchJobAutoFireJob extends JobBase {
    static Logger logger = LoggerFactory.getLogger(DeployBatchJobAutoFireJob.class);

    @Resource
    private UserMapper userMapper;

    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private AuthenticationInfoService authenticationInfoService;

    @Resource
    private DeployBatchJobService deployBatchJobService;

    @Override
    public String getGroupName() {
        return TenantContext.get().getTenantUuid() + "-DEPLOY-BATCHJOBAUTOFIRE-JOB";
    }

    @Override
    public Boolean isHealthy(JobObject jobObject) {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(Long.valueOf(jobObject.getJobName()));
        return jobVo != null && JobStatus.READY.getValue().equals(jobVo.getStatus()) && JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType());
    }

    @Override
    public void reloadJob(JobObject jobObject) {
        //判断作业是否已经存在，存在则unload
        if (schedulerManager.checkJobIsExists(jobObject.getJobName(),this.getGroupName())) {
            schedulerManager.unloadJob(jobObject);
        }
        String tenantUuid = jobObject.getTenantUuid();
        TenantContext.get().switchTenant(tenantUuid);
        Long jobId = Long.valueOf(jobObject.getJobName());
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(jobId);
        if (jobVo != null && JobStatus.READY.getValue().equals(jobVo.getStatus()) && JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType())) {
            try {
                if (jobVo.getPlanStartTime().after(new Date())) {
                    JobObject.Builder newJobObjectBuilder = new JobObject
                            .Builder(jobId.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid())
                            .withBeginTime(jobVo.getPlanStartTime())
                            .withIntervalInSeconds(60 * 60)
                            .withRepeatCount(0);
                    JobObject newJobObject = newJobObjectBuilder.build();
                    schedulerManager.loadJob(newJobObject);
                } else {
                    deployBatchJobService.fireBatch(jobId, JobAction.RESET_REFIRE.getValue(), JobAction.RESET_REFIRE.getValue());
                }
            } catch (Exception ex) {
                logger.error(ExceptionUtils.getStackTrace(ex));
            }
        }
    }

    @Override
    public void initJob(String tenantUuid) {
        List<Long> list = autoexecJobMapper.getBatchJobIdListByStatusAndTriggerType(JobStatus.READY.getValue(), JobTriggerType.AUTO.getValue());
        for (Long id : list) {
            JobObject.Builder jobObjectBuilder = new JobObject.Builder(id.toString(), this.getGroupName(), this.getClassName(), TenantContext.get().getTenantUuid());
            JobObject jobObject = jobObjectBuilder.build();
            this.reloadJob(jobObject);
        }
    }

    @Override
    public void executeInternal(JobExecutionContext context, JobObject jobObject) throws Exception {
        AutoexecJobVo jobVo = autoexecJobMapper.getJobInfo(Long.valueOf(jobObject.getJobName()));
        if (jobVo != null && JobStatus.READY.getValue().equals(jobVo.getStatus()) && JobTriggerType.AUTO.getValue().equals(jobVo.getTriggerType())) {
            fireJob(jobVo);
        }
        schedulerManager.unloadJob(jobObject);
    }

    private void fireJob(AutoexecJobVo jobVo) throws Exception {
        UserVo execUser = userMapper.getUserBaseInfoByUuid(jobVo.getExecUser());
        if (execUser != null) {
            AuthenticationInfoVo authenticationInfo = authenticationInfoService.getAuthenticationInfo(execUser.getUuid());
            UserContext.init(execUser, authenticationInfo, SystemUser.SYSTEM.getTimezone());
            UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(execUser).getCc());
            deployBatchJobService.fireBatch(jobVo.getId(), JobAction.RESET_REFIRE.getValue(), JobAction.RESET_REFIRE.getValue());
        }
    }

}
