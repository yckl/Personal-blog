package com.blog.server.service;

import com.blog.server.entity.SysConfig;
import java.util.List;

public interface SysConfigService {
    String getConfigValue(String key);
    List<SysConfig> getAllConfigs();
    void updateConfig(String key, String value);
}
