package com.exed1ons.localnewsdeliverer.data;

import com.exed1ons.localnewsdeliverer.domain.City;
import com.exed1ons.localnewsdeliverer.domain.NewsArticle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileDataStore implements DataStore {

    private static final Logger logger = LoggerFactory.getLogger(FileDataStore.class);

    private final String cityFilePath;
    private final String newsFilePath;

    private List<City> cities;
    private List<NewsArticle> newsArticles;

    private final NewsArticleReader newsArticleReader;
    private final CityCSVReader cityCSVReader;

    public FileDataStore(@Value("${city.file.path}") String cityFilePath,
                         @Value("${news.file.path}") String newsFilePath,
                         NewsArticleReader newsArticleReader,
                         CityCSVReader cityCSVReader) {
        this.cityFilePath = cityFilePath;
        this.newsFilePath = newsFilePath;
        this.newsArticleReader = newsArticleReader;
        this.cityCSVReader = cityCSVReader;
    }

    @Override
    public void init() {
        cities = cityCSVReader.readCities(cityFilePath);
        newsArticles = newsArticleReader.readNews(newsFilePath, cities);
    }

    @Override
    public List<City> getCities() {
        return cities;
    }

    @Override
    public List<NewsArticle> getNewsArticles() {
        return newsArticles;
    }
}
