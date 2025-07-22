package de.tostsoft.solarmonitoring.lib.service;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class MailService {

    @Autowired(required = false)
    private JavaMailSender javaMailSender;

    private Logger logger = LoggerFactory.getLogger(MailService.class);

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Value("${spring.mail.username:}")
    private String mailUser;

    public void sendMail(String toEmail, String subject, String message) {
        if(javaMailSender == null || StringUtils.isEmpty(mailUser)) {
            logger.warn("No mail send because mail not configured");
            return;
        }

        executor.execute(()-> {
            try {
                var mailMessage = new SimpleMailMessage();

                mailMessage.setTo(toEmail);
                mailMessage.setSubject(subject);

                mailMessage.setText(message);
                mailMessage.setFrom(mailUser);

                logger.info("try send mail to " + toEmail);

                javaMailSender.send(mailMessage);

                logger.info("send mail to " + toEmail);
            } catch (Exception exception) {
                logger.error("Error while sending mail", exception);
            }
        });
    }


}
