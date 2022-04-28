/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.globallock;

import codedriver.framework.dto.globallock.GlobalLockVo;
import codedriver.framework.globallock.GlobalLockManager;
import codedriver.framework.globallock.core.GlobalLockHandlerBase;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class DeployGlobalLockHandler extends GlobalLockHandlerBase {
    @Override
    public String getHandler() {
        return "deploy";
    }

    @Override
    public String getHandlerName() {
        return "发布 fileLock";
    }

    @Override
    public boolean getIsCanLock(List<GlobalLockVo> globalLockVoList, GlobalLockVo globalLockVo) {
        String mode = null;
        for (GlobalLockVo globalLock : globalLockVoList){
            if(StringUtils.isNotBlank(mode) && !Objects.equals(globalLock.getHandlerParam().getString("mode"),mode)){
                globalLockVo.setWaitReason("your mode is '"+mode+"',already has '"+globalLock.getHandlerParam().getString("mode")+"' lock");
                return false;
            }
            if(StringUtils.isNotBlank(mode) && Objects.equals("write",mode ) && Objects.equals(globalLock.getHandlerParam().getString("mode"),mode)){
                globalLockVo.setWaitReason("your mode is '"+mode+"',already has '"+globalLock.getHandlerParam().getString("mode")+"' lock");
                return false;
            }
            mode = globalLock.getHandlerParam().getString("mode");
            try {
                Thread.sleep(1000L);
            }catch (Exception ignored){}
        }
        return true;
    }

    @Override
    public JSONObject getLock(JSONObject paramJson) {
        JSONObject jsonObject = new JSONObject();
        GlobalLockVo globalLockVo = new GlobalLockVo();
        globalLockVo.setHandler("deploy");
        globalLockVo.setDescription(paramJson.getString("lockOwnerName"));
        globalLockVo.setKey(paramJson.getString("lockOwner")+"/"+paramJson.getString("lockTarget"));
        globalLockVo.setHandlerParamStr(paramJson.toJSONString());
        GlobalLockManager.getLock(globalLockVo);
        if (globalLockVo.getIsLock() == 1) {
            jsonObject.put("lockId", globalLockVo.getId());
            jsonObject.put("wait", 0);
        } else {
            jsonObject.put("wait", 1);
            jsonObject.put("message", globalLockVo.getWaitReason());
        }
        return jsonObject;
    }

    @Override
    public void cancelLock(Long lockId, JSONObject paramJson) {
        GlobalLockManager.cancelLock(lockId, paramJson);
    }

    @Override
    public void doNotify(GlobalLockVo globalLockVo, JSONObject paramJson) {

    }
}
