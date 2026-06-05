package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {

    @Resource
    FlowUtils flowUtils;

    @Resource
    AmqpTemplate amqpTemplate;

    @Resource(name = "stringRedisTemplate")
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PasswordEncoder passwordEncoder;

    /**
     * 从数据库中通过用户名或邮箱查找用户详细信息
     * @param username 用户名
     * @return 用户详细信息
     * @throws UsernameNotFoundException 如果用户未找到则抛出此异常
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if(account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    /**
     * 邮件验证码注册账号操作，需要检查验证码是否正确以及邮箱、用户名是否存在重名
     * @param vo 注册基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String registerEmailAccount(EmailRegisterVO vo) {
        String email = vo.getEmail();
        String username = vo.getUsername();
        String key = Const.VERITY_EMAIL_DATA+email;
        String code = stringRedisTemplate.opsForValue().get(key);//直接拿存在redis中的验证码
        if(code == null) return "请先获取验证码。";
        if(!code.equals(vo.getCode())) return "验证码错误，请重新输入。";
        if(this.existAccountByEmail(email)) return "该邮箱已被注册。";
        if(this.existAccountByUsername(username)) return "该用户名已被注册。";

        String password = passwordEncoder.encode(vo.getPassword());//存入数据库的是加密后的密码
        Account account = new Account(null,username,password,email,"user",new Date());//默认为身份user，id数据库设置自动递增
        if (this.save(account)) {
            stringRedisTemplate.delete(key);//成功注册后立即删除验证码
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
        String verify = this.resetConfirm(new ConfirmResetVO(email,vo.getCode()));
        if(verify != null) return verify;
        //没有问题说明验证码正确
        String password = passwordEncoder.encode(vo.getPassword());
        boolean update = this.update()
                .eq("email", email)
                .set("password", password)
                .update();
        if(update) {//更新成功
            stringRedisTemplate.delete(Const.VERITY_EMAIL_DATA+email);//删除验证码
        }
        return null;
    }

    /**
     * 重置密码确认操作，验证验证码是否正确
     * @param vo 验证基本信息
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String resetConfirm(ConfirmResetVO vo) {
        String email = vo.getEmail();
        String code = stringRedisTemplate.opsForValue().get(Const.VERITY_EMAIL_DATA + email);
        if(code == null) return "请先获取验证码";
        if(!code.equals(vo.getCode())) return "验证码错误，请重新输入。";
        return null;
    }

    public Account findAccountByNameOrEmail(String text){
        return this.query()
                .eq("username", text).or()
                .eq("email",text)
                .one();
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
            int code = random.nextInt(899999) + 100000;//6位验证码
            Map<String, Object> data = Map.of("type",type,"email",email,"code",code);
            amqpTemplate.convertAndSend("mail",data);
            stringRedisTemplate.opsForValue()//注册时要验证验证码是否输入正确，所以需要保存
                    .set(Const.VERITY_EMAIL_DATA+email,String.valueOf(code),3, TimeUnit.MINUTES);//3分钟有效
            return null;
        }
    }

    private boolean existAccountByEmail(String email){//通过邮件判断用户是否存在
        return this.baseMapper.exists(Wrappers.<Account>query().eq("email",email));
    }

    private boolean existAccountByUsername(String username){//通过邮件判断用户是否存在
        return this.baseMapper.exists(Wrappers.<Account>query().eq("username",username));
    }


    /**
     * 针对IP地址进行邮件验证码获取限流
     * @param ip 地址
     * @return 是否通过验证
     */
    private boolean verifyLimit(String ip){
        String key = Const.VERIFY_EMAIL_LIMIT+ip;
        return flowUtils.limitOnceChick(key,60);//限制60秒内只能请求一次
    }
}
