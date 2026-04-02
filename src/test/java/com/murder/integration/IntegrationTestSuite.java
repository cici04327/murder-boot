package com.murder.integration;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/**
 * 集成测试套件
 * 运行此类可以执行所有集成测试
 */
@Suite
@SuiteDisplayName("剧本杀系统集成测试套件")
@SelectClasses({
        CouponIntegrationTest.class,
        FeedbackIntegrationTest.class,
        FrontendSmokeIntegrationTest.class,
        NotificationIntegrationTest.class,
        PaymentIntegrationTest.class,
        ReservationIntegrationTest.class,
        ScriptIntegrationTest.class,
        StoreIntegrationTest.class,
        UserIntegrationTest.class,
        VipIntegrationTest.class,
        GroupIntegrationTest.class,
        ArticleIntegrationTest.class,
        StoreEmployeeIntegrationTest.class
})
public class IntegrationTestSuite {
    // 测试套件类，无需实现任何方法
}
