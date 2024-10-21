package com.exed1ons.localnewsdeliverer.data;

import com.exed1ons.localnewsdeliverer.domain.City;
import com.exed1ons.localnewsdeliverer.domain.NewsArticle;

import java.util.List;

public interface DataStore {
    void init();

    List<City> getCities();

    List<NewsArticle> getNewsArticles();

}
