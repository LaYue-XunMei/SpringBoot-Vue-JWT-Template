package com.example.controller;

import com.baomidou.mybatisplus.core.conditions.interfaces.Func;
import com.example.entity.RestBean;
import com.example.entity.vo.request.ConfirmResetVO;
import com.example.entity.vo.request.EmailRegisterVO;
import com.example.entity.vo.request.EmailResetVO;
import com.example.service.AccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
/**
 * 用于验证相关Controller包含用户的注册、重置密码等操作
 */
@Validated
@RestController
@RequestMapping("/api/auth")
@Tag(name = "登录校验相关", description = "包括用户登录、注册、验证码请求等操作。")
public class AuthorizeController {

    @Resource
    AccountService service;

    @GetMapping("/ask-code")
    public RestBean<Void> askVerifyCode(@RequestParam @Email String email,
                                        @RequestParam @Pattern(regexp = "register|reset") String type,
                                        HttpServletRequest request) {

        return this.messageHandle(()->service.registerEmailVerifyCode(type, email, request.getRemoteAddr()));
    }

    @PostMapping("/register")
    public RestBean<Void> register(@RequestBody @Valid EmailRegisterVO vo){//前端axios默认通过JSON请求，POST也通过JSON接收
      return this.messageHandle(()-> service.registerEmailAccount(vo));
    }

    @PostMapping("/reset-confirm")
    public RestBean<Void> restConfirm(@RequestBody @Valid ConfirmResetVO vo){
        //return this.messageHandle(() ->service.resetConfirm(vo));
        //再简化
        return this.messageHandle(vo,service::resetConfirm);
    }

    @PostMapping("/reset-password")
    public RestBean<Void> restPassword(@RequestBody @Valid EmailResetVO vo){
        return this.messageHandle(vo,service::resetEmailAccountPassword);
    }

    private <T> RestBean<Void> messageHandle(T vo, Function<T, String> function){
        return messageHandle(() ->function.apply(vo));

    }


    private RestBean<Void> messageHandle(Supplier<String> action){
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
        //        if(message == null)//如果message为空则正常发送
//            return RestBean.success();
//        else
//            return RestBean.failure(400,message);
    }
}
