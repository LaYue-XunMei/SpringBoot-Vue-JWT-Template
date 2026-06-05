package com.example.controller;

import com.example.entity.RestBean;
import com.example.entity.vo.request.ConfirmResetVO;
import com.example.entity.vo.request.EmailRegisterVO;
import com.example.entity.vo.request.EmailResetVO;
import com.example.service.AccountService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.function.Function;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {

    @Resource
    AccountService service;

    @GetMapping("/ask-code")//get请求就用请求参数的形式接收
    public RestBean<Void> askVerifyCode(@RequestParam @Email String email,//接口参数校验是否合法
                                        @RequestParam @Pattern(regexp = "register|reset") String type,
                                        HttpServletRequest request) {

        return this.messageHandle(()->service.registerEmailVerifyCode(type, email, request.getRemoteAddr()));
    }

    @PostMapping("/register")//接收的是JSON格式数据，用实体对象接收
    public RestBean<Void> register(@RequestBody @Valid EmailRegisterVO vo){
        return this.messageHandle(()->service.registerEmailAccount(vo));
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


    //重载一下，都是传入一个VO
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
