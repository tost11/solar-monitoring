package de.tostsoft.solarmonitoring.lib.repository;

import de.tostsoft.solarmonitoring.lib.model.Captcha;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.validation.annotation.Validated;

@Validated
public interface CaptchaRepository extends MongoRepository<Captcha,String> {

    Captcha getCaptchaByBase64Image(@NotNull String base64);

    long countByCreatedAtBefore(@NotNull Long timestamp);
}
