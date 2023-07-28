/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */
package codedriver.module.deploy.api.appconfig.env;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_BASE;
import codedriver.framework.deploy.dto.app.DeployAppConfigEnvDBConfigVo;
import codedriver.framework.deploy.exception.DeployAppConfigDBSchemaActionIrregularException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.RegexUtils;
import codedriver.module.deploy.dao.mapper.DeployAppConfigMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/8/22 16:38
 */
@Service
@Transactional
@AuthAction(action = DEPLOY_BASE.class)
@OperationType(type = OperationTypeEnum.OPERATE)
public class SaveDeployAppConfigDbSchemaForAutoexecApi extends PrivateApiComponentBase {

    @Resource
    DeployAppConfigMapper deployAppConfigMapper;

    @Override
    public String getName() {
        return "保存某个环境的DBConfig配置的schema";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/app/config/env/db/config/schemas/save/forautoexec";
    }

    @Input({
            @Param(name = "runnerId", type = ApiParamType.LONG, desc = "Runner的ID"),
            @Param(name = "runnerGroup", type = ApiParamType.JSONOBJECT, desc = "runner组信息"),
            @Param(name = "jobId", type = ApiParamType.LONG, desc = "作业ID"),
            @Param(name = "phaseName", type = ApiParamType.STRING, desc = "阶段名"),
            @Param(name = "sysId", type = ApiParamType.LONG, desc = "应用ID"),
            @Param(name = "moduleId", type = ApiParamType.LONG, desc = "模块ID"),
            @Param(name = "envId", type = ApiParamType.LONG, desc = "环境ID"),
            @Param(name = "sysName", type = ApiParamType.STRING, desc = "应用名"),
            @Param(name = "moduleName", type = ApiParamType.STRING, desc = "模块名"),
            @Param(name = "envName", type = ApiParamType.STRING, desc = "环境名"),
            @Param(name = "dbSchemas", type = ApiParamType.JSONARRAY, desc = "数据库schema列表（schema格式为：dbname.username）"),
    })
    @Output({
    })
    @Description(desc = "发布作业专用-保存某个环境的DBConfig配置的schema")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray dbSchemaArray = paramObj.getJSONArray("dbSchemas");
        if (CollectionUtils.isEmpty(dbSchemaArray)) {
            return null;
        }
        List<String> dbSchemaList = dbSchemaArray.toJavaList(String.class);
        List<String> dbSchemaIrregularList = new ArrayList<>();
        List<DeployAppConfigEnvDBConfigVo> insertDbConfigVoList = new ArrayList<>();
        for (String dbSchema : dbSchemaList) {
            if (!RegexUtils.isMatch(dbSchema, RegexUtils.DB_SCHEMA)) {
                dbSchemaIrregularList.add(dbSchema);
            } else {
                insertDbConfigVoList.add(new DeployAppConfigEnvDBConfigVo(paramObj.getLong("sysId"), paramObj.getLong("moduleId"), paramObj.getLong("envId"), dbSchema));
            }
        }
        if (CollectionUtils.isNotEmpty(dbSchemaIrregularList)) {
            throw new DeployAppConfigDBSchemaActionIrregularException(dbSchemaIrregularList);
        }

        deployAppConfigMapper.insertBatchAppConfigEnvDBConfig(insertDbConfigVoList);
        return null;
    }
}