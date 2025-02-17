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

package neatlogic.module.deploy.api.appconfig.system;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.crossover.IResourceCrossoverMapper;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.crossover.CrossoverServiceFactory;
import neatlogic.framework.deploy.auth.DEPLOY;
import neatlogic.framework.deploy.constvalue.DeployAppConfigAction;
import neatlogic.framework.deploy.constvalue.JobSourceType;
import neatlogic.framework.deploy.dto.app.DeployAppModuleVo;
import neatlogic.framework.deploy.dto.app.DeployAppSystemVo;
import neatlogic.framework.deploy.dto.app.DeployResourceSearchVo;
import neatlogic.framework.dto.globallock.GlobalLockVo;
import neatlogic.framework.globallock.dao.mapper.GlobalLockMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.deploy.dao.mapper.DeployAppConfigMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author longrf
 * @date 2022/6/20 5:13 下午
 */
@Service
@AuthAction(action = DEPLOY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchDeployAppConfigAppSystemApi extends PrivateApiComponentBase {

    @Resource
    private DeployAppConfigMapper deployAppConfigMapper;

    @Resource
    private GlobalLockMapper globalLockMapper;

    @Override
    public String getToken() {
        return "deploy/app/config/appsystem/search";
    }

    @Override
    public String getName() {
        return "查询发布应用配置的应用系统列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索-应用名|模块名"),
            @Param(name = "isFavorite", type = ApiParamType.ENUM, rule = "0,1", desc = "是否只显示已收藏的"),
            @Param(name = "isConfig", type = ApiParamType.ENUM, rule = "0,1", desc = "是否只显示已配置的"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "appModuleIdList", type = ApiParamType.JSONARRAY, desc = "应用模块id列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true"),
            @Param(name = "pageRange", type = ApiParamType.JSONARRAY, desc = "分页范围")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = DeployAppSystemVo[].class, desc = "发布应用配置的应用系统列表")
    })
    @Description(desc = "查询发布应用配置的应用系统列表（含关键字过滤）")
    @Override
    public Object myDoService(JSONObject paramObj) {
        DeployResourceSearchVo searchVo = paramObj.toJavaObject(DeployResourceSearchVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            searchVo.setAppSystemIdList(idList);
            List<DeployAppSystemVo> tbodyList = deployAppConfigMapper.getAppSystemListByIdList(searchVo, UserContext.get().getUserUuid());
            return TableResultUtil.getResult(tbodyList, searchVo);
        }
        if(CollectionUtils.isNotEmpty(searchVo.getAuthorityActionList()) && searchVo.getAuthorityActionList().contains(DeployAppConfigAction.VIEW.getValue())){
            searchVo.getAuthorityActionList().addAll(Arrays.asList(DeployAppConfigAction.EXECUTE.getValue(), DeployAppConfigAction.EDIT.getValue(), DeployAppConfigAction.AUTH.getValue()));
        }
        List<DeployAppSystemVo> returnAppSystemList = new ArrayList<>();
        Integer count = deployAppConfigMapper.getAppSystemIdListCount(searchVo);
        if (count > 0) {
            searchVo.setRowNum(count);
            //过滤出有查看权限的系统
            List<Long> appSystemIdList = deployAppConfigMapper.getAppSystemIdList(searchVo, UserContext.get().getUserUuid());
            if (CollectionUtils.isEmpty(appSystemIdList)) {
                return TableResultUtil.getResult(returnAppSystemList, searchVo);
            }
            searchVo.setAppSystemIdList(appSystemIdList);
            //补充上述系统的所有权限
            if (StringUtils.isNotEmpty(searchVo.getKeyword())) {
                returnAppSystemList = deployAppConfigMapper.getAppSystemListIncludeModuleByIdList(searchVo, UserContext.get().getUserUuid());
            } else {
                returnAppSystemList = deployAppConfigMapper.getAppSystemListByIdList(searchVo, UserContext.get().getUserUuid());
            }

            /*补充系统是否有模块、是否有环境、是否有配置权限 ,补充模块是否配置、是否有环境、是否含有资源锁*/
            IResourceCrossoverMapper resourceCrossoverMapper = CrossoverServiceFactory.getApi(IResourceCrossoverMapper.class);
            List<Long> hasModuleAppSystemIdList = resourceCrossoverMapper.getHasModuleAppSystemIdListByAppSystemIdList(appSystemIdList);
            List<Long> hasEnvAppSystemIdList = deployAppConfigMapper.getHasEnvAppSystemIdListByAppSystemIdList(appSystemIdList);

            Set<String> globalLockKeySet = new HashSet<>();
            List<GlobalLockVo> globalLockVoList = globalLockMapper.getLockListByKeyListAndHandler(appSystemIdList.stream().map(Object::toString).collect(Collectors.toList()), JobSourceType.DEPLOY.getValue());
            if (CollectionUtils.isNotEmpty(globalLockVoList)) {
                globalLockKeySet = globalLockVoList.stream().map(GlobalLockVo::getKey).collect(Collectors.toSet());
            }
            for (DeployAppSystemVo returnSystemVo : returnAppSystemList) {
                //补充系统是否有模块、是否有环境、是否有配置权限
                if (hasModuleAppSystemIdList.contains(returnSystemVo.getId())) {
                    returnSystemVo.setIsHasModule(1);
                }
                if (hasEnvAppSystemIdList.contains(returnSystemVo.getId())) {
                    returnSystemVo.setIsHasEnv(1);
                }

                //补充模块是否配置、是否有环境
                if (CollectionUtils.isNotEmpty(returnSystemVo.getAppModuleList())) {
                    int isHasConfig = 0;
                    if (CollectionUtils.isNotEmpty(deployAppConfigMapper.getAppConfigListByAppSystemId(returnSystemVo.getId()))) {
                        isHasConfig = 1;
                    }
                    List<Long> appModuleIdList = deployAppConfigMapper.getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(returnSystemVo.getId(), returnSystemVo.getAppModuleList().stream().map(DeployAppModuleVo::getId).collect(Collectors.toList()));
                    for (DeployAppModuleVo appModuleVo : returnSystemVo.getAppModuleList()) {
                        if (appModuleIdList.contains(appModuleVo.getId())) {
                            appModuleVo.setIsHasEnv(1);
                        }
                        appModuleVo.setIsConfig(isHasConfig);
                    }
                }

                //补充isHasResourceLock
                if (globalLockKeySet.stream().anyMatch(o -> o.contains(returnSystemVo.getId().toString()))) {
                    returnSystemVo.setIsHasResourceLock(1);
                }
            }
        }
        return TableResultUtil.getResult(returnAppSystemList, searchVo);
    }
}


