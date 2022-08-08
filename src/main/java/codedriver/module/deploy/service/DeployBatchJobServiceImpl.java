/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobAction;
import codedriver.framework.autoexec.constvalue.JobPhaseStatus;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.action.core.AutoexecJobActionHandlerFactory;
import codedriver.framework.autoexec.job.action.core.IAutoexecJobActionHandler;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.job.LaneGroupVo;
import codedriver.framework.deploy.dto.job.LaneVo;
import codedriver.framework.deploy.exception.DeployBatchJobCannotExecuteException;
import codedriver.framework.deploy.exception.DeployBatchJobFireWithRevokedException;
import codedriver.framework.deploy.exception.DeployBatchJobGroupFireWithInvalidStatusException;
import codedriver.framework.deploy.exception.DeployBatchJobGroupNotFoundException;
import codedriver.module.deploy.dao.mapper.DeployBatchJobMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DeployBatchJobServiceImpl implements DeployBatchJobService {
    static Logger logger = LoggerFactory.getLogger(DeployBatchJobServiceImpl.class);
    @Resource
    DeployJobMapper deployJobMapper;
    @Resource
    AutoexecJobMapper autoexecJobMapper;
    @Resource
    DeployBatchJobMapper deployBatchJobMapper;

    @Override
    public void fireBatch(Long batchJobId, String batchJobAction, String jobAction) {
        deployBatchJobMapper.getBatchDeployJobLockById(batchJobId);
        AutoexecJobVo batchJobVo = autoexecJobMapper.getJobInfo(batchJobId);
        //不允许存在"已撤销"的作业
        List<AutoexecJobVo> autoexecJobList = deployBatchJobMapper.getBatchDeployJobListByIdAndStatus(batchJobId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        if (CollectionUtils.isNotEmpty(autoexecJobList)) {
            throw new DeployBatchJobFireWithRevokedException(autoexecJobList);
        }
        //更新批量发布父作业状态
        String loginUserUuid = UserContext.get().getUserUuid();
        if (!Objects.equals(loginUserUuid, batchJobVo.getExecUser())) {
            throw new DeployBatchJobCannotExecuteException();
        }
        batchJobVo.setStatus(JobStatus.RUNNING.getValue());
        autoexecJobMapper.updateJobStatus(batchJobVo);
        //循环泳道，获取每个泳道第一个组fire
        List<LaneVo> laneVos = deployBatchJobMapper.getLaneListByBatchJobId(batchJobVo.getId());
        for (LaneVo laneVo : laneVos) {
            Long fireGroupId = deployBatchJobMapper.getLaneFireGroupId(batchJobVo.getId(), laneVo.getId());
            deployBatchJobMapper.updateLaneStatus(laneVo.getId(), JobStatus.RUNNING.getValue());
            deployBatchJobMapper.updateGroupStatusByLaneId(laneVo.getId(), JobStatus.PENDING.getValue());
            fireLaneGroup(fireGroupId, batchJobAction, jobAction, new JSONObject());
        }
    }

    @Override
    public void fireLaneGroup(Long groupId, String batchJobAction, String jobAction, JSONObject PassThroughEnv) {
        LaneGroupVo groupVo = deployBatchJobMapper.getLaneGroupByGroupId(groupId);
        if (groupVo == null) {
            throw new DeployBatchJobGroupNotFoundException(groupId);
        }
        groupVo.setBatchJobAction(batchJobAction);
        groupVo.setJobAction(jobAction);
        fireLaneGroup(groupVo, 0, PassThroughEnv);
    }

    @Override
    public void refireLaneGroup(Long groupId, int isGoon, String batchJobAction, String jobAction) {
        LaneGroupVo groupVo = deployBatchJobMapper.getLaneGroupByGroupId(groupId);
        if (groupVo == null) {
            throw new DeployBatchJobGroupNotFoundException(groupId);
        }
        groupVo.setBatchJobAction(batchJobAction);
        groupVo.setJobAction(jobAction);
        groupVo.setIsGoon(isGoon);
        fireLaneGroup(groupVo, 1, new JSONObject());
    }

    @Override
    public void fireLaneGroup(LaneGroupVo groupVo, int isRefire, JSONObject passThroughEnv) {
        Long groupId = groupVo.getId();
        if (Objects.equals(groupVo.getStatus(), JobStatus.COMPLETED.getValue())) {
            logger.info("Batch run fire group:#" + groupId + " status succeed, ignore.");
            //无需等待，直接执行下一个组
            if (groupVo.getIsGoon() == 1 && groupVo.getNeedWait() != 1) {
                checkAndFireLaneNextGroup(groupVo, passThroughEnv);
            }
            return;
        }

        // 如果待触发的group的状态是running，则停止触发
        if (Objects.equals(JobStatus.RUNNING.getValue(), groupVo.getStatus())) {
            if (isRefire == 1) {
                throw new DeployBatchJobGroupFireWithInvalidStatusException(groupVo.getStatus());
            }
            return;
        }

        // 如果属于正常触发，则继续执行以下逻辑
        logger.info("Batch run fire group:#" + groupId);
        groupVo.setStatus(JobStatus.RUNNING.getValue());
        deployBatchJobMapper.updateGroupStatus(groupVo);
        deployBatchJobMapper.updateLaneStatus(groupVo.getLaneId(), JobStatus.RUNNING.getValue());

        List<AutoexecJobVo> jobVoList = deployBatchJobMapper.getJobsByGroupIdAndWithoutStatus(groupId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        //循环执行作业
        int completedContinueCount = 0;
        if (CollectionUtils.isNotEmpty(jobVoList)) {
            List<DeployJobVo> deployJobVos = deployJobMapper.getDeployJobByJobIdList(jobVoList.stream().map(AutoexecJobVo::getId).collect(Collectors.toList()));
            Map<Long, String> deployJobIdPathMap = deployJobVos.stream().collect(Collectors.toMap(AutoexecJobVo::getId, o -> o.getAppSystemId() + "/" + o.getAppModuleId() + "/" + o.getEnvId()));
            if (MapUtils.isEmpty(passThroughEnv)) {
                passThroughEnv = new JSONObject();
            }
            if (StringUtils.isBlank(groupVo.getBatchJobAction())) {
                groupVo.setBatchJobAction(passThroughEnv.getString("BATCH_JOB_ACTION"));
            } else {
                passThroughEnv.put("BATCH_JOB_ACTION", groupVo.getBatchJobAction());
            }
            if (StringUtils.isBlank(groupVo.getJobAction())) {
                groupVo.setJobAction(passThroughEnv.getString("JOB_ACTION"));
            } else {
                passThroughEnv.put("JOB_ACTION", groupVo.getJobAction());
            }
            for (AutoexecJobVo jobVo : jobVoList) {
                try {
                    //跳过所有已完成的子作业
                    if (Objects.equals(groupVo.getBatchJobAction(), JobAction.REFIRE.getValue()) && Objects.equals(jobVo.getStatus(), JobStatus.COMPLETED.getValue())) {
                        completedContinueCount++;
                        continue;
                    }
                    passThroughEnv.put("DEPLOY_ID_PATH", deployJobIdPathMap.get(jobVo.getId()));
                    jobVo.setAction(groupVo.getJobAction());
                    IAutoexecJobActionHandler refireAction = AutoexecJobActionHandlerFactory.getAction(JobAction.REFIRE.getValue());
                    jobVo.setPassThroughEnv(passThroughEnv);
                    jobVo.setIsTakeOver(1);
                    refireAction.doService(jobVo);
                } catch (Exception ex) {
                    logger.error("Fire job by batch failed," + ex.getMessage(), ex);
                }
            }
        }
        //如果作业执行策略是refireAll且所有作业都是已完成，则
        if (completedContinueCount == jobVoList.size()) {
            checkAndFireLaneNextGroup(groupVo, passThroughEnv);
        }
    }

    @Override
    public void checkAndFireLaneNextGroup(Long groupId, JSONObject passThroughEnv) {
        LaneGroupVo groupVo = deployBatchJobMapper.getLaneGroupByGroupId(groupId);
        if (groupVo != null) {
            checkAndFireLaneNextGroup(groupVo, passThroughEnv);
        }
    }

    @Override
    public void checkAndFireLaneNextGroup(LaneGroupVo groupVo, JSONObject passThroughEnv) {
        Long groupId = groupVo.getId();
        List<AutoexecJobVo> groupJobs = deployBatchJobMapper.getJobsByGroupIdAndWithoutStatus(groupId, Collections.singletonList(JobStatus.REVOKED.getValue()));
        //初始化 状态数量map
        Map<String, Integer> statusCountMap = new HashMap<>();
        for (JobPhaseStatus jobStatus : JobPhaseStatus.values()) {
            statusCountMap.put(jobStatus.getValue(), 0);
        }
        //List<Long> failedJobsId = new ArrayList<>();
        for (AutoexecJobVo jobVo : groupJobs) {
//            if (Objects.equals(JobStatus.FAILED.getValue(), jobVo.getStatus())) {
//                failedJobsId.add(jobVo.getId());
//            }
            statusCountMap.put(jobVo.getStatus(), statusCountMap.get(jobVo.getStatus()) + 1);
        }
        //根据状态数量map 获取最终组状态
        String groupStatus = groupVo.getStatus();
        if (statusCountMap.get(JobStatus.RUNNING.getValue()) > 0) {
            groupStatus = JobStatus.RUNNING.getValue();
        } else if (statusCountMap.get(JobStatus.PENDING.getValue()) > 0) {
            if (Objects.equals(JobStatus.RUNNING.getValue(), groupVo.getStatus())) {
                groupStatus = JobStatus.RUNNING.getValue();
            } else {
                groupStatus = JobStatus.PENDING.getValue();
            }
        } else if (statusCountMap.get(JobStatus.FAILED.getValue()) > 0 || statusCountMap.get(JobStatus.ABORTED.getValue()) > 0) {
            groupStatus = JobStatus.FAILED.getValue();
        } else if (statusCountMap.get(JobStatus.COMPLETED.getValue()) == groupJobs.size()) {
            groupStatus = JobStatus.COMPLETED.getValue();
        }

        //如果组状态不一致（防止重复调用），并且组状态是completed则判断是否fire下一组
        if (!groupStatus.equals(groupVo.getStatus())) {
            Long nextGroupId = deployBatchJobMapper.getNextGroupId(groupVo.getLaneId(), groupVo.getSort());
            logger.info("Batch run update group:#" + groupId + " status:" + groupStatus);
            groupVo.setStatus(groupStatus);
            deployBatchJobMapper.updateGroupStatus(groupVo);
            if (Objects.equals(groupStatus, JobStatus.COMPLETED.getValue())) {
                if (nextGroupId == null) {
                    fireLaneNextGroup(groupVo, nextGroupId, passThroughEnv);
                } else if (groupVo.getIsGoon() == 1 && groupVo.getNeedWait() == 0) {
                    fireLaneNextGroup(groupVo, nextGroupId, passThroughEnv);
                }
            }
        }
    }

    /**
     * 激活该泳道下一组
     *
     * @param currentGroupVo 当前组
     * @param nextGroupId    下一组id
     */
    public void fireLaneNextGroup(LaneGroupVo currentGroupVo, Long nextGroupId, JSONObject passThroughEnv) {
        if (nextGroupId != null) {
            logger.info("Next group found:#" + nextGroupId + ", for lane:#" + currentGroupVo.getLaneId() + " pre sort:#" + currentGroupVo.getSort());
            fireLaneGroup(nextGroupId, currentGroupVo.getBatchJobAction(), currentGroupVo.getJobAction(), passThroughEnv);
        } else {
            deployBatchJobMapper.updateLaneStatus(currentGroupVo.getLaneId(), JobStatus.COMPLETED.getValue());
            logger.info("Batch run lane:#" + currentGroupVo.getLaneId() + " finished.");
            LaneVo laneVo = deployBatchJobMapper.getLaneById(currentGroupVo.getLaneId());
            Long batchJobId = laneVo.getBatchJobId();
            List<LaneVo> laneList = deployBatchJobMapper.getLaneListByBatchJobId(batchJobId);
            boolean isCompleted = true;
            for (LaneVo lane : laneList) {
                if (!Objects.equals(JobStatus.COMPLETED.getValue(), lane.getStatus())) {
                    isCompleted = false;
                    break;
                }
            }
            if (isCompleted) {
                AutoexecJobVo batchJobVo = new AutoexecJobVo();
                batchJobVo.setId(batchJobId);
                batchJobVo.setStatus(JobStatus.COMPLETED.getValue());
                autoexecJobMapper.updateJobStatus(batchJobVo);
            }
        }
    }


}
