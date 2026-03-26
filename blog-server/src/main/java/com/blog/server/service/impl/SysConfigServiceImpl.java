package com.blog.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.server.entity.SysConfig;
import com.blog.server.mapper.SysConfigMapper;
import com.blog.server.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    @Cacheable(value = "sysConfig", key = "#key", unless = "#result == null")
    public String getConfigValue(String key) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public List<SysConfig> getAllConfigs() {
        return sysConfigMapper.selectList(null);
    }

    @Override
    @CacheEvict(value = "sysConfig", key = "#key")
    public void updateConfig(String key, String value) {
        SysConfig config = sysConfigMapper.selectOne(
                new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, key)
        );
        if (config != null) {
            config.setConfigValue(value);
            sysConfigMapper.updateById(config);
        } else {
            SysConfig newConfig = new SysConfig();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setConfigName(key);
            newConfig.setConfigType("STRING");
            sysConfigMapper.insert(newConfig);
        }
    }
}
