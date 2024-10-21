package com.exed1ons.localnewsdeliverer.domain;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class City {
    Long id;
    String name;
    String stateName;
    String stateCode;
}
