/*Copyright (C) 2023  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.deploy.api.appconfig.env;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.dto.app.DeployAppEnvAutoConfigVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.service.DeployAppAuthorityService;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * @author lvzk
 * @since 2022/5/26 15:04
 **/
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SaveDeployAppConfigEnvAutoConfigApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    DeployAppAuthorityService deployAppAuthorityService;

    @Override
    public String getToken() {
        return "deploy/app/config/env/auto/config/save";
    }

    @Override
    public String getName() {
        return "保存应用环境实例autoConfig";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "appSystemId", type = ApiParamType.LONG, isRequired = true, desc = "应用 id"),
            @Param(name = "appModuleId", type = ApiParamType.LONG, isRequired = true, desc = "模块 id"),
            @Param(name = "envId", type = ApiParamType.LONG, isRequired = true, desc = "环境 id"),
            @Param(name = "deleteInstanceId", type = ApiParamType.LONG, desc = "删除的应用实例 id"),
            @Param(name = "instanceId", type = ApiParamType.LONG, desc = "应用实例 id"),
            @Param(name = "keyValueList", type = ApiParamType.JSONARRAY, desc = "[{\"id\": xxx,\"key\": xxx,\"value\":xxx}]"),
    })
    @Output({
    })
    @Description(desc = "保存应用环境实例autoConfig接口")
    @Override
    public Object myDoService(JSONObject paramObj) {

        //校验环境权限、编辑配置的操作权限
        deployAppAuthorityService.checkEnvAuth(paramObj.getLong("appSystemId"), paramObj.getLong("envId"));
        deployAppAuthorityService.checkOperationAuth(paramObj.getLong("appSystemId"), DeployAppConfigAction.EDIT);

        DeployAppEnvAutoConfigVo appEnvAutoConfigVo = JSON.toJavaObject(paramObj, DeployAppEnvAutoConfigVo.class);
        Date nowDate = new Date(System.currentTimeMillis());
        appEnvAutoConfigVo.setLcd(nowDate);
        if (CollectionUtils.isNotEmpty(appEnvAutoConfigVo.getKeyValueList())) {
            deployAppConfigMapper.insertAppEnvAutoConfig(appEnvAutoConfigVo);
        }
        deployAppConfigMapper.deleteAppEnvAutoConfig(appEnvAutoConfigVo);
        Long deleteInstanceId = paramObj.getLong("deleteInstanceId");
        if (deleteInstanceId != null) {
            DeployAppEnvAutoConfigVo deleteAppEnvAutoConfigVo = new DeployAppEnvAutoConfigVo(paramObj.getLong("appSystemId"), paramObj.getLong("appModuleId"), paramObj.getLong("envId"), deleteInstanceId);
            deployAppConfigMapper.deleteAppEnvAutoConfig(deleteAppEnvAutoConfigVo);
        }
        return null;
    }
}
