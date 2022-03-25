package codedriver.module.deploy.api.profile;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.autoexec.constvalue.ToolType;
import codedriver.framework.autoexec.dto.AutoexecToolAndScriptVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.auth.DEPLOY_PROFILE_MODIFY;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.deploy.service.DeployProfileService;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/3/23 5:45 下午
 */
@AuthAction(action = DEPLOY_PROFILE_MODIFY.class)
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class DeployProfileParamListApi extends PrivateApiComponentBase {


    @Resource
    DeployProfileService deployProfileService;

    @Override
    public String getName() {
        return "获取工具profile参数列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Override
    public String getToken() {
        return "deploy/profile/param/list";
    }

    @Input({
            @Param(name = "autoexecToolAndScriptVoList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "关联的工具和脚本列表")
    })
    @Output({
    })
    @Description(desc = "获取工具profile参数列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Map<String, List<AutoexecToolAndScriptVo>> toolAndScriptMap = paramObj.getJSONArray("autoexecToolAndScriptVoList").toJavaList(AutoexecToolAndScriptVo.class).stream().collect(Collectors.groupingBy(AutoexecToolAndScriptVo::getType));
        List<Long> toolIdList = null;
        List<Long> scriptIdList = null;
        if (toolAndScriptMap.containsKey(ToolType.TOOL.getValue())) {
            toolIdList = toolAndScriptMap.get(ToolType.TOOL.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        if (toolAndScriptMap.containsKey(ToolType.SCRIPT.getValue())) {
            scriptIdList = toolAndScriptMap.get(ToolType.SCRIPT.getValue()).stream().map(AutoexecToolAndScriptVo::getId).collect(Collectors.toList());
        }
        //获取工具参数并去重
        return deployProfileService.getProfileConfig(toolIdList, scriptIdList, null);
    }
}
