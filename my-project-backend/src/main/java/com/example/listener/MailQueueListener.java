package com.example.listener;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener(queues = "mail")
public class MailQueueListener {

    @Resource
    JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    String username;//发件人地址

    @RabbitHandler
    public void sendMailMessage(Map<String, Object> data){
        String email = data.get("email").toString();
        Integer code = (Integer) data.get("code");
        String type = (String) data.get("type");
        SimpleMailMessage message = switch (type){
            case "register" ->
                    createMessage("欢迎注册",
                            "您的邮件注册验证码为："+code+"，有效时间3分钟，请勿向他人泄露验证码。", email);
            case "reset" ->
                    createMessage("密码重置","您正在重置密码操作，您的验证码为："+code+"，有效时间3分钟，请勿向他人泄露验证码。",email);
            default -> null;
        };
        if (message == null) return;
        mailSender.send(message);
    }

    private SimpleMailMessage createMessage(String title, String content, String email){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return message;
    }

}
