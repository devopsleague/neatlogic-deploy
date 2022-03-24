package codedriver.module.deploy.service;


import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dto.AutoexecParamVo;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.deploy.dao.mapper.DeployProfileMapper;
import codedriver.framework.deploy.dto.profile.DeployProfileOperationVo;
import codedriver.framework.deploy.dto.profile.DeployProfileVo;
import codedriver.framework.deploy.exception.profile.DeployProfileIsNotFoundException;
import com.alibaba.fastjson.JSONArray;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * @author longrf
 * @date 2022/3/21 3:32 下午
 */
@Service
public class DeployProfileServiceImpl implements DeployProfileService {

    @Resource
    DeployProfileMapper deployProfileMapper;

    @Resource
    AutoexecToolMapper autoexecToolMapper;

    @Resource
    AutoexecScriptMapper autoexecScriptMapper;

    /**
     * 获取profile参数
     *
     * @param id
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileParamById(Long id) {
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        List<DeployProfileOperationVo> toolVoList = null;
        List<DeployProfileOperationVo> scriptVoList = null;
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }

        //获取profile关联的tool、script工具关系
        List<DeployProfileOperationVo> profileOperationVoList = deployProfileMapper.getProfileOperationVoListByProfileId(id);
        if (CollectionUtils.isNotEmpty(profileOperationVoList)) {
            toolVoList = profileOperationVoList.stream().filter(e -> StringUtils.equals(e.getType(), ToolType.TOOL.getValue())).collect(Collectors.toList());
            scriptVoList = profileOperationVoList.stream().filter(e -> StringUtils.equals(e.getType(), ToolType.SCRIPT.getValue())).collect(Collectors.toList());
        }

        List<Long> toolIdList = null;
        List<Long> scriptIdList = null;
        if (CollectionUtils.isNotEmpty(toolVoList)) {
            toolIdList = toolVoList.stream().map(DeployProfileOperationVo::getOperateId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(scriptVoList)) {
            scriptIdList = scriptVoList.stream().map(DeployProfileOperationVo::getOperateId).collect(Collectors.toList());
        }
        return getProfileConfig(toolIdList, scriptIdList, profileVo.getConfig().getJSONArray("paramList"));
    }

    /**
     * 获取工具参数并去重
     *
     * @param toolIdList   工具id
     * @param scriptIdList 脚本id
     * @param paramList    工具参数
     * @return
     */
    @Override
    public List<AutoexecParamVo> getProfileConfig(List<Long> toolIdList, List<Long> scriptIdList, JSONArray paramList) {

//         说明：
//         新的参数列表：工具和脚本参数的去重集合（name唯一键）
//         旧的参数列表：数据库存的
//         新旧名称和类型都相同时，将继续使用旧参数值，不做值是否存在的校验，前端回填失败提示即可
//

        List<AutoexecParamVo> toolAndScriptParamVoList = new ArrayList<>();
        List<AutoexecToolAndScriptVo> ToolAndScriptVoList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            ToolAndScriptVoList.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            ToolAndScriptVoList.addAll(autoexecScriptMapper.getScriptListByIdList(scriptIdList));
        }
        for (AutoexecToolAndScriptVo toolAndScriptVo : ToolAndScriptVoList) {
            toolAndScriptParamVoList.addAll(toolAndScriptVo.getParamList());
        }

        //根据name（唯一键）去重
        toolAndScriptParamVoList = toolAndScriptParamVoList.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(AutoexecParamVo::getName))), ArrayList::new));

        //实时的参数信息
        Map<String, AutoexecParamVo> newOperationParamMap = toolAndScriptParamVoList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));

        //旧的参数信息
        Map<String, AutoexecParamVo> oldOperationParamMap = new HashMap<>();
        if (CollectionUtils.isEmpty(paramList)) {
            List<AutoexecParamVo> oldParamList = paramList.toJavaList(AutoexecParamVo.class);
            oldOperationParamMap = oldParamList.stream().collect(Collectors.toMap(AutoexecParamVo::getName, e -> e));
        }

        //找出需要替换值的参数
        List<String> replaceNameList = new ArrayList<>();
        if (MapUtils.isNotEmpty(newOperationParamMap) && MapUtils.isNotEmpty(oldOperationParamMap)) {
            for (String newParamName : newOperationParamMap.keySet()) {
                if (oldOperationParamMap.containsKey(newParamName) && StringUtils.equals(oldOperationParamMap.get(newParamName).getType(), newOperationParamMap.get(newParamName).getType())) {
                    replaceNameList.add(newParamName);
                }
            }
        }

        if (CollectionUtils.isNotEmpty(replaceNameList)) {
            for (String name : replaceNameList) {
                newOperationParamMap.get(name).setConfig(oldOperationParamMap.get(name).getConfigStr());
            }
        }

        List<AutoexecParamVo> returnList = new ArrayList<>();
        for (String name : newOperationParamMap.keySet()) {
            returnList.add(newOperationParamMap.get(name));
        }

        return returnList;
    }

    /**
     * 根据profileId查询关联的tool、script工具
     *
     * @param id
     * @return
     */
    @Override
    public List<AutoexecToolAndScriptVo> getAutoexecToolAndScriptVoListByProfileId(Long id) {
        DeployProfileVo profileVo = deployProfileMapper.getProfileVoById(id);
        if (profileVo == null) {
            throw new DeployProfileIsNotFoundException(id);
        }
        List<AutoexecToolAndScriptVo> toolAndScriptVoList = profileVo.getAutoexecToolAndScriptVoList();
        List<AutoexecToolAndScriptVo> returnToolAndScriptVoList = new ArrayList<>();

        Map<String, List<AutoexecToolAndScriptVo>> toolAndScriptMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(toolAndScriptVoList)) {
            toolAndScriptMap = toolAndScriptVoList.stream().collect(Collectors.groupingBy(AutoexecToolAndScriptVo::getType));
        }
        //tool
        List<Long> toolIdList = null;
        if (toolAndScriptMap.containsKey(ToolType.TOOL.getValue())) {
            toolIdList = toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(toolIdList)) {
            returnToolAndScriptVoList.addAll(autoexecToolMapper.getToolListByIdList(toolIdList));
        }
        //script
        List<Long> scriptIdList = null;
        if (toolAndScriptMap.containsKey(ToolType.SCRIPT.getValue())) {
            scriptIdList = toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(scriptIdList)) {
            returnToolAndScriptVoList.addAll(autoexecScriptMapper.getScriptListByIdList(scriptIdList));
        }
        return returnToolAndScriptVoList;
    }

}
