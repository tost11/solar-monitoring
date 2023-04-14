package de.tostsoft.solarmonitoring.configuration;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

@Configuration
public class MongoCustomConverterConfig {

  @Bean
  public MongoCustomConversions mongoCustomConversions(){
    List<Converter<?,?>> converters = new ArrayList<>();
    converters.add(new DateToZonedDateTimeConverter());
    converters.add(new ZonedDateTimeToDateConverter());
    return new MongoCustomConversions(converters);
  }
}