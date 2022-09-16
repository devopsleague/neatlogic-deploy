package codedriver.module.deploy.api.ci;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.autoexec.constvalue.JobStatus;
import codedriver.framework.autoexec.constvalue.JobTriggerType;
import codedriver.framework.autoexec.constvalue.ReviewStatus;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopScenarioVo;
import codedriver.framework.autoexec.dto.node.AutoexecNodeVo;
import codedriver.framework.autoexec.exception.AutoexecScenarioIsNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.constvalue.SystemUser;
import codedriver.framework.deploy.constvalue.DeployCiActionType;
import codedriver.framework.deploy.constvalue.DeployCiTriggerType;
import codedriver.framework.deploy.constvalue.JobSource;
import codedriver.framework.deploy.dto.app.DeployPipelineConfigVo;
import codedriver.framework.deploy.dto.ci.DeployCiVo;
import codedriver.framework.deploy.dto.job.DeployJobModuleVo;
import codedriver.framework.deploy.dto.job.DeployJobVo;
import codedriver.framework.deploy.dto.pipeline.PipelineGroupVo;
import codedriver.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import codedriver.framework.deploy.dto.pipeline.PipelineLaneVo;
import codedriver.framework.deploy.dto.pipeline.PipelineVo;
import codedriver.framework.deploy.dto.version.DeploySystemModuleVersionVo;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.*;
import codedriver.framework.filter.core.LoginAuthHandlerBase;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.ApiAnonymousAccessSupportEnum;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.scheduler.core.IJob;
import codedriver.framework.scheduler.core.SchedulerManager;
import codedriver.framework.scheduler.dto.JobObject;
import codedriver.framework.scheduler.exception.ScheduleHandlerNotFoundException;
import codedriver.framework.util.TimeUtil;
import codedriver.module.deploy.dao.mapper.DeployCiMapper;
import codedriver.module.deploy.dao.mapper.DeployJobMapper;
import codedriver.module.deploy.dao.mapper.DeployPipelineMapper;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.schedule.plugin.DeployBatchJobAutoFireJob;
import codedriver.module.deploy.service.DeployBatchJobService;
import codedriver.module.deploy.service.DeployJobService;
import codedriver.module.deploy.util.DeployPipelineConfigManager;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
@OperationType(type = OperationTypeEnum.OPERATE)
public class CallbackDeployCiGitlabEventApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(CallbackDeployCiGitlabEventApi.class);

    @Resource
    DeployCiMapper deployCiMapper;

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployPipelineMapper deployPipelineMapper;

    @Resource
    DeployJobMapper deployJobMapper;

    @Resource
    DeployJobService deployJobService;

    @Resource
    DeployBatchJobService deployBatchJobService;

    @Override
    public String getName() {
        return "gitlab webhook回调api";
    }

    @Override
    public String getToken() {
        return "deploy/ci/gitlab/event/callback";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public ApiAnonymousAccessSupportEnum supportAnonymousAccess() {
        return ApiAnonymousAccessSupportEnum.ANONYMOUS_ACCESS_WITHOUT_ENCRYPTION;
    }

    @Input({
            @Param(name = "ciId", desc = "持续集成配置id", isRequired = true, type = ApiParamType.LONG),
    })
    @Description(desc = "gitlab webhook回调api")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        /*
            1、查出ciId对应的配置
            2、获取参数中的commits和ref，确定commitId、分支
            3、根据版本号规则拼接版本号
            4、根据场景决定是否需要生成版本号和新建builNo
            5、根据触发方式决定作业如何执行
            6、记录audit
         */
        Long ciId = paramObj.getLong("ciId");
        JSONArray commits = paramObj.getJSONArray("commits");
        String branchName = paramObj.getString("ref");
        String commitId = StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(commits)) {
            commitId = commits.getJSONObject(0).getString("id");
        }
        if (StringUtils.isNotBlank(branchName)) {
            branchName = branchName.substring(branchName.lastIndexOf('/') + 1);
        }
        DeployCiVo ci = deployCiMapper.getDeployCiById(ciId);
        if (ci == null) {
            logger.error("Gitlab callback error. Deploy ci not found, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
            throw new DeployCiNotFoundException(ciId);
        }
        if (!Objects.equals(ci.getIsActive(), 1)) {
            logger.info("Gitlab callback stop. Deploy ci is not active, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
            return null;
        }
        String triggerType = ci.getTriggerType();
        if (StringUtils.isBlank(triggerType)) {
            logger.error("Gitlab callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
            throw new DeployCiTriggerTypeLostException();
        }
        String triggerTimeStr = ci.getTriggerTime();
        if (!DeployCiTriggerType.INSTANT.getValue().equals(ci.getTriggerType()) && StringUtils.isBlank(triggerTimeStr)) {
            logger.error("Gitlab callback error. Missing triggerTime in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
            throw new DeployCiTriggerTimeLostException();
        }
        JSONObject versionRule = ci.getVersionRule();
        String versionPrefix = versionRule.getString("versionPrefix");
        String versionRegex = versionRule.getString("versionRegex");
        int useCommitId = versionRule.getInteger("useCommitId") != null ? versionRule.getInteger("useCommitId") : 0;
        String versionName = getVersionName(branchName, versionRegex, versionPrefix, commitId, useCommitId);
        DeployVersionVo deployVersion = deployVersionMapper.getDeployVersionBaseInfoBySystemIdAndModuleIdAndVersion(new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId()));
        UserContext.init(SystemUser.SYSTEM.getUserVo(), SystemUser.SYSTEM.getTimezone());
        UserContext.get().setToken("GZIP_" + LoginAuthHandlerBase.buildJwt(SystemUser.SYSTEM.getUserVo()).getCc());
        // 普通作业
        if (DeployCiActionType.CREATE_JOB.getValue().equals(ci.getAction())) {
            /*
              只要场景包含编译，那么新建buildNo和新建版本（如果版本不存在）
              如果场景只包含部署，那么新建版本（如果版本不存在）
              如果场景不含编译和部署，那么buildNo和版本都不新建
            */
            Long scenarioId = ci.getConfig().getLong("scenarioId");
            if (scenarioId == null) {
                logger.error("Gitlab callback error. Missing scenarioId in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiScenarioIdLostException();
            }
            Long envId = ci.getConfig().getLong("envId");
            if (envId == null) {
                logger.error("Gitlab callback error. Missing envId in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiEnvIdLostException();
            }
            DeployPipelineConfigVo deployPipelineConfigVo = DeployPipelineConfigManager.init(ci.getAppSystemId())
                    .withAppModuleId(ci.getAppModuleId())
                    .withEnvId(envId)
                    .isHasBuildOrDeployTypeTool(true)
                    .getConfig();
            if (deployPipelineConfigVo == null) {
                logger.error("Gitlab callback error. Deploy app config not found, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployAppConfigNotFoundException(ci.getAppSystemId());
            }
            List<AutoexecCombopScenarioVo> scenarioList = deployPipelineConfigVo.getScenarioList();
            Optional<AutoexecCombopScenarioVo> scenarioOptional = scenarioList.stream().filter(o -> Objects.equals(o.getScenarioId(), scenarioId)).findFirst();
            if (!scenarioOptional.isPresent()) {
                logger.error("Gitlab callback error. Scenario not found, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new AutoexecScenarioIsNotFoundException(scenarioId);
            }
            AutoexecCombopScenarioVo scenarioVo = scenarioOptional.get();
            // 如果版本不存在且包含编译或部署工具
            if (deployVersion == null && (Objects.equals(scenarioVo.getIsHasBuildTypeTool(), 1) || Objects.equals(scenarioVo.getIsHasDeployTypeTool(), 1))) {
                deployVersion = new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId(), 0);
                deployVersionMapper.insertDeployVersion(deployVersion);
            }
            Date triggerTime = getTriggerTime(triggerTimeStr);
            if (DeployCiTriggerType.INSTANT.getValue().equals(ci.getTriggerType())) {
                triggerType = DeployCiTriggerType.AUTO.getValue();
            }
            DeployJobVo deployJobParam = new DeployJobVo(ci.getAppSystemId(), scenarioId, envId, triggerType, triggerTime, ci.getConfig().getInteger("roundCount"), ci.getConfig().getJSONObject("param"));
            JSONArray selectNodeList = ci.getConfig().getJSONArray("selectNodeList");
            DeployJobModuleVo moduleVo = new DeployJobModuleVo(ci.getAppModuleId(), deployVersion != null ? deployVersion.getVersion() : null, CollectionUtils.isNotEmpty(selectNodeList) ? selectNodeList.toJavaList(AutoexecNodeVo.class) : null);
            // 包含编译工具则新建buildNo
            if (Objects.equals(scenarioVo.getIsHasBuildTypeTool(), 1)) {
                moduleVo.setBuildNo(-1);
            }
            deployJobParam.setModuleList(Collections.singletonList(moduleVo));
            if (!Objects.equals(ci.getTriggerType(), DeployCiTriggerType.INSTANT.getValue())) {
                deployJobService.createScheduleJob(deployJobParam, moduleVo);
            } else {
                deployJobService.createJobAndFire(deployJobParam, moduleVo);
            }
        } else if (DeployCiActionType.CREATE_BATCH_JOB.getValue().equals(ci.getAction())) {
            /*
               1、遍历当前流水线所有属于当前模块的子作业，检查其场景以决定是否要创建版本
               2、筛选出属于当前模块的子作业来执行
             */
            Long pipelineId = ci.getConfig().getLong("pipelineId");
            if (pipelineId == null) {
                logger.error("Gitlab callback error. Missing pipelineId in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiPipelineIdLostException();
            }
            boolean hasBuildTypeTool = false;
            PipelineVo pipeline = deployPipelineMapper.getPipelineBaseInfoByIdAndModuleId(pipelineId, ci.getAppModuleId());
            // 判断超级流水线中是否含有编译工具的作业模版
            Map<Long, DeployPipelineConfigVo> envPipelineMap = new HashMap<>();
            out:
            if (CollectionUtils.isNotEmpty(pipeline.getLaneList())) {
                for (int i = 0; i < pipeline.getLaneList().size(); i++) {
                    PipelineLaneVo pipelineLaneVo = pipeline.getLaneList().get(i);
                    if (CollectionUtils.isNotEmpty(pipelineLaneVo.getGroupList())) {
                        for (int j = 0; j < pipelineLaneVo.getGroupList().size(); j++) {
                            PipelineGroupVo pipelineGroupVo = pipelineLaneVo.getGroupList().get(j);
                            if (CollectionUtils.isNotEmpty(pipelineGroupVo.getJobTemplateList())) {
                                for (int k = 0; k < pipelineGroupVo.getJobTemplateList().size(); k++) {
                                    PipelineJobTemplateVo jobTemplateVo = pipelineGroupVo.getJobTemplateList().get(k);
                                    DeployPipelineConfigVo pipelineConfigVo = envPipelineMap.get(jobTemplateVo.getEnvId());
                                    if (pipelineConfigVo == null) {
                                        pipelineConfigVo = DeployPipelineConfigManager.init(ci.getAppSystemId())
                                                .withAppModuleId(ci.getAppModuleId())
                                                .withEnvId(jobTemplateVo.getEnvId())
                                                .isHasBuildOrDeployTypeTool(true)
                                                .getConfig();
                                        if (pipelineConfigVo != null) {
                                            envPipelineMap.put(jobTemplateVo.getEnvId(), pipelineConfigVo);
                                        }
                                    }
                                    if (pipelineConfigVo != null) {
                                        List<AutoexecCombopScenarioVo> scenarioList = pipelineConfigVo.getScenarioList();
                                        if (CollectionUtils.isNotEmpty(scenarioList)) {
                                            Optional<AutoexecCombopScenarioVo> first = scenarioList.stream().filter(o -> Objects.equals(o.getScenarioId(), jobTemplateVo.getScenarioId())).findFirst();
                                            if (first.isPresent()) {
                                                hasBuildTypeTool = Objects.equals(first.get().getIsHasBuildTypeTool(), 1);
                                                if (hasBuildTypeTool) {
                                                    break out;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (deployVersion == null && hasBuildTypeTool) {
                deployVersion = new DeployVersionVo(versionName, ci.getAppSystemId(), ci.getAppModuleId(), 0);
                deployVersionMapper.insertDeployVersion(deployVersion);
            }
            String jobName = ci.getConfig().getString("jobName");
            if (StringUtils.isBlank(jobName)) {
                logger.error("Gitlab callback error. Missing jobName in ci config, ciId: {}, callback params: {}", ciId, paramObj.toJSONString());
                throw new DeployCiJobNameLostException();
            }
            DeployJobVo deployJobVo = new DeployJobVo();
            deployJobVo.setPipelineId(pipelineId);
            deployJobVo.setName(jobName);
            Date triggerTime = getTriggerTime(triggerTimeStr);
            if (DeployCiTriggerType.MANUAL.getValue().equals(triggerType)) {
                deployJobVo.setStatus(JobStatus.PENDING.getValue());
                deployJobVo.setTriggerType(JobTriggerType.MANUAL.getValue());
            } else {
                deployJobVo.setStatus(JobStatus.READY.getValue());
                deployJobVo.setTriggerType(JobTriggerType.AUTO.getValue());
                deployJobVo.setPlanStartTime(triggerTime);
            }
            deployJobVo.setAppSystemModuleVersionList(Collections.singletonList(new DeploySystemModuleVersionVo(ci.getAppSystemId(), ci.getAppModuleId(), deployVersion != null ? deployVersion.getId() : null)));
            deployJobVo.setReviewStatus(ReviewStatus.PASSED.getValue());
            deployJobVo.setSource(JobSource.BATCHDEPLOY.getValue());
            deployJobVo.setExecUser(UserContext.get().getUserUuid());

            deployBatchJobService.creatBatchJob(deployJobVo, pipeline, false);
            deployJobMapper.insertJobInvoke(deployJobVo.getId(), pipelineId, JobSource.PIPELINE.getValue());

            //补充定时执行逻辑
            if (Objects.equals(deployJobVo.getTriggerType(), JobTriggerType.AUTO.getValue())) {
                IJob jobHandler = SchedulerManager.getHandler(DeployBatchJobAutoFireJob.class.getName());
                if (jobHandler == null) {
                    throw new ScheduleHandlerNotFoundException(DeployBatchJobAutoFireJob.class.getName());
                }
                JobObject.Builder jobObjectBuilder = new JobObject.Builder(deployJobVo.getId().toString(), jobHandler.getGroupName(), jobHandler.getClassName(), TenantContext.get().getTenantUuid());
                jobHandler.reloadJob(jobObjectBuilder.build());
            }
        }
        return null;
    }

    /**
     * 计算触发时间
     *
     * @param triggerTimeStr
     * @return
     */
    private Date getTriggerTime(String triggerTimeStr) {
        Date triggerTime = null;
        if (StringUtils.isNotBlank(triggerTimeStr)) {
            LocalTime triggerInstance = LocalTime.parse(triggerTimeStr, DateTimeFormatter.ofPattern(TimeUtil.HH_MM_SS));
            // 如果当前时间在前，则触发时间为当天；如果当前时间在后，则触发时间为第二日
            String day;
            if (LocalTime.now().isBefore(triggerInstance)) {
                day = LocalDate.now().format(DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD));
            } else {
                day = LocalDate.now().plusDays(1L).format(DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD));
            }
            triggerTime = Date.from(LocalDateTime.parse(day + " " + triggerTimeStr
                    , DateTimeFormatter.ofPattern(TimeUtil.YYYY_MM_DD_HH_MM_SS)).atZone(ZoneId.systemDefault()).toInstant());
        }
        return triggerTime;
    }

    /**
     * 计算版本号
     *
     * @param branchName    分支名
     * @param versionRegex  分支名截取规则
     * @param versionPrefix 版本前缀
     * @param commitId      commitId
     * @param useCommitId   是否拼接commitId
     * @return
     */
    private String getVersionName(String branchName, String versionRegex, String versionPrefix, String commitId, Integer useCommitId) {
        String versionName = StringUtils.EMPTY;
        String regex = StringUtils.EMPTY;
        if (StringUtils.isNotEmpty(versionRegex)) {
            String pattern = "\\(.*\\)";
            Pattern r = Pattern.compile(pattern);
            Matcher m = r.matcher(versionRegex);
            if (m.find()) {
                regex = m.group();
            }
            if (StringUtils.isBlank(regex)) {
                throw new DeployCiVersionRegexIllegalException();
            }
            regex = regex.substring(1, regex.lastIndexOf(")"));
        }
        if (StringUtils.isEmpty(regex)) {
            versionName += branchName + "_";
        } else {
            Pattern r = Pattern.compile(regex);
            Matcher m = r.matcher(branchName);
            if (m.find()) {
                versionName += m.group();
            } else {
                versionName += branchName;
            }
        }
        if (StringUtils.isNotBlank(versionPrefix)) {
            versionName = versionPrefix + versionName;
        }
        if (StringUtils.isNotBlank(commitId) && Objects.equals(useCommitId, 1)) {
            if (commitId.length() > 8) {
                versionName += ("_" + commitId.substring(0, 8));
            } else {
                versionName += ("_" + commitId);
            }
        }
        return versionName;
    }

}
