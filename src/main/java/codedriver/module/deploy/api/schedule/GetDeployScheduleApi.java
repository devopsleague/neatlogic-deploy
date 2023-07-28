/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.schedule;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.auth.PIPELINE_MODIFY;
import codedriver.framework.deploy.constvalue.DeployAppConfigAction;
import codedriver.framework.deploy.constvalue.PipelineType;
import codedriver.framework.deploy.constvalue.ScheduleType;
import codedriver.framework.deploy.dto.schedule.DeployScheduleConfigVo;
import codedriver.framework.deploy.dto.schedule.DeployScheduleVo;
import codedriver.framework.deploy.exception.DeployScheduleNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.auth.core.DeployAppAuthChecker;
import codedriver.module.deploy.dao.mapper.DeployScheduleMapper;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployScheduleApi extends PrivateApiComponentBase {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;
    @Resource
    private DeployPipelineMapper pipelineMapper;

    @Override
    public String getToken() {
        return "deploy/schedule/get";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "定时作业id")
    })
    @Description(desc = "获取定时作业信息")
    @Output({
            @Param(name = "Return", explode = DeployScheduleVo.class, desc = "定时作业信息")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        DeployScheduleVo scheduleVo = deployScheduleMapper.getScheduleById(id);
        if (scheduleVo == null) {
            throw new DeployScheduleNotFoundException(id);
        }
        String userUuid = UserContext.get().getUserUuid(true);
        String type = scheduleVo.getType();
        if (type.equals(ScheduleType.GENERAL.getValue())) {
            Long appSystemId = scheduleVo.getAppSystemId();
            DeployScheduleConfigVo config = scheduleVo.getConfig();
            Set<String> actionSet = DeployAppAuthChecker.builder(appSystemId)
                    .addEnvAction(config.getEnvId())
                    .addScenarioAction(config.getScenarioId())
                    .check();
            if (actionSet.contains(config.getEnvId().toString()) && actionSet.contains(config.getScenarioId().toString())) {
                scheduleVo.setEditable(1);
                scheduleVo.setDeletable(1);
            }
        } else if(type.equals(ScheduleType.PIPELINE.getValue())) {
            String pipelineType = scheduleVo.getPipelineType();
            if (pipelineType.equals(PipelineType.APPSYSTEM.getValue())) {
                Set<String> actionSet = DeployAppAuthChecker.builder(scheduleVo.getAppSystemId())
                        .addOperationAction(DeployAppConfigAction.PIPELINE.getValue())
                        .check();
                if (actionSet.contains(DeployAppConfigAction.PIPELINE.getValue())) {
                    scheduleVo.setEditable(1);
                    scheduleVo.setDeletable(1);
                } else {
                    List<Long> pipelineIdList = new ArrayList<>();
                    pipelineIdList.add(scheduleVo.getPipelineId());
                    pipelineIdList = pipelineMapper.checkHasAuthPipelineIdList(pipelineIdList, userUuid);
                    if (CollectionUtils.isNotEmpty(pipelineIdList)) {
                        scheduleVo.setEditable(1);
                        scheduleVo.setDeletable(1);
                    }
                }
            } else if (pipelineType.equals(PipelineType.GLOBAL.getValue())) {
                boolean hasPipelineModify = AuthActionChecker.check(PIPELINE_MODIFY.class);
                if (hasPipelineModify) {
                    scheduleVo.setEditable(1);
                    scheduleVo.setDeletable(1);
                } else {
                    List<Long> pipelineIdList = new ArrayList<>();
                    pipelineIdList.add(scheduleVo.getPipelineId());
                    pipelineIdList = pipelineMapper.checkHasAuthPipelineIdList(pipelineIdList, userUuid);
                    if (CollectionUtils.isNotEmpty(pipelineIdList)) {
                        scheduleVo.setEditable(1);
                        scheduleVo.setDeletable(1);
                    }
                }
            }
        }
        return scheduleVo;
    }
}