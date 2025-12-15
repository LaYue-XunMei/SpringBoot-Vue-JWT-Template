package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.vo.request.ConfirmResetVO;
import com.example.entity.vo.request.EmailRegisterVO;
import com.example.entity.vo.request.EmailResetVO;
import com.example.mapper.AccountMapper;
import com.example.service.AccountService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.Resource;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
/**
 * 账户信息处理相关服务
 */
@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    //验证邮件发送冷却时间限制，秒为单位
    @Value("${spring.web.verify.mail-limit}")
    int verifyLimit;

    @Resource
    FlowUtils flowUtils;

    @Resource
    AmqpTemplate amqpTemplate;

//    @Resource
//    private RedisTemplate<String, String> redisTemplate;  // 改用 RedisTemplate<String, String>

    @Resource(name = "stringRedisTemplate")
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    /**
     * 生成注册验证码存入Redis中，并将邮件发送请求提交到消息队列等待发送
     * @param type 类型
     * @param email 邮件地址
     * @param ip 请求IP地址
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String registerEmailVerifyCode(String type, String email, String ip) {
        synchronized (ip.intern()){//防止同一时间被多次调用，加锁
            if(!this.verifyLimit(ip)){//没有通过验证则发送
                return "请求频繁，请稍后再发送";
            }
            //如果不在冷却期内，通过验证
            Random random = new Random();
            int code = random.nextInt(899999) + 10000;//6位验证码
            Map<String, Object> data = Map.of("type",type,"email",email,"code",code);
            amqpTemplate.convertAndSend(Const.MQ_MAIL,data);
            stringRedisTemplate.opsForValue()
                    .set(Const.VERITY_EMAIL_DATA+email,String.valueOf(code),3, TimeUnit.MINUTES);//3分钟有效
            return null;
        }
    }

    /**
     * 邮件验证码注册账号操作，需要检查验证码是否正确以及邮箱、用户名是否存在重名
     * @param vo 注册基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String registerEmailAccount(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String code = this.getEmailVerifyCode(email);//直接拿存在redis中的验证码
        if(code == null) return "请先获取验证码。";
        if(!code.equals(vo.getCode())) return "验证码错误，请重新输入。";
        if(this.existAccountByEmail(email)) return "该邮箱已被注册。";

        String username = vo.getUsername();
        if(this.existAccountByUsername(username)) return "该用户名已被注册。";

        String password = passwordEncoder.encode(vo.getPassword());
        Account account = new Account(null,username,password,email,Const.ROLE_DEFAULT,new Date());//默认为user
        if (this.save(account)) {
            this.deleteEmailVerifyCode(email);//删除验证码
            return null;
        }else {
            return "注册失败，内部错误，请联系管理员。";
        }
    }

    /**
     * 邮件验证码重置密码操作，需要检查验证码是否正确
     * @param vo 重置基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetEmailAccountPassword(EmailResetVO vo) {
        String email = vo.getEmail();
        String verifyCode = this.resetConfirm(new ConfirmResetVO(email,vo.getCode()));
        if(verifyCode != null) return verifyCode;
        String password = passwordEncoder.encode(vo.getPassword());
        boolean update = this.update()
                .eq("email", email)
                .set("password", password)
                .update();
        if(update) {
            this.deleteEmailVerifyCode(email);//删除验证码
        }
        return update ? null : "更新失败，请联系管理员";
    }

    /**
     * 重置密码确认操作，验证验证码是否正确
     * @param info 验证基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetConfirm(ConfirmResetVO info) {
        String email = info.getEmail();
        String code = stringRedisTemplate.opsForValue().get(Const.VERITY_EMAIL_DATA+email);
        if(code == null) return "请先获取验证码。";
        if(!code.equals(info.getCode())) return "验证码错误，请重新输入。";
        return null;
    }

    /**
     * 通过用户名或邮件地址查找用户
     * @param text 用户名或邮件
     * @return 账户实体
     */
    public Account findAccountByNameOrEmail(String text){
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();
    }


    /**
     * 获取Redis中存储的邮件验证码
     * @param email 电邮
     * @return 验证码
     */
    private String getEmailVerifyCode(String email){
        String key = Const.VERITY_EMAIL_DATA + email;
        return stringRedisTemplate.opsForValue().get(key);
    }

    /**
     * 移除Redis中存储的邮件验证码
     * @param email 电邮
     */
    private void deleteEmailVerifyCode(String email){
        String key = Const.VERITY_EMAIL_DATA + email;
        stringRedisTemplate.delete(key);
    }

    /**
     * 针对IP地址进行邮件验证码获取限流
     * @param ip 地址
     * @return 是否通过验证
     */
    private boolean verifyLimit(String ip){
        String key = Const.VERIFY_EMAIL_LIMIT+ip;
        return flowUtils.limitOneChick(key,verifyLimit);//限制60秒内只能请求一次
    }

    /**
     * 查询指定邮箱的用户是否已经存在
     * @param email 邮箱
     * @return 是否存在
     */
    private boolean existAccountByEmail(String email){//通过邮件判断用户是否存在
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }
    /**
     * 查询指定用户名的用户是否已经存在
     * @param username 用户名
     * @return 是否存在
     */
    private boolean existAccountByUsername(String username){//通过邮件判断用户是否存在
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username",username));
    }
}
