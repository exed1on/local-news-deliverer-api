package com.exed1ons.localnewsdeliverer.controller;

import com.exed1ons.localnewsdeliverer.domain.City;
import com.exed1ons.localnewsdeliverer.domain.NewsArticle;
import com.exed1ons.localnewsdeliverer.service.NewsArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@CrossOrigin(origins = "https://local-news-deliverer.onrender.com/")
public class CityController {
    private final NewsArticleService newsArticleService;

    public CityController(NewsArticleService newsArticleService) {
        this.newsArticleService = newsArticleService;
    }

    @GetMapping("/cities")
    public ResponseEntity<List<City>> getCities() {
        return ResponseEntity.ok(newsArticleService.getAllCities());
    }

    @GetMapping("/news")
    public ResponseEntity<List<NewsArticle>> getNews() {
        return ResponseEntity.ok(newsArticleService.getAllNewsArticles());
    }

    @GetMapping("cities/search")
    public ResponseEntity<List<City>> searchCities(
            @RequestParam String name) {
        List<City> cities = newsArticleService.searchCities(name);
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/news/search")
    public ResponseEntity<List<NewsArticle>> getBody(
            @RequestParam String cityName,
            @RequestParam String stateName,
            @RequestParam boolean includeGlobalNews) {
        return ResponseEntity.ok(newsArticleService.searchNewsByCity(cityName, stateName, includeGlobalNews));
    }

    @GetMapping("/keep")
    public ResponseEntity<String> keepAlive() {
        return ResponseEntity.ok("Keep");
    }
}
