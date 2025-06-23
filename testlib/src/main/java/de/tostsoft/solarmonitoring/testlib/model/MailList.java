package de.tostsoft.solarmonitoring.testlib.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MailList {

    int size;
    List<Mail> mailList = new ArrayList<>();
}
