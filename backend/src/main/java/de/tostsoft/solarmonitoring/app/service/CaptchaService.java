package de.tostsoft.solarmonitoring.app.service;

import cn.apiclub.captcha.backgrounds.GradiatedBackgroundProducer;
import cn.apiclub.captcha.noise.CurvedLineNoiseProducer;
import cn.apiclub.captcha.text.producer.DefaultTextProducer;
import cn.apiclub.captcha.text.renderer.DefaultWordRenderer;
import de.tostsoft.solarmonitoring.lib.model.Captcha;
import de.tostsoft.solarmonitoring.lib.repository.CaptchaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;

@Service
public class CaptchaService
{
    @Autowired
    private CaptchaRepository captchaRepository;

    public Captcha generageCaptcha(){
        var  captcha = createCaptcha(200,50);
        Captcha dbCaptcha = Captcha.builder()
                .base64Image(encodeBase64(captcha))
                .text(captcha.getAnswer())
                .createdAt(Instant.now().toEpochMilli())
                .build();

        return captchaRepository.save(dbCaptcha);
    }

    private static cn.apiclub.captcha.Captcha createCaptcha(int width, int height) {
        return new cn.apiclub.captcha.Captcha.Builder(width, height)
                .addBackground(new GradiatedBackgroundProducer())
                .addText(new DefaultTextProducer(), new DefaultWordRenderer())
                .addNoise(new CurvedLineNoiseProducer()).build();
    }

    public static String encodeBase64(cn.apiclub.captcha.Captcha captcha) {
        String image= null;
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(captcha.getImage(), "png", outputStream);
            byte[] arr = Base64.getEncoder().encode(outputStream.toByteArray());
            image = new String(arr);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public Captcha getCaptchaByByBase64Image(String base64Image) {
        return captchaRepository.getCaptchaByBase64Image(base64Image);
    }

    public void deleteCaptcha(Captcha captcha) {
        captchaRepository.deleteById(captcha.getId());
    }
}
