/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.appconfig.system;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigAuthorityVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/5/25 15:04
 **/
@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class DeployAppConfigAuthorityDeleteApi extends PrivateApiComponentBase {
    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/authority/delete";
    }

    @Override
    public String getName() {
        return "删除应用配置权限";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用资产id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境资产id"),
            @Param(name = "authUuid", type = ApiParamType.STRING, isRequired = true, desc = "授权列表")
    })
    @Output({
    })
    @Description(desc = "删除应用配置权限")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployAppConfigAuthorityVo deployAppConfigAuthorityVo = paramObj.toJavaObject(DeployAppConfigAuthorityVo.class);
        deployAppConfigMapper.deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(deployAppConfigAuthorityVo.getAppSystemId(),deployAppConfigAuthorityVo.getEnvId(),deployAppConfigAuthorityVo.getAuthUuid(),null);
        return null;
    }
}