/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.deploy.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.ci.DeployCiVo;
import neatlogic.module.deploy.dao.mapper.DeployCiMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeployCiJobSourceHandler implements IAutoexecJobSource {

    @Resource
    DeployCiMapper deployCiMapper;

    @Override
    public String getValue() {
        return JobSource.DEPLOY_CI.getValue();
    }

    @Override
    public String getText() {
        return JobSource.DEPLOY_CI.getText();
    }

    @Override
    public List<AutoexecJobRouteVo> getListByUniqueKeyList(List<String> uniqueKeyList) {
        if (CollectionUtils.isEmpty(uniqueKeyList)) {
            return null;
        }
        List<Long> idList = new ArrayList<>();
        for (String str : uniqueKeyList) {
            idList.add(Long.valueOf(str));
        }
        List<AutoexecJobRouteVo> resultList = new ArrayList<>();
        List<DeployCiVo> list = deployCiMapper.getDeployCiListByIdList(idList);
        for (DeployCiVo ciVo : list) {
            JSONObject config = new JSONObject();
            config.put("id", ciVo.getId());
            config.put("appSystemId", ciVo.getAppSystemId());
            config.put("appModuleId", ciVo.getAppModuleId());
            resultList.add(new AutoexecJobRouteVo(ciVo.getId(), ciVo.getName(), config));
        }
        return resultList;
    }
}
