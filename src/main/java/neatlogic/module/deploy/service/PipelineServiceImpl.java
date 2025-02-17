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

package neatlogic.module.deploy.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.autoexec.constvalue.ParamMappingMode;
import neatlogic.framework.autoexec.crossover.IAutoexecCombopCrossoverService;
import neatlogic.framework.autoexec.dto.combop.*;
import neatlogic.framework.cmdb.crossover.IAppSystemMapper;
import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppSystemVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.dependency.core.DependencyManager;
import neatlogic.framework.deploy.auth.PIPELINE_MODIFY;
import neatlogic.framework.deploy.auth.core.DeployAppAuthChecker;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.PipelineType;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.deploy.dto.pipeline.PipelineJobTemplateVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineSearchVo;
import neatlogic.framework.deploy.dto.pipeline.PipelineVo;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import neatlogic.module.deploy.dao.mapper.DeployPipelineMapper;
import neatlogic.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler;
import neatlogic.module.deploy.dependency.handler.AutoexecScenarioDeployPipelineDependencyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PipelineServiceImpl implements PipelineService {
    private final static Logger logger = LoggerFactory.getLogger(PipelineServiceImpl.class);
    @Resource
    DeployPipelineMapper deployPipelineMapper;

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;


    @Override
    public List<PipelineVo> searchPipeline(PipelineSearchVo searchVo) {
        IAppSystemMapper appSystemMapper = CrossoverServiceFactory.getApi(IAppSystemMapper.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<PipelineVo> pipelineList = deployPipelineMapper.getPipelineListByIdList(idList);
            for (PipelineVo pipeline : pipelineList) {
                if (pipeline.getAppSystemId() != null) {
                    AppSystemVo appSystemVo = appSystemMapper.getAppSystemById(pipeline.getAppSystemId());
                    if (appSystemVo != null) {
                        pipeline.setAppSystemName(appSystemVo.getName());
                        pipeline.setAppSystemAbbrName(appSystemVo.getAbbrName());
                    }
                }
            }
            return pipelineList;
        }
        // 判断是否需要验证权限
        int isHasAllAuthority = 0;
        if (Objects.equals(searchVo.getNeedVerifyAuth(), 1)) {
            String type = searchVo.getType();
            if (StringUtils.isNotBlank(type)) {
                if (PipelineType.APPSYSTEM.getValue().equals(type)) {
                    Set<String> actionSet = DeployAppAuthChecker.builder(searchVo.getAppSystemId())
                            .addOperationAction(DeployAppConfigAction.PIPELINE.getValue())
                            .check();
                    if (actionSet.contains(DeployAppConfigAction.PIPELINE.getValue())) {
                        isHasAllAuthority = 1;
                    }
                } else if (PipelineType.GLOBAL.getValue().equals(type)) {
                    if (AuthActionChecker.check(PIPELINE_MODIFY.class)) {
                        isHasAllAuthority = 1;
                    }
                }
            }
        } else {
            // 不需要验证权限的话，就当拥有所有权限
            isHasAllAuthority = 1;
        }

        searchVo.setIsHasAllAuthority(isHasAllAuthority);
        searchVo.setAuthUuid(UserContext.get().getUserUuid());
        int rowNum = deployPipelineMapper.searchPipelineCount(searchVo);
        searchVo.setRowNum(rowNum);
        List<PipelineVo> pipelineList = deployPipelineMapper.searchPipeline(searchVo);
        Map<Long, AppSystemVo> appSystemMap = new HashMap<>();
        List<Long> appSystemIdList = pipelineList.stream().map(PipelineVo::getAppSystemId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            List<AppSystemVo> appSystemList = appSystemMapper.getAppSystemListByIdList(appSystemIdList);
            appSystemMap = appSystemList.stream().collect(Collectors.toMap(e -> e.getId(), e -> e));
        }
        for (PipelineVo pipeline : pipelineList) {
            if (pipeline.getAppSystemId() != null) {
                AppSystemVo appSystemVo = appSystemMap.get(pipeline.getAppSystemId());
                if (appSystemVo != null) {
                    pipeline.setAppSystemName(appSystemVo.getName());
                    pipeline.setAppSystemAbbrName(appSystemVo.getAbbrName());
                }
            }
        }
        return pipelineList;
    }

    @Override
    public List<PipelineJobTemplateVo> searchPipelineJobTemplate(PipelineJobTemplateVo pipelineJobTemplateVo) {
        int rowNum = deployPipelineMapper.searchJobTemplateCount(pipelineJobTemplateVo);
        pipelineJobTemplateVo.setRowNum(rowNum);
        return deployPipelineMapper.searchJobTemplate(pipelineJobTemplateVo);
    }

    @Override
    public void deleteDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }

        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            DependencyManager.delete(AutoexecScenarioDeployPipelineDependencyHandler.class, deployAppConfigVo.getId());
        }

        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }
        Long moduleId = deployAppConfigVo.getAppModuleId();
        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (moduleId != null) {
                //如果是模块层或环境层，没有重载，就不用保存依赖关系
                Integer override = combopPhaseVo.getOverride();
                if (Objects.equals(override, 0)) {
                    continue;
                }
            }
            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            deleteOperationDependency(phaseConfig.getPhaseOperationList());
        }
    }

    /**
     * 删除工具依赖
     *
     * @param operationList 工具列表
     */
    private void deleteOperationDependency(List<AutoexecCombopPhaseOperationVo> operationList) {
        if (CollectionUtils.isNotEmpty(operationList)) {
            for (AutoexecCombopPhaseOperationVo operation : operationList) {
                if (operation == null) {
                    continue;
                }
                DependencyManager.delete(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, operation.getId());
                DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, operation.getId());
                DependencyManager.delete(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, operation.getId());
                AutoexecCombopPhaseOperationConfigVo operationConfig = operation.getConfig();
                deleteOperationDependency(operationConfig.getIfList());
                deleteOperationDependency(operationConfig.getElseList());
                deleteOperationDependency(operationConfig.getOperations());
            }
        }
    }

    /**
     * 找出修改部分配置信息
     *
     * @param fullConfig   前端传过来的全量配置信息
     * @param parentConfig 如果当前层是环境层，parentConfig表示的是模块层修改部分配置信息；如果当前层是模块层，parentConfig应该为null。
     * @return
     */
    @Override
    public DeployPipelineConfigVo getModifiedPartConfig(DeployPipelineConfigVo fullConfig, DeployPipelineConfigVo parentConfig) {
        DeployPipelineConfigVo result = new DeployPipelineConfigVo();
        boolean flag = false;
        // 阶段
        List<DeployPipelinePhaseVo> overridePhaseList = new ArrayList<>();
        List<Long> disabledPhaseIdList = new ArrayList<>();
        List<DeployPipelinePhaseVo> phaseList = fullConfig.getCombopPhaseList();
        for (DeployPipelinePhaseVo phaseVo : phaseList) {
            if (Objects.equals(phaseVo.getOverride(), 1)) {
                flag = true;
                overridePhaseList.add(phaseVo);
            }
            if (Objects.equals(phaseVo.getIsActive(), 0)) {
                // 如果当前层是环境层，模块层禁用了该阶段，环境层不能激活该阶段，这时isActive=0也不用加入禁用列表disabledPhaseIdList中
                if (parentConfig == null || CollectionUtils.isEmpty(parentConfig.getDisabledPhaseIdList()) || !parentConfig.getDisabledPhaseIdList().contains(phaseVo.getId())) {
                    flag = true;
                    disabledPhaseIdList.add(phaseVo.getId());
                }
            }
        }
        result.setCombopPhaseList(overridePhaseList);
        result.setDisabledPhaseIdList(disabledPhaseIdList);
        // 阶段组
        List<DeployPipelineGroupVo> overrideGroupList = new ArrayList<>();
        List<DeployPipelineGroupVo> groupList = fullConfig.getCombopGroupList();
        for (DeployPipelineGroupVo groupVo : groupList) {
            if (Objects.equals(groupVo.getInherit(), 0)) {
                flag = true;
                overrideGroupList.add(groupVo);
            }
        }
        result.setCombopGroupList(overrideGroupList);
        // 执行账号
        DeployPipelineExecuteConfigVo executeConfigVo = fullConfig.getExecuteConfig();
        if (Objects.equals(executeConfigVo.getInherit(), 0)) {
            flag = true;
            result.setExecuteConfig(executeConfigVo);
        }
        // 预置参数
        List<DeployProfileVo> overrideProfileList = new ArrayList<>();
        List<DeployProfileVo> profileList = fullConfig.getOverrideProfileList();
        for (DeployProfileVo deployProfileVo : profileList) {
            List<DeployProfileParamVo> overrideProfileParamList = new ArrayList<>();
            List<DeployProfileParamVo> profileParamList = deployProfileVo.getParamList();
            for (DeployProfileParamVo profileParamVo : profileParamList) {
                if (Objects.equals(profileParamVo.getInherit(), 0)) {
                    overrideProfileParamList.add(profileParamVo);
                }
            }
            if (CollectionUtils.isNotEmpty(overrideProfileParamList)) {
                flag = true;
                DeployProfileVo overrideProfileVo = new DeployProfileVo();
                overrideProfileVo.setProfileId(deployProfileVo.getProfileId());
                overrideProfileVo.setProfileName(deployProfileVo.getProfileName());
                overrideProfileVo.setParamList(overrideProfileParamList);
                overrideProfileList.add(overrideProfileVo);
            }
        }
        result.setOverrideProfileList(overrideProfileList);
        if (flag) {
            return result;
        }
        return null;
    }

    @Override
    public void saveDeployAppPipeline(DeployAppConfigVo deployAppConfigVo) {
        Long appSystemId = deployAppConfigVo.getAppSystemId();
        Long appModuleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();
        String configStr = deployAppConfigVo.getConfigStr();
        IAutoexecCombopCrossoverService autoexecCombopCrossoverService = CrossoverServiceFactory.getApi(IAutoexecCombopCrossoverService.class);
        autoexecCombopCrossoverService.verifyAutoexecCombopConfig(deployAppConfigVo.getConfig().getAutoexecCombopConfigVo(), false);
        deployAppConfigVo.setConfigStr(configStr);
        deployAppConfigVo.setFcu(UserContext.get().getUserUuid());
        deployAppConfigVo.setLcu(UserContext.get().getUserUuid());
        if (appModuleId == 0L && envId == 0L) {
            // 应用层
            DeployAppConfigVo oldAppSystemAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId));
            if (oldAppSystemAppConfigVo != null) {
                if (Objects.equals(oldAppSystemAppConfigVo.getConfigStr(), deployAppConfigVo.getConfigStr())) {
                    return;
                } else {
                    deleteDependency(oldAppSystemAppConfigVo);
                    deployAppConfigVo.setId(oldAppSystemAppConfigVo.getId());
                    deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
                    saveDependency(deployAppConfigVo);
                }
            } else {
                deployAppConfigVo.setId(null);
                deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
                saveDependency(deployAppConfigVo);
            }
        } else if (envId == 0L) {
            // 模块层
            DeployAppConfigVo oldAppModuleAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId));
            // 找出修改部分配置
            DeployPipelineConfigVo modifiedPartConfig = getModifiedPartConfig(deployAppConfigVo.getConfig(), null);
            if (modifiedPartConfig == null) {
                if (oldAppModuleAppConfigVo != null) {
                    deleteDependency(oldAppModuleAppConfigVo);
                    deployAppConfigMapper.deleteAppModuleAppConfig(appSystemId, appModuleId);
                }
                return;
            }
            deployAppConfigVo.setConfig(modifiedPartConfig);
            if (oldAppModuleAppConfigVo != null) {
                deleteDependency(oldAppModuleAppConfigVo);
                deployAppConfigVo.setId(oldAppModuleAppConfigVo.getId());
                deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
                saveModifiedPartConfigDependency(deployAppConfigVo);
            } else {
                deployAppConfigVo.setId(null);
                deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
                saveModifiedPartConfigDependency(deployAppConfigVo);
            }
        } else {
            // 环境层
            DeployPipelineConfigVo appModuleAppConfigConfig = null;
            DeployAppConfigVo oldAppModuleAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId));
            if (oldAppModuleAppConfigVo != null) {
                appModuleAppConfigConfig = oldAppModuleAppConfigVo.getConfig();
            }

            DeployAppConfigVo oldAppEnvAppConfigVo = deployAppConfigMapper.getAppConfigVo(new DeployAppConfigVo(appSystemId, appModuleId, envId));
            // 找出修改部分配置
            DeployPipelineConfigVo modifiedPartConfig = getModifiedPartConfig(deployAppConfigVo.getConfig(), appModuleAppConfigConfig);
            if (modifiedPartConfig == null) {
                if (oldAppEnvAppConfigVo != null) {
                    deleteDependency(oldAppEnvAppConfigVo);
                    deployAppConfigMapper.deleteAppEnvAppConfig(appSystemId, appModuleId, envId);
                }
                return;
            }
            deployAppConfigVo.setConfig(modifiedPartConfig);
            if (oldAppEnvAppConfigVo != null) {
                deleteDependency(oldAppEnvAppConfigVo);
                deployAppConfigVo.setId(oldAppEnvAppConfigVo.getId());
                deployAppConfigMapper.updateAppConfig(deployAppConfigVo);
                saveModifiedPartConfigDependency(deployAppConfigVo);
            } else {
                deployAppConfigVo.setId(null);
                deployAppConfigMapper.insertAppConfig(deployAppConfigVo);
                saveModifiedPartConfigDependency(deployAppConfigVo);
            }
        }
    }

    /**
     * 保存重载部分阶段中操作工具对预置参数集和全局参数的引用关系、流水线对场景的引用关系
     *
     * @param deployAppConfigVo
     */
    private void saveModifiedPartConfigDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }

        Long appSystemId = deployAppConfigVo.getAppSystemId();
        Long moduleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();

        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }

        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (moduleId != null) {
                //如果是模块层或环境层，没有重载，就不用保存依赖关系
                Integer override = combopPhaseVo.getOverride();
                if (Objects.equals(override, 0)) {
                    continue;
                }
            }
            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            saveOperationDependency(phaseConfig.getPhaseOperationList(), combopPhaseVo, appSystemId, moduleId, envId);
        }
    }

    /**
     * 保存工具依赖
     *
     * @param operationVos  工具列表
     * @param combopPhaseVo 组合工具阶段
     * @param appSystemId   应用id
     * @param moduleId      模块id
     * @param envId         环境id
     */
    private void saveOperationDependency(List<AutoexecCombopPhaseOperationVo> operationVos, AutoexecCombopPhaseVo combopPhaseVo, Long appSystemId, Long moduleId, Long envId) {
        if (CollectionUtils.isNotEmpty(operationVos)) {
            for (AutoexecCombopPhaseOperationVo operationVo : operationVos) {
                if (operationVo == null) {
                    continue;
                }
                saveDependency(combopPhaseVo, operationVo, appSystemId, moduleId, envId);
                AutoexecCombopPhaseOperationConfigVo operationConfig = operationVo.getConfig();
                saveOperationDependency(operationConfig.getIfList(), combopPhaseVo, appSystemId, moduleId, envId);
                saveOperationDependency(operationConfig.getElseList(), combopPhaseVo, appSystemId, moduleId, envId);
                saveOperationDependency(operationConfig.getOperations(), combopPhaseVo, appSystemId, moduleId, envId);
            }
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系、流水线对场景的引用关系
     *
     * @param deployAppConfigVo
     */
    private void saveDependency(DeployAppConfigVo deployAppConfigVo) {
        DeployPipelineConfigVo config = deployAppConfigVo.getConfig();
        if (config == null) {
            return;
        }

        Long appSystemId = deployAppConfigVo.getAppSystemId();
        Long moduleId = deployAppConfigVo.getAppModuleId();
        Long envId = deployAppConfigVo.getEnvId();

        JSONObject dependencyConfig = new JSONObject();
        dependencyConfig.put("appSystemId", appSystemId);
        dependencyConfig.put("moduleId", moduleId);
        dependencyConfig.put("envId", envId);

        List<AutoexecCombopScenarioVo> scenarioList = config.getScenarioList();
        if (CollectionUtils.isNotEmpty(scenarioList)) {
            for (AutoexecCombopScenarioVo scenarioVo : scenarioList) {
                dependencyConfig.put("scenarioId", scenarioVo.getScenarioId());
                dependencyConfig.put("scenarioName", scenarioVo.getScenarioName());
                DependencyManager.insert(AutoexecScenarioDeployPipelineDependencyHandler.class, scenarioVo.getScenarioId(), deployAppConfigVo.getId(), dependencyConfig);
            }
        }

        List<DeployPipelinePhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isEmpty(combopPhaseList)) {
            return;
        }

        for (DeployPipelinePhaseVo combopPhaseVo : combopPhaseList) {
            if (combopPhaseVo == null) {
                continue;
            }
            if (moduleId != null) {
                //如果是模块层或环境层，没有重载，就不用保存依赖关系
                Integer override = combopPhaseVo.getOverride();
                if (Objects.equals(override, 0)) {
                    continue;
                }
            }
            AutoexecCombopPhaseConfigVo phaseConfig = combopPhaseVo.getConfig();
            if (phaseConfig == null) {
                continue;
            }
            saveOperationDependency(phaseConfig.getPhaseOperationList(), combopPhaseVo, appSystemId, moduleId, envId);
        }
    }

    /**
     * 保存阶段中操作工具对预置参数集和全局参数的引用关系
     *
     * @param combopPhaseVo
     * @param phaseOperationVo
     * @param appSystemId
     * @param moduleId
     * @param envId
     */
    private void saveDependency(AutoexecCombopPhaseVo combopPhaseVo, AutoexecCombopPhaseOperationVo phaseOperationVo, Long appSystemId, Long moduleId, Long envId) {
        AutoexecCombopPhaseOperationConfigVo operationConfigVo = phaseOperationVo.getConfig();
        if (operationConfigVo == null) {
            return;
        }
        Long profileId = operationConfigVo.getProfileId();
        if (profileId != null) {
            JSONObject dependencyConfig = new JSONObject();
            dependencyConfig.put("appSystemId", appSystemId);
            dependencyConfig.put("moduleId", moduleId);
            dependencyConfig.put("envId", envId);
            dependencyConfig.put("phaseId", combopPhaseVo.getId());
            dependencyConfig.put("phaseName", combopPhaseVo.getName());
            DependencyManager.insert(AutoexecProfile2DeployAppPipelinePhaseOperationDependencyHandler.class, profileId, phaseOperationVo.getId(), dependencyConfig);
        }
        List<ParamMappingVo> paramMappingList = operationConfigVo.getParamMappingList();
        if (CollectionUtils.isNotEmpty(paramMappingList)) {
            for (ParamMappingVo paramMappingVo : paramMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("appSystemId", appSystemId);
                    dependencyConfig.put("moduleId", moduleId);
                    dependencyConfig.put("envId", envId);
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    dependencyConfig.put("key", paramMappingVo.getKey());
                    dependencyConfig.put("name", paramMappingVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationInputParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
        List<ParamMappingVo> argumentMappingList = operationConfigVo.getArgumentMappingList();
        if (CollectionUtils.isNotEmpty(argumentMappingList)) {
            for (ParamMappingVo paramMappingVo : argumentMappingList) {
                if (Objects.equals(paramMappingVo.getMappingMode(), ParamMappingMode.GLOBAL_PARAM.getValue())) {
                    JSONObject dependencyConfig = new JSONObject();
                    dependencyConfig.put("appSystemId", appSystemId);
                    dependencyConfig.put("moduleId", moduleId);
                    dependencyConfig.put("envId", envId);
                    dependencyConfig.put("phaseId", combopPhaseVo.getId());
                    dependencyConfig.put("phaseName", combopPhaseVo.getName());
                    DependencyManager.insert(AutoexecGlobalParam2DeployAppPipelinePhaseOperationArgumentParamDependencyHandler.class, paramMappingVo.getValue(), phaseOperationVo.getId(), dependencyConfig);
                }
            }
        }
    }
}
