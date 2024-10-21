package com.exed1ons.localnewsdeliverer.domain;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewsArticle {
    String title;
    String description;
    String url;
    String urlToImage;
    boolean isLocal;
    List<City> cities;
}
