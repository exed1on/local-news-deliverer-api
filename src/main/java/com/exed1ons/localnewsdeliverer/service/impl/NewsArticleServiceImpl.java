package com.exed1ons.localnewsdeliverer.service.impl;

import com.exed1ons.localnewsdeliverer.data.DataStore;
import com.exed1ons.localnewsdeliverer.domain.City;
import com.exed1ons.localnewsdeliverer.domain.NewsArticle;
import com.exed1ons.localnewsdeliverer.service.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NewsArticleServiceImpl implements NewsArticleService {

    private final DataStore dataStore;

    public NewsArticleServiceImpl(DataStore dataStore) {
        this.dataStore = dataStore;
        dataStore.init();
    }

    @Override
    public List<NewsArticle> getAllNewsArticles() {
        return dataStore.getNewsArticles();
    }

    @Override
    public List<City> getAllCities() {
        return dataStore.getCities();
    }

    public List<City> searchCities(String name) {
        List<City> allCities = dataStore.getCities();

        return allCities.stream()
                .filter(city -> name == null || city.getName().toLowerCase().contains(name.toLowerCase()) ||
                        city.getStateName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public List<NewsArticle> searchNewsByCity(String cityName, String stateName, boolean includeGlobalNews) {
        List<NewsArticle> allArticles = dataStore.getNewsArticles();

        return allArticles.stream()
                .filter(article ->
                        article.getCities().stream().anyMatch(city ->
                                city.getName().equalsIgnoreCase(cityName) &&
                                        city.getStateName().equalsIgnoreCase(stateName)
                        ) || (includeGlobalNews && !article.isLocal())
                )
                .collect(Collectors.toList());
    }
}
