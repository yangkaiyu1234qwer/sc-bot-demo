package com.yangky.scbotdemo.util;

import lombok.AllArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 用来读取项目配置文件里的属性
 *
 * @author yangky
 * @Date 2026/4/29 16:21
 */
@Component
@AllArgsConstructor
public class Properties {

    private Environment environment;

    /**
     * 获取字符串配置
     *
     * @param key 配置键
     * @return 配置值，不存在返回 null
     */
    public String getString(String key) {
        return environment.getProperty(key);
    }

    /**
     * 获取字符串配置（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public String getString(String key, String defaultValue) {
        return environment.getProperty(key, defaultValue);
    }

    /**
     * 获取整数配置（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Integer getInt(String key, Integer defaultValue) {
        return environment.getProperty(key, Integer.class, defaultValue);
    }

    /**
     * 获取布尔配置（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        return environment.getProperty(key, Boolean.class, defaultValue);
    }

    /**
     * 获取长整型配置（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Long getLong(String key, Long defaultValue) {
        return environment.getProperty(key, Long.class, defaultValue);
    }

    /**
     * 获取双精度浮点配置（带默认值）
     *
     * @param key          配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    public Double getDouble(String key, Double defaultValue) {
        return environment.getProperty(key, Double.class, defaultValue);
    }

    /**
     * 获取应用名称
     */
    public String getApplicationName() {
        return environment.getProperty("spring.application.name", "sc-bot-demo");
    }

    /**
     * 获取策略配置
     */
    public String getStrategyPolicy() {
        return environment.getProperty("strategy.policy", "Default");
    }

    public Boolean isWallOffOn() {
        return getBoolean("wall.off.button", false);
    }
}
