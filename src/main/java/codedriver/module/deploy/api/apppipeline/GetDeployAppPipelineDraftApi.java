/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.apppipeline;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.crossover.IResourceCrossoverMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.resourcecenter.AppEnvNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppModuleNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.AppSystemNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.crossover.CrossoverServiceFactory;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployAppPipelineDraftApi extends PrivateApiComponentBase {

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
        String schemaName = TenantContext.get().getDataDbName();
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(searchVo.getAppSystemId(), schemaName);
        if (appSystem == null) {
            throw new AppSystemNotFoundException(searchVo.getAppSystemId());
        }
        searchVo.setAppSystemName(appSystem.getAbbrName());
        boolean isAppSystemDraft = true;
        boolean isAppModuleDraft = false;
        boolean isEnvDraft = false;
        Long appModuleId = searchVo.getAppModuleId();
        if (appModuleId != null && appModuleId != 0) {
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId, schemaName);
            if (appModule == null) {
                throw new AppModuleNotFoundException(appModuleId);
            }
            searchVo.setAppModuleName(appModule.getAbbrName());
            isAppSystemDraft = false;
            isAppModuleDraft = true;
        }
        Long envId = searchVo.getEnvId();
        if (envId != null && envId != 0) {
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(envId, schemaName);
            if (env == null) {
                throw new AppEnvNotFoundException(envId);
            }
            searchVo.setEnvName(env.getName());
            isAppSystemDraft = false;
            isAppModuleDraft = false;
            isEnvDraft = true;
        }
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(searchVo.getAppSystemId())
                .withAppModuleId(searchVo.getAppModuleId())
                .withEnvId(searchVo.getEnvId())
                .withAppSystemDraft(isAppSystemDraft)
                .withAppModuleDraft(isAppModuleDraft)
                .withEnvDraft(isEnvDraft)
                .getConfig();
        searchVo.setConfig(deployPipelineConfigVo);
        return searchVo;
    }
}
