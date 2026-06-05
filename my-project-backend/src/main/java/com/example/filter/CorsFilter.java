package com.example.filter;


import com.example.utils.Const;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Const.ORDER_CORS)//过滤器执行顺序，早于security的-100
public class CorsFilter extends HttpFilter {

    /**
     * 该过滤器用于设置跨域头，允许Origin发起的请求
     * @param request   HttpServletRequest对象
     * @param response   HttpServletResponse对象
     * @param chain        FilterChain对象
     */
    @Override
    protected void doFilter(HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain chain) throws IOException, ServletException {
        this.addCorsHeader(request,response);//设置跨域
        chain.doFilter(request,response);//全部放行
    }


    /**
     * 添加跨域头，允许Origin发起的请求，methods为GET、POST、PUT、DELETE、OPTIONS，headers为Authorization、Content-Type
     * @param request HttpServletRequest对象
     * @param response HttpServletResponse对象
     */
    private void addCorsHeader(HttpServletRequest request,HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));//Origin是发起请求的原始站点,设置允许哪些站点访问，开发阶段就先允许任何站点
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Authorization, Content-Type");
    }

}
