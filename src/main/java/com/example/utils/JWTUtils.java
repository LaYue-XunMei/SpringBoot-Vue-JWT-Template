package com.example.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class JWTUtils {
    @Value("${spring.security.jwt.key}")
    String key;

    @Value("${spring.security.jwt.expire}")
    int expire;

    @Resource
    StringRedisTemplate template;

    public boolean invalidateJwt(String HeaderToken){
        String token = this.convertToken(HeaderToken);
        if(token == null) return false;
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try{
            DecodedJWT jwt = jwtVerifier.verify(token);
            String id = jwt.getId();
            return deleteToken(id,jwt.getExpiresAt());
        }catch (JWTVerificationException e){
            return false;
        }
    }

    private boolean deleteToken(String uuid,Date time){//是否成功删除，指定过期时间
        if(this.isInvalidToken(uuid)) return false;
        Date now = new Date();
        long expire = Math.max(0,time.getTime() - now.getTime());//剩余存放时间,时间必须大于0，防止已经过期
        template.opsForValue().set(Const.JWT_BLACK_LIST+uuid,"",expire, TimeUnit.MILLISECONDS);
        return true;
    }

    private boolean isInvalidToken(String uuid){//判断是否已经失效
      return Boolean.TRUE.equals(template.hasKey(Const.JWT_BLACK_LIST+uuid));//redis中是否存在黑名单中
    }

    public DecodedJWT resolveJWT(String HeaderToken){
        String token = this.convertToken(HeaderToken);
        if(token == null) return null;
        Algorithm algorithm = Algorithm.HMAC256(key);
        JWTVerifier jwtVerifier = JWT.require(algorithm).build();
        try{
            DecodedJWT jwt = jwtVerifier.verify(token);//验证token是否合法，有没有被篡改过
            if(this.isInvalidToken(jwt.getId())) return null;//判断是否已经失效
            Date expireAt = jwt.getExpiresAt();//查看是否过期
            return  new Date().after(expireAt) ? null : jwt;
        }catch (JWTVerificationException e){//运行时异常
            return null;
        }
    }

    public String createJWT(UserDetails details,int id,String username){ {
        Algorithm algorithm = Algorithm.HMAC256(key);
        Date expireTime = this.expireTime();
        return JWT.create()
                .withJWTId(UUID.randomUUID().toString())//创建时赋予ID，在拉黑时使用
                .withClaim("id",id)
                .withClaim("name",username)
                .withClaim("authorities",details.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList())
                .withExpiresAt(expireTime)
                .withIssuedAt(new Date())
                .sign(algorithm);
        }
    }
    public Date expireTime(){
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR,expire*24);
        return calendar.getTime();
    }

    private String convertToken(String headerToken) {
        if(headerToken == null || !headerToken.startsWith("Bearer "))
            return null;
        return headerToken.substring(7);//从7开始截取，前7位是“Bearer空格”
    }

    public UserDetails toUser(DecodedJWT jwt){
        Map<String, Claim> claims = jwt.getClaims();
        return User
                .withUsername(claims.get("name").asString())
                .password("123")
                .authorities(claims.get("authorities").asArray(String.class))
                .build();
    }

    public Integer toID(DecodedJWT jwt){//解析jwt里的用户ID
        Map<String, Claim> claims = jwt.getClaims();
        return claims.get("id").asInt();
    }




}
