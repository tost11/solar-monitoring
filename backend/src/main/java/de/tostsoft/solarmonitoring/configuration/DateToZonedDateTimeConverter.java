package de.tostsoft.solarmonitoring.configuration;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

@ReadingConverter
public class DateToZonedDateTimeConverter implements Converter<Date, ZonedDateTime> {
  @Override
  public ZonedDateTime convert(Date source) {
    if (source == null) {
      return null;
    }
    return ZonedDateTime.ofInstant(source.toInstant(), ZoneId.of("UTC"));
  }
}