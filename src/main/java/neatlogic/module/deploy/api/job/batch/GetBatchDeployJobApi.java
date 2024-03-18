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

package neatlogic.module.deploy.api.job.batch;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.deploy.auth.DEPLOY_BASE;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.deploy.exception.job.DeployBatchJobNotFoundEditTargetException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.deploy.auth.core.BatchDeployAuthChecker;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author lvzk
 * @since 2022/6/29 11:20
 **/

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
@AuthAction(action = DEPLOY_BASE.class)
public class GetBatchDeployJobApi extends PrivateApiComponentBase {

    @Resource
    DeployJobMapper deployJobMapper;

    @Override
    public String getName() {
        return "nmdajb.getbatchdeployjobapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "common.id")})
    @Output({@Param(explode = DeployJobVo.class)})
    @Description(desc = "nmdajb.getbatchdeployjobapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        DeployJobVo deployJobVo = deployJobMapper.getBatchDeployJobById(id);
        if (deployJobVo == null) {
            throw new DeployBatchJobNotFoundEditTargetException(id);
        }
        deployJobVo.setIsCanExecute(BatchDeployAuthChecker.isCanExecute(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanTakeOver(BatchDeployAuthChecker.isCanTakeOver(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanEdit(BatchDeployAuthChecker.isCanEdit(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanCheck(BatchDeployAuthChecker.isCanCheck(deployJobVo) ? 1 : 0);
        deployJobVo.setIsCanGroupExecute(BatchDeployAuthChecker.isCanGroupExecute(deployJobVo) ? 1 : 0);
        return deployJobVo;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/get";
    }
}
