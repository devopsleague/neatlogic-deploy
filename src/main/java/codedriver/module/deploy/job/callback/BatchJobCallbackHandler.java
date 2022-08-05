/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job.callback;

import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.job.LaneGroupVo;
import codedriver.module.deploy.dao.mapper.DeployBatchJobMapper;
import codedriver.module.deploy.service.DeployBatchJobService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author lvzk
 * @since 2022/7/27 17:40
 **/
@Component
public class BatchJobCallbackHandler extends AutoexecJobCallbackBase {
    @Resource
    private AutoexecJobMapper autoexecJobMapper;
    @Resource
    private DeployBatchJobService deployBatchJobService;
    @Resource
    private DeployBatchJobMapper deployBatchJobMapper;

    @Override
    public String getHandler() {
        return BatchJobCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        if (jobVo != null) {
            AutoexecJobVo autoexecJob = autoexecJobMapper.getJobInfo(jobVo.getId());
            if (Objects.equals(JobSource.DEPLOY.getValue(), autoexecJob.getSource()) && autoexecJob.getParentId() != null) {
                //作业回调
                AutoexecJobVo parentJobVo = autoexecJobMapper.getJobInfo(autoexecJob.getParentId());
                if (parentJobVo != null && Objects.equals(parentJobVo.getSource(), JobSource.BATCHDEPLOY.getValue())) {
                    return Arrays.asList(JobStatus.COMPLETED.getValue(),JobStatus.FAILED.getValue(),JobStatus.ABORTED.getValue()).contains(autoexecJob.getStatus()) ;
                }
            } else if (Objects.equals(autoexecJob.getSource(), JobSource.BATCHDEPLOY.getValue())) {
                //TODO 批量作业回调
            }
        }
        return false;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        Long jobId = jobVo.getId();
        LaneGroupVo laneGroupVo = deployBatchJobMapper.getLaneGroupByJobId(jobId);
        if(laneGroupVo != null) {
            deployBatchJobService.checkAndFireLaneNextGroup(laneGroupVo.getId());
        }
    }
}
