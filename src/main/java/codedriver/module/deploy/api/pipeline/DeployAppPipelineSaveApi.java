/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipeline;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.app.DeployAppConfigVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

@Service
@OperationType(type = OperationTypeEnum.OPERATE)
public class DeployAppPipelineSaveApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存应用流水线";
    }

    @Override
    public String getToken() {
        return "deploy/app/pipeline/save";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用系统ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "config", type = ApiParamType.JSONOBJECT, isRequired = true, desc = "流水线配置信息")
    })
    @Description(desc = "保存应用流水线")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployAppConfigVo deployAppConfigVo = paramObj.toJavaObject(DeployAppConfigVo.class);
        String configStr = deployAppConfigMapper.getAppConfig(deployAppConfigVo);
        if (configStr != null) {
            if (Objects.equals(configStr, deployAppConfigVo.getConfigStr())) {
                return null;
            }
            deployAppConfigVo.setLcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
            deployAppConfigMapper.deleteAppConfigDraft(deployAppConfigVo);
        } else {
            deployAppConfigVo.setFcu(UserContext.get().getUserUuid());
            deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
        }
        return null;
    }
}
