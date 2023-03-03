/*
Copyright(c) $today.year NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.deploy.schedule.plugin;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.autoexec.constvalue.JobAction;
import neatlogic.framework.autoexec.constvalue.JobStatus;
import neatlogic.framework.autoexec.constvalue.JobTriggerType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.dao.mapper.UserMapper;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.UserVo;
import neatlogic.framework.filter.core.LoginAuthHandlerBase;
import neatlogic.framework.scheduler.core.JobBase;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.service.AuthenticationInfoService;
import neatlogic.module.deploy.service.DeployBatchJobService;
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
    public Boolean isMyHealthy(JobObject jobObject) {
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
