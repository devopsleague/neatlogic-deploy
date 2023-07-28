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
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListDeployAppPipelineProfileApi extends PrivateApiComponentBase {

    @Override
    public String getName() {
        return "获取应用流水线预置参数集列表";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/profile/List";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值")
    })
    @Output({
            @Param(name = "Return", explode = DeployProfileVo[].class, desc = "应用流水线预置参数集列表")
    })
    @Description(desc = "获取应用流水线预置参数集列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo searchVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
        ResourceVo appSystem = resourceCrossoverMapper.getAppSystemById(searchVo.getAppSystemId());
        if (appSystem == null) {
            throw new AppSystemNotFoundException(searchVo.getAppSystemId());
        }
        searchVo.setAppSystemName(appSystem.getName());
        searchVo.setAppSystemAbbrName(appSystem.getAbbrName());
        Long appModuleId = searchVo.getAppModuleId();
        if (appModuleId != null && appModuleId != 0) {
            ResourceVo appModule = resourceCrossoverMapper.getAppModuleById(appModuleId);
            if (appModule == null) {
                throw new AppModuleNotFoundException(appModuleId);
            }
            searchVo.setAppModuleName(appModule.getName());
            searchVo.setAppModuleAbbrName(appModule.getAbbrName());
        }
        Long envId = searchVo.getEnvId();
        if (envId != null && envId != 0) {
            ResourceVo env = resourceCrossoverMapper.getAppEnvById(envId);
            if (env == null) {
                throw new AppEnvNotFoundException(envId);
            }
            searchVo.setEnvName(env.getName());
        }
        List<Long> profileIdList = new ArrayList<>();
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            profileIdList = defaultValue.toJavaList(Long.class);
        }
        DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(searchVo.getAppSystemId())
                .withAppModuleId(searchVo.getAppModuleId())
                .withEnvId(searchVo.getEnvId())
                .withProfileIdList(profileIdList)
                .getConfig();
        return deployPipelineConfigVo.getOverrideProfileList();
    }
}