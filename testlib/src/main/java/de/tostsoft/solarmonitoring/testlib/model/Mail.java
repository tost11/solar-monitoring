package de.tostsoft.solarmonitoring.testlib.model;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mail {
    String subject;
    String content;
    String from;
    String to;
}
