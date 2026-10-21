package com.ruankao.api.fixture;

import cn.dev33.satoken.annotation.SaCheckRole;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试夹具：<b>类级</b> {@code @SaCheckRole("ADMIN")}，方法上没有任何鉴权注解
 * （docs/16-p1-code-audit.md §4 E1）。
 *
 * <p>这是管理端控制器的惯用写法，也正是原实现的假阳性来源：Java 反射的
 * {@code isAnnotationPresent} 只作用于方法、<b>不会</b>向上读声明类的注解，于是这个类会被误报为
 * "既没标注解又不在白名单"。更糟的是原文案的第二种修法写着"在 AuthWhitelist.RULES 中登记"——
 * 被人按这条捷径改，管理端路径就真进了白名单。自测断言本类不被判为违规。
 *
 * <p>只存在于测试类路径，不会被打进产物。
 */
@RestController
@RequestMapping("/api/fixture/admin")
@SaCheckRole("ADMIN")
@Validated
public class ClassLevelFixtureController {

    @GetMapping("/only-admin")
    public String onlyAdmin() {
        return "ok";
    }
}
