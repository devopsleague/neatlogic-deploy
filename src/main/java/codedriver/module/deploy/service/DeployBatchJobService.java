/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.deploy.service;

import codedriver.framework.deploy.dto.job.DeployJobVo;

public interface DeployBatchJobService {
    /**
     * 执行批量作业
     * @param deployJobVo 批量作业
     */
    void fireBatch(DeployJobVo deployJobVo);
}
