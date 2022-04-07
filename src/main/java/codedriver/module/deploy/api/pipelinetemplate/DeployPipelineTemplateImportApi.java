/*
 * Copyright(c) 2022 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.api.pipelinetemplate;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.CombopOperationType;
import codedriver.framework.autoexec.dao.mapper.AutoexecScriptMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecToolMapper;
import codedriver.framework.autoexec.dao.mapper.AutoexecTypeMapper;
import codedriver.framework.autoexec.dto.AutoexecToolVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseConfigVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseOperationVo;
import codedriver.framework.autoexec.dto.combop.AutoexecCombopPhaseVo;
import codedriver.framework.deploy.auth.DEPLOY_PIPELINE_TEMPLATE_MANAGE;
import codedriver.framework.deploy.dto.pipelinetemplate.DeployPipelineTemplateVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVersionVo;
import codedriver.framework.autoexec.dto.script.AutoexecScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.file.FileExtNotAllowedException;
import codedriver.framework.exception.file.FileNotUploadException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateBinaryStreamApiComponentBase;
import codedriver.module.deploy.dao.mapper.DeployPipelineTemplateMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.zip.ZipInputStream;

/**
 * 导入流水线模板接口
 *
 * @author linbq
 * @since 2021/4/13 11:21
 **/
@Service
@Transactional
@OperationType(type = OperationTypeEnum.CREATE)
@AuthAction(action = DEPLOY_PIPELINE_TEMPLATE_MANAGE.class)
public class DeployPipelineTemplateImportApi extends PrivateBinaryStreamApiComponentBase {

    @Resource
    private DeployPipelineTemplateMapper deployPipelineTemplateMapper;
    @Resource
    private AutoexecScriptMapper autoexecScriptMapper;

    @Resource
    private AutoexecTypeMapper autoexecTypeMapper;

    @Resource
    private AutoexecToolMapper autoexecToolMapper;

    @Override
    public String getToken() {
        return "deploy/pipelinetemplate/import";
    }

    @Override
    public String getName() {
        return "导入流水线模板";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
            @Param(name = "Return", type = ApiParamType.JSONARRAY, desc = "导入结果")
    })
    @Description(desc = "导入流水线模板")
    @Override
    public Object myDoService(JSONObject paramObj, HttpServletRequest request, HttpServletResponse response) throws Exception {
        MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest) request;
        //获取所有导入文件
        Map<String, MultipartFile> multipartFileMap = multipartRequest.getFileMap();
        //如果没有导入文件, 抛异常
        if (multipartFileMap.isEmpty()) {
            throw new FileNotUploadException();
        }
        int successCount = 0;
        int failureCount = 0;
        JSONArray failureReasonList = new JSONArray();
        byte[] buf = new byte[1024];
        //遍历导入文件
        for (Entry<String, MultipartFile> entry : multipartFileMap.entrySet()) {
            MultipartFile multipartFile = entry.getValue();
            //反序列化获取对象
            try (ZipInputStream zipis = new ZipInputStream(multipartFile.getInputStream());
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                while (zipis.getNextEntry() != null) {
                    int len;
                    while ((len = zipis.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    DeployPipelineTemplateVo deployPipelineTemplateVo = JSONObject.parseObject(new String(out.toByteArray(), StandardCharsets.UTF_8), new TypeReference<DeployPipelineTemplateVo>() {});
                    JSONObject resultObj = save(deployPipelineTemplateVo);
                    if (resultObj != null) {
                        failureCount++;
                        failureReasonList.add(resultObj);
                    } else {
                        successCount++;
                    }
                    out.reset();
                }
            } catch (Exception e) {
                throw new FileExtNotAllowedException(multipartFile.getOriginalFilename());
            }
        }
        JSONObject resultObj = new JSONObject();
        resultObj.put("successCount", successCount);
        resultObj.put("failureCount", failureCount);
        resultObj.put("failureReasonList", failureReasonList);
        return resultObj;
    }

    private JSONObject save(DeployPipelineTemplateVo deployPipelineTemplateVo) {
        Long id = deployPipelineTemplateVo.getId();
        String oldName = deployPipelineTemplateVo.getName();
        if (StringUtils.isBlank(oldName)) {
            throw new ClassCastException();
        }
        if (deployPipelineTemplateVo.getTypeId() == null){
            throw new ClassCastException();
        }

        if (deployPipelineTemplateVo.getConfig() == null){
            throw new ClassCastException();
        }
        DeployPipelineTemplateVo oldDeployPipelineTemplateVo = deployPipelineTemplateMapper.getPinelineTemplateById(id);
        if (oldDeployPipelineTemplateVo != null) {
            if (equals(oldDeployPipelineTemplateVo, deployPipelineTemplateVo)) {
                return null;
            }
        }

        Set<String> failureReasonSet = new HashSet<>();
        if (autoexecTypeMapper.checkTypeIsExistsById(deployPipelineTemplateVo.getTypeId()) == 0) {
            failureReasonSet.add("添加工具类型：'" + deployPipelineTemplateVo.getTypeId() + "'");
        }
        int index = 0;
        //如果导入的流程名称已存在就重命名
        while (deployPipelineTemplateMapper.checkPinelineTemplateNameIsRepeat(deployPipelineTemplateVo) != null) {
            index++;
            deployPipelineTemplateVo.setName(oldName + "_" + index);
        }
        String userUuid = UserContext.get().getUserUuid(true);
        deployPipelineTemplateVo.setFcu(userUuid);
        AutoexecCombopConfigVo config = deployPipelineTemplateVo.getConfig();
        int iSort = 0;
        List<AutoexecCombopPhaseVo> combopPhaseList = config.getCombopPhaseList();
        if (CollectionUtils.isNotEmpty(combopPhaseList)) {
            for (AutoexecCombopPhaseVo autoexecCombopPhaseVo : combopPhaseList) {
                if (autoexecCombopPhaseVo != null) {
                    autoexecCombopPhaseVo.setId(null);
                    autoexecCombopPhaseVo.setCombopId(id);
                    autoexecCombopPhaseVo.setSort(iSort++);
                    AutoexecCombopPhaseConfigVo phaseConfig = autoexecCombopPhaseVo.getConfig();
                    List<AutoexecCombopPhaseOperationVo> phaseOperationList = phaseConfig.getPhaseOperationList();
                    if (CollectionUtils.isNotEmpty(phaseOperationList)) {
                        Long combopPhaseId = autoexecCombopPhaseVo.getId();
                        int jSort = 0;
                        for (AutoexecCombopPhaseOperationVo autoexecCombopPhaseOperationVo : phaseOperationList) {
                            if (autoexecCombopPhaseOperationVo != null) {
                                autoexecCombopPhaseOperationVo.setSort(jSort++);
                                autoexecCombopPhaseOperationVo.setCombopPhaseId(combopPhaseId);
                                if (Objects.equals(autoexecCombopPhaseOperationVo.getOperationType(), CombopOperationType.SCRIPT.getValue())) {
                                    AutoexecScriptVo autoexecScriptVo = autoexecScriptMapper.getScriptBaseInfoById(autoexecCombopPhaseOperationVo.getOperationId());
                                    if (autoexecScriptVo == null) {
                                        failureReasonSet.add("添加自定义工具：'" + autoexecCombopPhaseOperationVo.getOperationId() + "'");
                                    } else {
                                        AutoexecScriptVersionVo autoexecScriptVersionVo = autoexecScriptMapper.getActiveVersionByScriptId(autoexecScriptVo.getId());
                                        if (autoexecScriptVersionVo == null) {
                                            failureReasonSet.add("启用自定义工具：'" + autoexecScriptVo.getName() + "'");
                                        }
                                    }
                                } else {
                                    AutoexecToolVo autoexecToolVo = autoexecToolMapper.getToolById(autoexecCombopPhaseOperationVo.getOperationId());
                                    if (autoexecToolVo == null) {
                                        failureReasonSet.add("添加工具：'" + autoexecCombopPhaseOperationVo.getOperationId() + "'");
                                    } else if (Objects.equals(autoexecToolVo.getIsActive(), 0)) {
                                        failureReasonSet.add("启用工具：'" + autoexecToolVo.getName() + "'");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (CollectionUtils.isEmpty(failureReasonSet)) {
            if (oldDeployPipelineTemplateVo == null) {
                deployPipelineTemplateMapper.insertPinelineTemplate(deployPipelineTemplateVo);
            } else {
                deployPipelineTemplateMapper.updatePinelineTemplateById(deployPipelineTemplateVo);
            }
            return null;
        } else {
            JSONObject resultObj = new JSONObject();
            resultObj.put("item", "导入：'" + oldName + "'，失败；请先在系统中：");
            resultObj.put("list", failureReasonSet);
            return resultObj;
        }
    }

    private boolean equals(DeployPipelineTemplateVo obj1, DeployPipelineTemplateVo obj2){
        if (!Objects.equals(obj1.getName(), obj2.getName())) {
            return false;
        }
        if (!Objects.equals(obj1.getDescription(), obj2.getDescription())) {
            return false;
        }
        if (!Objects.equals(obj1.getTypeId(), obj2.getTypeId())) {
            return false;
        }
        if (!Objects.equals(obj1.getConfigStr(), obj2.getConfigStr())) {
            return false;
        }
        return true;
    }
}
