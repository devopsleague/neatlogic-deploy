/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.apppipeline;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.crossover.IAutoexecServiceCrossoverService;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import codedriver.module.deploy.service.DeployAppPipelineService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppPipelineDraftApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppPipelineService deployAppPipelineService;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "获取应用流水线草稿";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/draft/get";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID")
    })
    @Output({
            @Param(name = "Return", explode = DeployAppConfigVo.class, desc = "应用流水线草稿")
    })
    @Description(desc = "获取应用流水线草稿")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo searchVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        DeployAppConfigVo deployAppConfigDraftVo = deployAppConfigMapper.getAppConfigDraft(searchVo);
        if (deployAppConfigDraftVo == null) {
            return null;
        }
        String overrideConfigStr = deployAppConfigDraftVo.getConfigStr();
        if (StringUtils.isBlank(overrideConfigStr)) {
            return null;
        }
        String targetLevel = null;
        DeployPipelineConfigVo appConfig = null;
        DeployPipelineConfigVo moduleOverrideConfig = null;
        DeployPipelineConfigVo envOverrideConfig = null;
        Long appSystemId = searchVo.getAppSystemId();
        Long moduleId = searchVo.getAppModuleId();
        Long envId = searchVo.getEnvId();
        if (moduleId == 0L && envId == 0L) {
            targetLevel = "应用";
            //查询应用层流水线配置信息
            appConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
        } else if (moduleId == 0L && envId != 0L) {
            // 如果是访问环境层配置信息，moduleId不能为空
            throw new ParamNotExistsException("moduleId");
        } else if (moduleId != 0L && envId == 0L) {
            targetLevel = "模块";
            //查询应用层配置信息
            String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
            if (StringUtils.isBlank(configStr)) {
                configStr = "{}";
            }
            appConfig = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
            moduleOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
        } else {
            targetLevel = "环境";
            //查询应用层配置信息
            String configStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId));
            if (StringUtils.isBlank(configStr)) {
                configStr = "{}";
            }
            appConfig = JSONObject.parseObject(configStr, DeployPipelineConfigVo.class);
            String moduleOverrideConfigStr = deployAppConfigMapper.getAppConfig(new DeployAppConfigVo(appSystemId, moduleId));
            if (StringUtils.isNotBlank(moduleOverrideConfigStr)) {
                moduleOverrideConfig = JSONObject.parseObject(moduleOverrideConfigStr, DeployPipelineConfigVo.class);
            }
            envOverrideConfig = JSONObject.parseObject(overrideConfigStr, DeployPipelineConfigVo.class);
        }
        DeployPipelineConfigVo deployPipelineConfigVo = deployAppPipelineService.mergeDeployPipelineConfigVo(appConfig, moduleOverrideConfig, envOverrideConfig, targetLevel);
        IAutoexecServiceCrossoverService autoexecServiceCrossoverService = CrossoverServiceFactory.getApi(IAutoexecServiceCrossoverService.class);
        autoexecServiceCrossoverService.updateAutoexecCombopConfig(deployPipelineConfigVo.getAutoexecCombopConfigVo());
        deployAppConfigDraftVo.setConfig(deployPipelineConfigVo);
        return deployAppConfigDraftVo;
    }
}