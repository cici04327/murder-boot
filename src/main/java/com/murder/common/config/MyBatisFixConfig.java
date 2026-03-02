package com.murder.common.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 修复配置
 * 解决 ddlApplicationRunner Bean 冲突问题
 */
@Configuration
public class MyBatisFixConfig implements BeanFactoryPostProcessor {

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // 移除 MyBatis-Plus 自动配置中有问题的 ddlApplicationRunner Bean
        if (beanFactory instanceof BeanDefinitionRegistry) {
            BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
            if (registry.containsBeanDefinition("ddlApplicationRunner")) {
                registry.removeBeanDefinition("ddlApplicationRunner");
            }
        }
    }
}
