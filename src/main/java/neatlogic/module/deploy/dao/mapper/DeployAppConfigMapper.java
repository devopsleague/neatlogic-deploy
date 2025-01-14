package neatlogic.module.deploy.dao.mapper;

import neatlogic.framework.cmdb.dto.resourcecenter.entity.AppEnvironmentVo;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.deploy.crossover.IDeployAppConfigCrossoverMapper;
import neatlogic.framework.deploy.dto.app.*;
import neatlogic.framework.dto.AuthenticationInfoVo;
import neatlogic.framework.dto.runner.RunnerGroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author lvzk
 * @date 2022/5/23 12:19 下午
 */
public interface DeployAppConfigMapper extends IDeployAppConfigCrossoverMapper {

    List<Long> getAppSystemIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppSystemVo> searchAppSystemList(DeployResourceSearchVo searchVo);

    List<DeployAppSystemVo> getAppSystemListIncludeModuleByIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppSystemVo> getAppSystemListByIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppSystemVo> getAppSystemModuleListBySystemIdList(@Param("idList") List<Long> idList, @Param("isConfig") Integer isConfig, @Param("userUuid") String userUuid);

    List<DeployAppSystemVo> getAppSystemListByUserUuid(@Param("userUuid") String userUuid, @Param("searchVo") DeployResourceSearchVo searchVo);

    Set<Long> getViewableAppSystemIdList(AuthenticationInfoVo authenticationInfoVo);

    Integer getAppConfigAuthorityCount(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityList(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityDetailList(@Param("appConfigAuthList") List<DeployAppConfigAuthorityVo> appConfigAuthList);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityListByAppSystemId(Long appSystemId);

    List<DeployAppEnvAutoConfigVo> getAppEnvAutoConfigListBySystemIdAndModuleIdAndEnvIdAndInstanceIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceIdList") List<Long> instanceIdList);

    List<DeployAppEnvAutoConfigKeyValueVo> getAppEnvAutoConfigKeyValueList(DeployAppEnvAutoConfigVo envAutoConfigVo);

    List<DeployAppEnvAutoConfigKeyValueVo> getAppEnvAttrList(DeployAppConfigEnvAttrVo envAutoConfigVo);

    String getAppConfig(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigVo(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    List<DeployAppConfigVo> getAppConfigListByAppSystemId(Long appSystemId);

    int getAppConfigCountByAppSystemId(Long appSystemId);

    List<DeployAppConfigVo> getAppConfigList(DeployAppConfigVo configVo);

    int getAllDeployAppConfigCount();

    List<DeployAppConfigVo> getAllAppConfigListByPage(BasePageVo searchVo);

    DeployAppConfigVo getAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    List<DeployAppConfigInstanceVo> searchAppConfigEnvInstanceList(DeployAppConfigInstanceVo searchVo);

    List<DeployAppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList);

    List<DeployAppEnvironmentVo> getCmdbEnvListByAppSystemIdAndModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<DeployAppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<Long> getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList);

    List<Long> getAppModuleEnvAutoConfigInstanceIdList(DeployAppEnvAutoConfigVo searchVo);

    RunnerGroupVo getAppModuleRunnerGroupByAppSystemIdAndModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<Long> getAppModuleIdListHasRunnerByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList);

    DeployAppConfigEnvDBConfigVo getAppConfigEnvDBConfigById(Long id);

    List<DeployAppConfigEnvDBConfigVo> getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    int getAppConfigEnv(DeployAppConfigVo deployAppConfigVo);

    List<Long> getAppConfigAppSystemIdListByAppSystemIdList(List<Long> list);

    List<Long> getAppConfigUserAppSystemIdList(@Param("userUuid") String userUuid, @Param("appSystemIdList") List<Long> appSystemIdList);

    List<DeployAppEnvAutoConfigVo> getAppEnvAutoConfigBySystemIdAndModuleIdAndEnvId(@Param("systemId") Long systemId, @Param("moduleId") Long moduleId, @Param("envId") Long envId);

    List<DeployAppModuleEnvVo> getDeployAppModuleEnvListByAppSystemId(Long appSystemId);

    List<DeployAppModuleEnvVo> getDeployAppModuleEnvListByAppSystemIdAndAppModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList);

    List<AppEnvironmentVo> getDeployAppModuleEnvListByAppSystemIdAndModuleId(@Param("systemId") Long systemId, @Param("moduleId") Long moduleId);

    List<DeployAppEnvironmentVo> getAppConfigEnvListIncludeDBCSchemaListAndAutoCfgKeyListByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envIdList") List<Long> envIdList);

    DeployAppSystemVo getAppSystemById(Long appSystemId);

    List<DeployAppModuleVo> getAppModuleListBySystemId(Long appSystemId);

    Integer insertAppConfigAuthority(DeployAppConfigAuthorityVo deployAppConfigAuthorityVo);

    Integer insertAppModuleRunnerGroup(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("runnerGroupId") Long runnerGroupId);

    void insertAppModuleRunnerGroupList(@Param("runnerGroupVoList") List<DeployAppModuleRunnerGroupVo> runnerGroupVoList);

    Integer insertAppEnvAutoConfig(DeployAppEnvAutoConfigVo appEnvAutoConfigVo);

    Integer insertAppConfigEnvAttr(DeployAppConfigEnvAttrVo appConfigEnvAttrVo);

    /**
     * 插入autoCfg时 DUPLICATE 只update key
     *
     * @param appEnvAutoConfigVo autoConfigVo
     */
    void insertAppEnvAutoConfigNew(DeployAppEnvAutoConfigVo appEnvAutoConfigVo);

    void insertBatchAppEnvAutoConfig(@Param("appEnvAutoConfigVoList") List<DeployAppEnvAutoConfigVo> appEnvAutoConfigVoList);

    Integer insertAppConfig(DeployAppConfigVo deployAppConfigVo);

    void insertBatchAppConfig(@Param("configList") List<DeployAppConfigVo> configList);

    Integer insertAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    void insertAppConfigEnv(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envIdList") List<Long> envIdList);

    void insertAppConfigSystemFavorite(@Param("appSystemId") Long appSystemId, @Param("userUuid") String userUuid);

    void insertAppConfigEnvDBConfig(DeployAppConfigEnvDBConfigVo dbConfigVo);

    void insertBatchAppConfigEnvDBConfig(@Param("dbConfigVoList") List<DeployAppConfigEnvDBConfigVo> dbConfigVoList);

    Integer updateAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer updateAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    void updateDeployAppConfigEnvDBConfig(DeployAppConfigEnvDBConfigVo dbConfigVo);

    void deleteAppConfigAuthorityByAppIdAndAuthUuidListAndLcd(@Param("appSystemId") Long appSystemId, @Param("authUuidList") List<String> authUuidList, @Param("lcd") Date nowTime);

    Integer deleteAppConfigAuthorityByAppIdAndAuthUuidList(@Param("appSystemId") Long appSystemId, @Param("uuidList") List<String> uuidList);

    Integer deleteAppEnvAutoConfig(DeployAppEnvAutoConfigVo deployAppEnvAutoConfigVo);

    Integer deleteAppConfigEnvAttr(DeployAppConfigEnvAttrVo deployAppConfigEnvAttrVo);

    Integer deleteAppEnvAutoConfigByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    Integer deleteAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    Integer getAppSystemIdListCount(DeployResourceSearchVo searchVo);

    Long getAppSystemLimitMaxId(DeployResourceSearchVo searchVo);

    String getAppSystemNotifyPolicyConfigByAppSystemId(Long appSystemId);

    int getCiEntityIdListCount(Integer isConfig);

    int getAppModuleEnvAutoConfigInstanceIdCount(DeployAppEnvAutoConfigVo searchVo);

    /**
     * 查询发布应用配置的应用实例下的无模块无环境、无模块同环境、同模块无环境的实例个数
     *
     * @param searchVo searchVo
     * @return count
     */
    int getAppConfigEnvInstanceCount(DeployAppConfigInstanceVo searchVo);

    int checkDeployAppConfigEnvDBSchemaIsRepeat(DeployAppConfigEnvDBConfigVo configVo);

    int getAppModuleCountBySystemIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId);

    /**
     * 查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库的数量
     *
     * @param searchVo   searchVo
     * @return count
     */
    int getAppConfigEnvDatabaseCount(DeployResourceSearchVo searchVo);

    /**
     * 查询发布应用配置DB库下的无模块无环境、无模块同环境、同模块无环境、同模块同环境且发布没配置的数据库
     *
     * @param searchVo   searchVo
     * @return 数据库idList
     */
    List<Long> getAppConfigEnvDatabaseResourceIdList(DeployResourceSearchVo searchVo);

    List<Long> getHasEnvAppSystemIdListByAppSystemIdList(List<Long> idList);

    List<Long> getDeployAppEnvIdListByAppSystemId(Long appSystemId);

    List<Long> getDeployAppHasAuthorityAppSystemIdListByAppSystemIdList(@Param("appSystemIdSet") Set<Long> appSystemIdSet);

    List<DeployAppModuleVo> getAppModuleListBySystemIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId);

    List<DeployAppModuleVo> getDeployHasNotConfigAppModuleListByAppSystemIdAndAppModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    List<DeployAppEnvironmentVo> getDeployHasNotConfigAppEnvListByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    List<DeployAppConfigAuthorityActionVo> getDeployAppAuthorityActionList(DeployAppAuthCheckVo deployAppAuthCheckVo);

    List<DeployAppConfigAuthorityActionVo> getDeployAppAllAuthorityActionListByAppSystemIdAndAuthUuidList(@Param("appSystemId") Long appSystemId, @Param("authUuidList") List<String> authUuidList);

    List<DeployAppAuthCheckVo> getBatchDeployAppAuthorityActionList(List<DeployAppAuthCheckVo> deployAppAuthCheckVoList);

    List<DeployAppSystemVo> getBatchAppConfigAuthorityListByAppSystemIdList(List<Long> appSystemIdList);

    List<Map<String, Object>> getDeployAppConfigListForUpdateConfig(BasePageVo searchVo);

    List<Long> getAppModuleIdListByAppSystemIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId);

    void updateDeployAppConfigById(@Param("id") Long id, @Param("configStr") String configStr);

    void deleteAppConfigSystemFavoriteByAppSystemIdAndUserUuid(@Param("appSystemId") Long appSystemId, @Param("userUuid") String userUuid);

    void deleteAppConfig(DeployAppConfigVo configVo);

    void deleteAppModuleAppConfig(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    void deleteAppEnvAppConfig(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    void deleteAppConfigEnv(DeployAppConfigVo deployAppConfigVo);

    void deleteAppConfigAuthorityByAppSystemId(Long appSystemId);

    void deleteAppModuleRunnerGroup(DeployAppConfigVo configVo);

    void deleteAppConfigDBConfig(DeployAppConfigEnvDBConfigVo appConfigEnvDBConfigVo);

    void deleteAppConfigDBConfigById(Long id);

    void deleteAppConfigEnvByAppSystemIdAndAppModuleIdAndEnvIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envIdList") List<Long> envIdList);

    void deleteAppConfigDBConfigByIdList(List<Long> idList);
}
