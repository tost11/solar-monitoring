package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Captcha;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CaptchaRepository extends MongoRepository<Captcha,String> {

    Captcha getCaptchaByBase64Image(String base64);

    Captcha getCaptchaByCreatedAtBefore(Long timestamp);
}
