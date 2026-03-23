package com.blog.server.controller;

import com.blog.server.common.Result;
import com.blog.server.entity.SiteConfig;
import com.blog.server.entity.MenuConfig;
import com.blog.server.exception.BusinessException;
import com.blog.server.mapper.SiteConfigMapper;
import com.blog.server.mapper.MenuConfigMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SiteConfigController {

    private final SiteConfigMapper siteConfigMapper;
    private final MenuConfigMapper menuConfigMapper;

    // ============ Site Config ============

    @GetMapping("/api/admin/site-config")
    public Result<List<SiteConfig>> getAllConfigs() {
        return Result.ok(siteConfigMapper.selectList(new LambdaQueryWrapper<>()));
    }

    @PutMapping("/api/admin/site-config")
    public Result<Void> updateConfigs(@RequestBody Map<String, String> configs) {
        configs.forEach((key, value) -> {
            SiteConfig config = siteConfigMapper.selectOne(
                    new LambdaQueryWrapper<SiteConfig>().eq(SiteConfig::getConfigKey, key));
            if (config != null) {
                config.setConfigValue(value);
                siteConfigMapper.updateById(config);
            } else {
                SiteConfig newConfig = new SiteConfig();
                newConfig.setConfigKey(key);
                newConfig.setConfigValue(value);
                siteConfigMapper.insert(newConfig);
            }
        });
        return Result.ok();
    }

    /** Public endpoint for frontend blog to read config */
    @GetMapping("/api/public/site-config")
    public Result<List<SiteConfig>> getPublicConfigs() {
        return Result.ok(siteConfigMapper.selectList(new LambdaQueryWrapper<>()));
    }

    // ============ Menu Config ============

    @GetMapping("/api/admin/menu")
    public Result<List<MenuConfig>> listMenus() {
        return Result.ok(menuConfigMapper.selectList(
                new LambdaQueryWrapper<MenuConfig>().orderByAsc(MenuConfig::getSortOrder)));
    }

    @PostMapping("/api/admin/menu")
    public Result<Long> createMenu(@RequestBody MenuRequest request) {
        MenuConfig menu = new MenuConfig();
        menu.setName(request.getName());
        menu.setUrl(request.getUrl());
        menu.setTarget(request.getTarget() != null ? request.getTarget() : "_self");
        menu.setIcon(request.getIcon());
        menu.setParentId(request.getParentId());
        menu.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menu.setVisible(request.getVisible() != null ? request.getVisible() : true);
        menu.setIsExternal(request.getIsExternal() != null ? request.getIsExternal() : false);
        menuConfigMapper.insert(menu);
        return Result.ok(menu.getId());
    }

    @PutMapping("/api/admin/menu/{id}")
    public Result<Void> updateMenu(@PathVariable Long id, @RequestBody MenuRequest request) {
        MenuConfig menu = menuConfigMapper.selectById(id);
        if (menu == null) throw new BusinessException(404, "Menu not found");
        menu.setName(request.getName());
        menu.setUrl(request.getUrl());
        if (request.getTarget() != null) menu.setTarget(request.getTarget());
        menu.setIcon(request.getIcon());
        menu.setParentId(request.getParentId());
        if (request.getSortOrder() != null) menu.setSortOrder(request.getSortOrder());
        if (request.getVisible() != null) menu.setVisible(request.getVisible());
        if (request.getIsExternal() != null) menu.setIsExternal(request.getIsExternal());
        menuConfigMapper.updateById(menu);
        return Result.ok();
    }

    @DeleteMapping("/api/admin/menu/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuConfigMapper.deleteById(id);
        return Result.ok();
    }

    /** Batch update sort order — receives an ordered list of {id, sortOrder} */
    @PutMapping("/api/admin/menu/sort")
    public Result<Void> batchSort(@RequestBody List<SortItem> items) {
        for (SortItem item : items) {
            MenuConfig menu = menuConfigMapper.selectById(item.getId());
            if (menu != null) {
                menu.setSortOrder(item.getSortOrder());
                menuConfigMapper.updateById(menu);
            }
        }
        return Result.ok();
    }

    /** Public endpoint for frontend blog */
    @GetMapping("/api/public/menus")
    public Result<List<MenuConfig>> getPublicMenus() {
        return Result.ok(menuConfigMapper.selectList(
                new LambdaQueryWrapper<MenuConfig>()
                        .eq(MenuConfig::getVisible, true)
                        .orderByAsc(MenuConfig::getSortOrder)));
    }

    @Data
    public static class MenuRequest {
        private String name;
        private String url;
        private String target;
        private String icon;
        private Long parentId;
        private Integer sortOrder;
        private Boolean visible;
        private Boolean isExternal;
    }

    @Data
    public static class SortItem {
        private Long id;
        private Integer sortOrder;
    }
}
