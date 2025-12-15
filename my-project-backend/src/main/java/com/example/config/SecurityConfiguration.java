package com.example.config;

import com.example.entity.RestBean;
import com.example.entity.dto.Account;
import com.example.entity.vo.response.AuthorizeVO;
import com.example.filter.JWTAuthorizeFilter;
import com.example.service.AccountService;
import com.example.utils.JWTUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.io.IOException;
import java.io.PrintWriter;
/**
 * SpringSecurity相关配置
 */
@Configuration
public class SecurityConfiguration {

    @Resource
    JWTUtils utils;

    @Resource
    JWTAuthorizeFilter jwtAuthorizeFilter;

    @Resource
    AccountService accountService;
    /**
     * 针对于 SpringSecurity 6 的新版配置方法
     * @param http 配置器
     * @return 自动构建的内置过滤器链
     * @throws Exception 可能的异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(conf-> conf
                        .requestMatchers("/api/auth/**","/error").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(conf -> conf
                        .loginProcessingUrl("/api/auth/login")
                        .failureHandler(this::onAuthenticationFailure)
                        .successHandler(this::onAuthenticationSuccess)
                )
                .logout(conf -> conf
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler(this::onLogoutSuccess)
                )
                .exceptionHandling(conf -> conf
                        .authenticationEntryPoint(this::onUnauthorized)//未登录时的处理
                        .accessDeniedHandler(this::onAccessDeny)//权限不足
                )
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(conf -> conf
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(jwtAuthorizeFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }




    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException{
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        User user = (User)authentication.getPrincipal();
        Account account = accountService.findAccountByNameOrEmail(user.getUsername());//查出用户信息
        String token =utils.createJWT(user,account.getId(),account.getUsername());
        //AuthorizeVO vo = new AuthorizeVO();//返回vo对象，防止信息泄露
        AuthorizeVO vo = account.asViewObject(AuthorizeVO.class,v ->{
            v.setExpire(utils.expireTime());
            v.setToken(token);
        });//通过自定义方法实现自动赋值给vo对象
        //BeanUtils.copyProperties(account,vo);//拷贝属性
        response.getWriter().write(RestBean.success(vo).asJsonString());

    }

    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }

    public void onLogoutSuccess(HttpServletRequest request,
                                HttpServletResponse response,
                                Authentication authentication) throws IOException, ServletException {
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        String authorization = request.getHeader("Authorization");
        if(utils.invalidateJwt(authorization)){
            writer.write(RestBean.success("退出登录成功").asJsonString());//失效后退出成功
        }else {
            writer.write(RestBean.failure(400,"退出登录失败").asJsonString());
        }
    }

    public void onAccessDeny(HttpServletRequest request,
                             HttpServletResponse response,
                             AccessDeniedException accessDeniedException) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(RestBean.forbidden(accessDeniedException.getMessage()).asJsonString());
    }

    public void onUnauthorized(HttpServletRequest request,
                               HttpServletResponse response,
                               AuthenticationException exception) throws IOException{
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(RestBean.unauthorized(exception.getMessage()).asJsonString());
    }
}
