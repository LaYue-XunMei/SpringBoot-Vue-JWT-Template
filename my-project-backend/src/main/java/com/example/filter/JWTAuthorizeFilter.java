package com.example.filter;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.example.utils.JWTUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
/**
 * 用于对请求头中Jwt令牌进行校验的工具，为当前请求添加用户验证信息
 * 并将用户的ID存放在请求对象属性中，方便后续使用
 */
@Component
public class JWTAuthorizeFilter extends OncePerRequestFilter {

    @Resource
    JWTUtils utils;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        //验证逻辑
        String authorization = request.getHeader("Authorization");//从请求头中获取Authorization
        DecodedJWT jwt = utils.resolveJWT(authorization);//解析token
        if(jwt != null) {//token合法的话，就放行
            UserDetails user = utils.toUser(jwt);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user,null,user.getAuthorities());//security内部验证token
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));//设置请求信息
            SecurityContextHolder.getContext().setAuthentication(authentication);//放入上下文中,放入验证信息，已经通过JWT验证了
            request.setAttribute("id",utils.toID(jwt));
            //其他参数也可以放入上下文中
        }
        filterChain.doFilter(request,response);//放行
    }
}
