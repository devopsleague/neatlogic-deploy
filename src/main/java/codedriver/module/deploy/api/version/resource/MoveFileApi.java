package codedriver.module.deploy.api.version.resource;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.deploy.dto.version.DeployVersionVo;
import codedriver.framework.deploy.exception.DeployVersionEnvNotFoundException;
import codedriver.framework.deploy.exception.DeployVersionNotFoundException;
import codedriver.framework.deploy.exception.MoveFileFailedException;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.integration.authentication.enums.AuthenticateType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.HttpRequestUtil;
import codedriver.framework.util.RegexUtils;
import codedriver.module.deploy.dao.mapper.DeployVersionMapper;
import codedriver.module.deploy.service.DeployVersionService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author laiwt
 * @date 2022/6/17 9:59 上午
 */
@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class MoveFileApi extends PrivateApiComponentBase {

    Logger logger = LoggerFactory.getLogger(MoveFileApi.class);

    @Resource
    DeployVersionMapper deployVersionMapper;

    @Resource
    DeployVersionService deployVersionService;

    @Override
    public String getName() {
        return "文件复制或重命名";
    }

    @Override
    public String getToken() {
        return "deploy/version/resource/file/move";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", desc = "版本id", isRequired = true, type = ApiParamType.LONG),
            @Param(name = "buildNo", desc = "buildNo", type = ApiParamType.INTEGER),
            @Param(name = "envId", desc = "环境ID", type = ApiParamType.LONG),
            @Param(name = "resourceType", rule = "version_product,env_product,diff_directory,sql_script", desc = "资源类型(version_product:版本制品;env_product:环境制品;diff_directory:差异目录;sql_script:SQL脚本)", isRequired = true, type = ApiParamType.ENUM),
            @Param(name = "name", desc = "新文件名(重命名时需指定)", rule = RegexUtils.NAME, maxLength = 50, type = ApiParamType.REGEX),
            @Param(name = "src", desc = "源文件路径(路径一律以'/'开头，HOME本身的路径为'/')", isRequired = true, type = ApiParamType.STRING),
            @Param(name = "dest", desc = "目标文件路径(路径一律以'/'开头，HOME本身的路径为'/')", type = ApiParamType.STRING),
            @Param(name = "operation", desc = "操作(move:移动;rename:重命名)", rule = "move,rename", isRequired = true, type = ApiParamType.ENUM)
    })
    @Description(desc = "文件复制或重命名")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        Integer buildNo = paramObj.getInteger("buildNo");
        Long envId = paramObj.getLong("envId");
        String resourceType = paramObj.getString("resourceType");
        String name = paramObj.getString("name");
        String src = paramObj.getString("src");
        String dest = paramObj.getString("dest");
        String operation = paramObj.getString("operation");
        if ("move".equals(operation) && StringUtils.isBlank(dest)) {
            throw new ParamNotExistsException("dest");
        }
        if ("rename".equals(operation) && StringUtils.isBlank(name)) {
            throw new ParamNotExistsException("name");
        }
        DeployVersionVo version = deployVersionMapper.getDeployVersionById(id);
        if (version == null) {
            throw new DeployVersionNotFoundException(id);
        }
        String envName = null;
        if (envId != null) {
            envName = deployVersionService.getVersionEnvNameByEnvId(envId);
            if (StringUtils.isBlank(envName)) {
                throw new DeployVersionEnvNotFoundException(version.getVersion(), envId);
            }
        }
        String url = deployVersionService.getVersionRunnerUrl(paramObj, version, envName);
        url += "api/rest/file/move";
        String fullSrcPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, src);
        String fullDestPath;
        if ("move".equals(operation)) {
            fullDestPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, dest + src.substring(src.lastIndexOf("/")));
        } else {
            fullDestPath = deployVersionService.getVersionResourceFullPath(version, resourceType, buildNo, envName, src.substring(0, src.lastIndexOf("/")) + "/" + name);
        }
        JSONObject paramJson = new JSONObject();
        paramJson.put("src", fullSrcPath);
        paramJson.put("dest", fullDestPath);
        HttpRequestUtil request = HttpRequestUtil.post(url).setPayload(paramJson.toJSONString()).setAuthType(AuthenticateType.BUILDIN).sendRequest();
        int responseCode = request.getResponseCode();
        String error = request.getError();
        if (StringUtils.isNotBlank(error)) {
            if (responseCode == 520) {
                throw new MoveFileFailedException(JSONObject.parseObject(error).getString("Message"));
            } else {
                throw new MoveFileFailedException(error);
            }
        }
        return null;
    }
}
