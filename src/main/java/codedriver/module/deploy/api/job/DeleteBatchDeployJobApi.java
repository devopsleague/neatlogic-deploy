/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.job;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.BATCHDEPLOY_MODIFY;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
@AuthAction(action = BATCHDEPLOY_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteBatchDeployJobApi extends PrivateApiComponentBase {
    @Resource
    private DeployJobMapper deployJobMapper;


    @Override
    public String getName() {
        return "删除批量发布作业";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "/deploy/batchjob/delete";
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "作业id", isRequired = true)})
    @Output({@Param(explode = DeployJobVo.class)})
    @Description(desc = "删除批量发布作业接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        DeployJobVo deployJobVo = JSONObject.toJavaObject(jsonObj, DeployJobVo.class);
        deployJobVo.setReviewer(UserContext.get().getUserUuid(true));
        deployJobMapper.updateDeployJobReviewStatusById(deployJobVo);
        return deployJobMapper.getBatchDeployJobById(deployJobVo.getId());
    }

}
