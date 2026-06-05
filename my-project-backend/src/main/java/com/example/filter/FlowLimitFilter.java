package com.example.filter;

import com.example.entity.RestBean;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Order(Const.ORDER_LIMIT)
public class FlowLimitFilter extends HttpFilter {

    @Resource
    StringRedisTemplate template;

    @Override
    public void doFilter(HttpServletRequest request,
                         HttpServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        String addr = request.getRemoteAddr();
        if(this.tryCount(addr)){//如果计数成功，正常频率则放行
            chain.doFilter(request, response);
        }else{//请求过快，拦截
            this.writeBlockMessage(response);
        }
    }

    private void writeBlockMessage(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;UTF-8");
        response.getWriter().write(RestBean.forbidden("操作频繁，请稍后再试").toString());
    }

    private boolean tryCount(String ip){
        synchronized (ip.intern()){
            //防止同一时间被多次调用，加锁
            if(Boolean.TRUE.equals(template.hasKey(Const.FLOW_LIMIT_BLOCK+ip)))//先查看是否被封禁
                return false;
            return this.limitPeriodCheck(ip);//如果还没被封禁，继续检查
        }
    }

    private boolean limitPeriodCheck(String ip){
        if (Boolean.TRUE.equals(template.hasKey(Const.FLOW_LIMIT_COUNTER+ip))) {
            Long increment = Optional.ofNullable(template.opsForValue().increment(Const.FLOW_LIMIT_COUNTER + ip)).orElse(0L);
            if(increment>10){//10次请求，封禁3秒
                template.opsForValue().set(Const.FLOW_LIMIT_BLOCK+ip,"",30, TimeUnit.SECONDS);
                return false;
            }
        }else{
            template.opsForValue().set(Const.FLOW_LIMIT_COUNTER+ip,"1",3, TimeUnit.SECONDS);
        }
        return true;
    }

}
