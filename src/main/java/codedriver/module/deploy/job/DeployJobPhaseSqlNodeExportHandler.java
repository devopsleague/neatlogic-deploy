/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.job;

import codedriver.framework.autoexec.dao.mapper.AutoexecJobMapper;
import codedriver.framework.autoexec.dto.INodeDetail;
import codedriver.framework.autoexec.dto.ISqlNodeDetail;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseNodeVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobPhaseVo;
import codedriver.framework.autoexec.dto.job.AutoexecJobVo;
import codedriver.framework.autoexec.job.AutoexecJobPhaseNodeExportHandlerBase;
import codedriver.framework.deploy.constvalue.NodeType;
import codedriver.framework.deploy.dto.sql.DeploySqlNodeDetailVo;
import codedriver.framework.util.TimeUtil;
import codedriver.module.deploy.dao.mapper.DeploySqlMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeployJobPhaseSqlNodeExportHandler extends AutoexecJobPhaseNodeExportHandlerBase {

    @Resource
    DeploySqlMapper deploySqlMapper;

    @Resource
    AutoexecJobMapper autoexecJobMapper;

    @Override
    public String getName() {
        return NodeType.DEPLOY_SQL_NODE.getValue();
    }

    @Override
    protected int getJobPhaseNodeCount(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        jobPhaseNodeVo.setJobPhaseName(autoexecJobMapper.getJobPhaseByPhaseId(jobPhaseNodeVo.getJobPhaseId()).getName());
        return deploySqlMapper.searchDeploySqlCount(jobPhaseNodeVo);
    }

    @Override
    protected List<ISqlNodeDetail> searchJobPhaseNode(AutoexecJobPhaseNodeVo jobPhaseNodeVo) {
        List<ISqlNodeDetail> result = new ArrayList<>();
        List<DeploySqlNodeDetailVo> list = deploySqlMapper.searchDeploySql(jobPhaseNodeVo);
        if (list.size() > 0) {
            list.forEach(o -> result.add(o));
        }
        deploySqlMapper.searchDeploySql(jobPhaseNodeVo);
        return result;
    }

    @Override
    protected void assembleData(AutoexecJobVo jobVo, AutoexecJobPhaseVo phaseVo, List<? extends INodeDetail> list, Map<Long, Map<String, Object>> nodeDataMap, Map<String, List<Long>> runnerNodeMap, Map<Long, JSONObject> nodeLogTailParamMap) {
        for (INodeDetail vo : list) {
            ISqlNodeDetail detail = (ISqlNodeDetail) vo;
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("name", detail.getSqlFile());
            dataMap.put("host", detail.getHost() + (detail.getPort() != null ? ":" + detail.getPort() : ""));
            dataMap.put("nodeName", detail.getNodeName());
            dataMap.put("statusName", detail.getStatusName());
            dataMap.put("costTime", detail.getCostTime());
            dataMap.put("startTime", detail.getStartTime() != null ? TimeUtil.convertDateToString(detail.getStartTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
            dataMap.put("endTime", detail.getEndTime() != null ? TimeUtil.convertDateToString(detail.getEndTime(), TimeUtil.YYYY_MM_DD_HH_MM_SS) : "");
            nodeDataMap.put(detail.getId(), dataMap);
            runnerNodeMap.computeIfAbsent(detail.getRunnerUrl(), k -> new ArrayList<>()).add(detail.getId());
            nodeLogTailParamMap.put(detail.getId(), new JSONObject() {
                {
                    this.put("id", detail.getId());
                    this.put("jobId", jobVo.getId());
                    this.put("resourceId", detail.getResourceId());
                    this.put("sqlName", detail.getSqlFile());
                    this.put("phase", phaseVo.getName());
                    this.put("ip", detail.getHost());
                    this.put("port", detail.getPort());
                    this.put("execMode", phaseVo.getExecMode());
                }
            });
        }
    }
}
