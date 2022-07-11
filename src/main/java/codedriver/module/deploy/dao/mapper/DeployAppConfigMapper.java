package codedriver.module.deploy.dao.mapper;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.deploy.dto.app.*;
import codedriver.framework.dto.AuthenticationInfoVo;
import codedriver.framework.dto.runner.RunnerGroupVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author lvzk
 * @date 2022/5/23 12:19 下午
 */
public interface DeployAppConfigMapper {

    List<Long> getAppSystemIdList(@Param("searchVo") DeployResourceSearchVo searchVo, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemListByIdList(@Param("idList") List<Long> idList, @Param("schemaName") String schemaName, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemModuleListBySystemIdList(@Param("idList") List<Long> idList, @Param("isConfig") Integer isConfig, @Param("schemaName") String schemaName, @Param("userUuid") String userUuid);

    List<DeployAppConfigResourceVo> getAppSystemListByUserUuid(@Param("userUuid") String userUuid, @Param("searchVo") DeployResourceSearchVo searchVo);

    Set<Long> getViewableAppSystemIdList(AuthenticationInfoVo authenticationInfoVo);

    Integer getAppConfigAuthorityCount(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityList(DeployAppConfigAuthorityVo searchVo);

    List<DeployAppConfigAuthorityVo> getAppConfigAuthorityDetailList(@Param("appConfigAuthList") List<DeployAppConfigAuthorityVo> appConfigAuthList);

    List<DeployAppEnvAutoConfigVo> getAppEnvAutoConfigListBySystemIdAndModuleIdAndEnvIdAndInstanceIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId, @Param("instanceIdList") List<Long> instanceIdList);

    List<DeployAppEnvAutoConfigKeyValueVo> getAppEnvAutoConfigKeyValueList(DeployAppEnvAutoConfigVo envAutoConfigVo);

    String getAppConfig(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigVo(DeployAppConfigVo deployAppConfigVo);

    DeployAppConfigVo getAppConfigByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    List<DeployAppConfigVo> getAppConfigListByAppSystemId(Long appSystemId);

    String getAppConfigOverrideConfig(DeployAppConfigOverrideVo deployAppConfigOverrideVo);

    List<DeployAppConfigOverrideVo> getAppConfigOverrideListByAppSystemId(Long appSystemId);

    DeployAppConfigVo getAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    List<DeployAppConfigInstanceVo> getAppModuleEnvNotEnvOrSameEnvAndNotModuleInstanceList(@Param("searchVo") DeployAppConfigInstanceVo searchVo, @Param("schemaName") String schemaName);

    List<DeployAppEnvironmentVo> getDeployAppEnvListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList, @Param("schemaName") String schemaName);

    List<DeployAppEnvironmentVo> getDeployAppHasNotEnvListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList, @Param("schemaName") String schemaName);

    List<Long> getHasEnvAppModuleIdListByAppSystemIdAndModuleIdList(@Param("appSystemId") Long appSystemId, @Param("appModuleIdList") List<Long> appModuleIdList, @Param("schemaName") String schemaName);

    List<Long> getAppModuleEnvAutoConfigInstanceIdList(@Param("searchVo") DeployAppEnvAutoConfigVo searchVo, @Param("schemaName") String schemaName);

    List<Long> getAppConfigEnvDBConfigResourceIdByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    RunnerGroupVo getAppModuleRunnerGroupByAppSystemIdAndModuleId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId);

    DeployAppConfigEnvDBConfigVo getAppConfigEnvDBConfigById(Long id);

    List<DeployAppConfigEnvDBConfigVo> getAppConfigEnvDBConfigListByAppSystemIdAndAppModuleIdAndEnvId(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envId") Long envId);

    int getAppConfigEnv(DeployAppConfigVo deployAppConfigVo);

    List<Long> getAppConfigAppSystemIdListByAppSystemIdList(List<Long> list);

    List<Long> getAppConfigUserAppSystemIdList(@Param("userUuid") String userUuid, @Param("appSystemIdList") List<Long> appSystemIdList);

    Integer insertAppConfigAuthority(DeployAppConfigAuthorityVo deployAppConfigAuthorityVo);

    Integer insertAppModuleRunnerGroup(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("runnerGroupId") Long runnerGroupId);

    Integer insertAppEnvAutoConfig(DeployAppEnvAutoConfigVo appEnvAutoConfigVo);

    Integer insertAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer insertAppConfigOverride(DeployAppConfigOverrideVo deployAppOverrideOverrideVo);

    Integer insertAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    void insertAppConfigEnv(@Param("appSystemId") Long appSystemId, @Param("appModuleId") Long appModuleId, @Param("envIdList") List<Long> envIdList);

    void insertAppConfigSystemFavorite(@Param("appSystemId") Long appSystemId, @Param("userUuid") String userUuid);

    void insertAppConfigEnvDBConfig(DeployAppConfigEnvDBConfigVo dbConfigVo);

    void insertAppConfigEnvDBConfigAccount(@Param("dbConfigId") Long dbConfigId, @Param("accountList") List<DeployAppConfigEnvDBConfigAccountVo> accountList);

    Integer updateAppConfig(DeployAppConfigVo deployAppConfigVo);

    Integer updateAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    Integer deleteAppConfigAuthorityByAppIdAndEnvIdAndAuthUuidAndLcd(@Param("appSystemId") Long appSystemId, @Param("envId") Long envId, @Param("authUuid") String uuid, @Param("lcd") Date nowTime);

    Integer deleteAppEnvAutoConfig(DeployAppEnvAutoConfigVo deployAppEnvAutoConfigVo);

    Integer deleteAppConfigDraft(DeployAppConfigVo deployAppConfigDraftVo);

    Integer getAppSystemIdListCount(DeployResourceSearchVo searchVo);

    int getCiEntityIdListCount(Integer isConfig);

    int getAppModuleEnvAutoConfigInstanceIdCount(@Param("searchVo") DeployAppEnvAutoConfigVo searchVo, @Param("schemaName") String schemaName);

    int getAppModuleEnvNotEnvOrSameEnvAndNotModuleInstanceIdCount(@Param("searchVo") DeployAppConfigInstanceVo searchVo, @Param("schemaName") String schemaName);

    int checkDeployAppConfigEnvDBAliasNameIsRepeat(DeployAppConfigEnvDBConfigVo configVo);

    int checkDeployAppConfigEnvDBExistsById(Long id);

    int getAppSystemCountNew(String sql);

    List<Long> getAppSystemIdListNew(String sql);

    List<DeployAppSystemVo> getAppSystemListByIdListNew(String sql);

    int getAppConfigEnvDatabaseCount(String sql);

    List<Long> getAppConfigEnvDatabaseResourceIdList(String sql);

    List<ResourceVo> getAppConfigEnvDatabaseResourceListByIdList(String sql);

    void deleteAppConfigSystemFavoriteByAppSystemIdAndUserUuid(@Param("appSystemId") Long appSystemId, @Param("userUuid") String userUuid);

    void deleteAppConfig(DeployAppConfigVo configVo);

    void deleteAppConfigEnv(DeployAppConfigVo deployAppConfigVo);

    void deleteAppConfigAuthorityByAppSystemId(Long appSystemId);

    void deleteAppModuleRunnerGroup(DeployAppConfigVo configVo);

    void deleteAppConfigDBConfigAccountByDBConfigId(Long id);

    void deleteAppConfigDBConfigAccount(DeployAppConfigEnvDBConfigVo appConfigEnvDBConfigVo);

    void deleteAppConfigDBConfig(DeployAppConfigEnvDBConfigVo appConfigEnvDBConfigVo);

    void deleteAppConfigDBConfigById(Long id);
}
