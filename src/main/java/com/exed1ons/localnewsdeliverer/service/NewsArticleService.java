package com.exed1ons.localnewsdeliverer.service;

import com.exed1ons.localnewsdeliverer.domain.City;
import com.exed1ons.localnewsdeliverer.domain.NewsArticle;

import java.util.List;

public interface NewsArticleService {
    List<NewsArticle> getAllNewsArticles();
    List<City> getAllCities();
    List<City> searchCities(String name);
    List<NewsArticle> searchNewsByCity(String cityName, String stateName, boolean includeGlobalNews);
}
