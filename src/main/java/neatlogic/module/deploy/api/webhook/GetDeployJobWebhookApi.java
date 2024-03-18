/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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
package neatlogic.module.deploy.api.webhook;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.framework.deploy.dto.webhook.DeployJobWebhookVo;
import neatlogic.framework.deploy.exception.webhook.DeployWebhookNotFoundException;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.dao.mapper.DeployJobWebhookMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author longrf
 * @date 2022/9/27 10:40
 */

@Service
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDeployJobWebhookApi extends PrivateApiComponentBase {

    @Resource
    DeployJobWebhookMapper webhookMapper;

    @Override
    public String getName() {
        return "获取触发器配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/job/webhook/get";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "触发器id")
    })
    @Output({
            @Param(explode = DeployScheduleVo.class, desc = "触发器详情")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        DeployJobWebhookVo webhookVo = webhookMapper.getWebhookById(paramObj.getLong("id"));
        if(webhookVo == null){
            throw new DeployWebhookNotFoundException(paramObj.getLong("id"));
        }
        return webhookVo;
    }
}
