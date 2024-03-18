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

package neatlogic.module.deploy.job.source.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.dto.job.AutoexecJobRouteVo;
import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.deploy.constvalue.JobSource;
import neatlogic.framework.deploy.dto.schedule.DeployScheduleVo;
import neatlogic.module.deploy.dao.mapper.DeployScheduleMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeploySchedulePipelineJobSourceHandler implements IAutoexecJobSource {

    @Resource
    private DeployScheduleMapper deployScheduleMapper;

    @Override
    public String getValue() {
        return JobSource.DEPLOY_SCHEDULE_PIPELINE.getValue();
    }

    @Override
    public String getText() {
        return JobSource.DEPLOY_SCHEDULE_PIPELINE.getText();
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
        List<DeployScheduleVo> list = deployScheduleMapper.getScheduleListByIdList(idList);
        for (DeployScheduleVo deployScheduleVo : list) {
            JSONObject config = new JSONObject();
            config.put("id", deployScheduleVo.getId());
            resultList.add(new AutoexecJobRouteVo(deployScheduleVo.getId(), deployScheduleVo.getName(), config));
        }
        return resultList;
    }
}
