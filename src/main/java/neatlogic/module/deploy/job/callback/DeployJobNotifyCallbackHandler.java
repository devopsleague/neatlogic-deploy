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
package neatlogic.module.deploy.job.callback;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.autoexec.constvalue.JobUserType;
import neatlogic.framework.autoexec.dao.mapper.AutoexecJobMapper;
import neatlogic.framework.autoexec.dto.job.AutoexecJobVo;
import neatlogic.framework.autoexec.job.callback.core.AutoexecJobCallbackBase;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.common.constvalue.GroupSearch;
import neatlogic.framework.common.constvalue.SystemUser;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.constvalue.DeployJobNotifyTriggerType;
import neatlogic.framework.deploy.dto.job.DeployJobVo;
import neatlogic.framework.notify.crossover.INotifyServiceCrossoverService;
import neatlogic.framework.notify.dao.mapper.NotifyMapper;
import neatlogic.framework.notify.dto.InvokeNotifyPolicyConfigVo;
import neatlogic.framework.notify.dto.NotifyPolicyVo;
import neatlogic.framework.notify.dto.NotifyReceiverVo;
import neatlogic.framework.transaction.util.TransactionUtil;
import neatlogic.framework.util.NotifyPolicyUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployJobMapper;
import neatlogic.module.deploy.handler.DeployJobMessageHandler;
import neatlogic.module.deploy.notify.handler.DeployJobNotifyPolicyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author longrf
 * @date 2022/12/29 15:32
 */

@Component
public class DeployJobNotifyCallbackHandler extends AutoexecJobCallbackBase {

    private final static Logger logger = LoggerFactory.getLogger(DeployJobNotifyCallbackHandler.class);
    @Resource
    private AutoexecJobMapper autoexecJobMapper;

    @Resource
    private DeployJobMapper deployJobMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private NotifyMapper notifyMapper;

    @Override
    public String getHandler() {
        return DeployJobNotifyCallbackHandler.class.getSimpleName();
    }

    @Override
    public Boolean getIsNeedCallback(AutoexecJobVo jobVo) {
        DeployJobNotifyTriggerType trigger = DeployJobNotifyTriggerType.getTriggerByStatus(jobVo.getStatus());
        if (trigger != null) {
            AutoexecJobVo jobInfo;
            // 开启一个新事务来查询父事务提交前的作业状态，如果新事务查出来的状态与当前jobVo的状态不同，则表示该状态未通知过
            TransactionStatus tx = TransactionUtil.openNewTx();
            try {
                jobInfo = autoexecJobMapper.getJobInfo(jobVo.getId());
            } finally {
                if (tx != null) {
                    TransactionUtil.commitTx(tx);
                }
            }
            if (jobInfo != null && !Objects.equals(jobVo.getStatus(), jobInfo.getStatus())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doService(Long invokeId, AutoexecJobVo jobVo) {
        DeployJobNotifyTriggerType trigger = DeployJobNotifyTriggerType.getTriggerByStatus(jobVo.getStatus());
        if (trigger == null) {
            return;
        }
        DeployJobVo jobInfo = deployJobMapper.getDeployJobInfoByJobId(jobVo.getId());
        if (jobInfo == null) {
            return;
        }
        Long appSystemId = jobInfo.getAppSystemId();
        if (appSystemId == null) {
            return;
        }
        IAppSystemMapper iAppSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        AppSystemVo appSystemVo = iAppSystemMapper.getAppSystemById(appSystemId);
        if (appSystemVo != null) {
            jobInfo.setAppSystemName(appSystemVo.getName());
            jobInfo.setAppSystemAbbrName(appSystemVo.getAbbrName());
        }
        Long appModuleId = jobInfo.getAppModuleId();
        if (appModuleId != null) {
            AppModuleVo appModuleVo = iAppSystemMapper.getAppModuleById(appModuleId);
            if (appModuleVo != null) {
                jobInfo.setAppModuleName(appModuleVo.getName());
                jobInfo.setAppModuleAbbrName(appModuleVo.getAbbrName());
            }
        }
        String configStr = deployAppConfigMapper.getAppSystemNotifyPolicyConfigByAppSystemId(appSystemId);
        InvokeNotifyPolicyConfigVo invokeNotifyPolicyConfigVo = JSONObject.parseObject(configStr, InvokeNotifyPolicyConfigVo.class);
        INotifyServiceCrossoverService notifyServiceCrossoverService = CrossoverServiceFactory.getApi(INotifyServiceCrossoverService.class);
        invokeNotifyPolicyConfigVo = notifyServiceCrossoverService.regulateNotifyPolicyConfig(invokeNotifyPolicyConfigVo, DeployJobNotifyPolicyHandler.class);
        if (invokeNotifyPolicyConfigVo == null) {
            return;
        }
        // 触发点被排除，不用发送邮件
        List<String> excludeTriggerList = invokeNotifyPolicyConfigVo.getExcludeTriggerList();
        if (CollectionUtils.isNotEmpty(excludeTriggerList) && excludeTriggerList.contains(trigger.getTrigger())) {
            return;
        }
        Long notifyPolicyId = invokeNotifyPolicyConfigVo.getPolicyId();
        if (notifyPolicyId == null) {
            return;
        }
        NotifyPolicyVo notifyPolicyVo = notifyMapper.getNotifyPolicyById(notifyPolicyId);
        if (notifyPolicyVo == null || notifyPolicyVo.getConfig() == null) {
            return;
        }
        try {
            Map<String, List<NotifyReceiverVo>> receiverMap = new HashMap<>();
            if (!Objects.equals(jobInfo.getExecUser(), SystemUser.SYSTEM.getUserUuid())) {
                receiverMap.computeIfAbsent(JobUserType.EXEC_USER.getValue(), k -> new ArrayList<>())
                        .add(new NotifyReceiverVo(GroupSearch.USER.getValue(), jobInfo.getExecUser()));
            }
            String notifyAuditMessage = jobInfo.getId() + "-" + jobInfo.getName();
            NotifyPolicyUtil.execute(notifyPolicyVo.getHandler(), trigger, DeployJobMessageHandler.class
                    , notifyPolicyVo, null, null, receiverMap
                    , jobInfo, null, notifyAuditMessage);
        } catch (Exception ex) {
            logger.error("发布作业：" + jobInfo.getId() + "-" + jobInfo.getName() + "通知失败");
            logger.error(ex.getMessage(), ex);
        }
    }
}
