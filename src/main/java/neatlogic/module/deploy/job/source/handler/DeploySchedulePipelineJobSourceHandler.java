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

import neatlogic.framework.autoexec.source.IAutoexecJobSource;
import neatlogic.framework.common.dto.ValueTextVo;
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
    public List<ValueTextVo> getListByIdList(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)) {
            return null;
        }
        List<ValueTextVo> resultList = new ArrayList<>();
        List<DeployScheduleVo> list = deployScheduleMapper.getScheduleListByIdList(idList);
        for (DeployScheduleVo serviceVo : list) {
            resultList.add(new ValueTextVo(serviceVo.getId(), serviceVo.getName()));
        }
        return resultList;
    }
}
