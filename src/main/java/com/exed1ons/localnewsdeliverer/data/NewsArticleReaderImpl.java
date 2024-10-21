package com.exed1ons.localnewsdeliverer.data;

import com.exed1ons.localnewsdeliverer.domain.City;
import com.exed1ons.localnewsdeliverer.domain.NewsArticle;
import com.exed1ons.localnewsdeliverer.service.LLMRequestCityService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewsArticleReaderImpl implements NewsArticleReader {

    private static final Logger logger = LoggerFactory.getLogger(NewsArticleReaderImpl.class);

    private final ObjectMapper objectMapper;
    private final LLMRequestCityService llmRequestCityService;

    private List<City> cities;

    public NewsArticleReaderImpl(LLMRequestCityService llmRequestCityService) {
        this.objectMapper = new ObjectMapper();
        this.llmRequestCityService = llmRequestCityService;
    }

    public List<NewsArticle> readNews(String filePath, List<City> cities) {
        this.cities = cities;

        List<NewsArticle> articles = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.debug("Reading line: " + line);
                JsonNode jsonNode = objectMapper.readTree(line);
                List<NewsArticle> parsedArticles = processJsonNode(jsonNode);

                for (NewsArticle article : parsedArticles) {
                    try {
                        enrichArticle(article);
                        if (article.getDescription() == null || article.getDescription().isBlank() ||
                                article.getDescription().equals("No description available")) {
                            logger.warn("Skipping article without a valid description");
                            continue;
                        }
                        if (articles.stream()
                                .map(NewsArticle::getUrl)
                                .collect(Collectors.toSet())
                                .contains(article.getUrl())) {
                            logger.warn("Skipping article with duplicate URL: " + article.getUrl());
                            continue;
                        }
                        articles.add(article);
                    }
                    catch (Exception e) {
                        logger.error("Error while enriching article: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error while reading JSON file: " + e.getMessage());
        }

        return articles;
    }

    private List<NewsArticle> processJsonNode(JsonNode jsonNode) {
        List<NewsArticle> articles = new ArrayList<>();

        jsonNode.fields().forEachRemaining(entry -> {
            String sourceUrl = entry.getKey();
            JsonNode sourceData = entry.getValue();

            JsonNode links = sourceData.get("links");
            if (links != null && links.isArray()) {
                for (JsonNode link : links) {
                    String url = link.get("link").asText();
                    String snippet = link.get("snippet").asText();

                    if (!snippet.isEmpty()) {
                        logger.debug("Snippet: " + snippet);
                        if (isValidURL(url)) {
                            logger.debug("Creating article with url: " + url);
                            NewsArticle article = NewsArticle.builder()
                                    .url(url)
                                    .build();
                            articles.add(article);
                        } else {
                            logger.warn("Skipping URL due to HTTP error: " + url);
                        }
                    }
                }
            } else {
                logger.warn("No links array found for source: " + sourceUrl);
            }
        });

        return articles;
    }

    private boolean isValidURL(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            return (responseCode >= 200 && responseCode < 400);
        } catch (IOException e) {
            logger.error("Error checking URL: " + urlString + " - " + e.getMessage());
            return false;
        }
    }

    private void enrichArticle(NewsArticle article) {

        String url = article.getUrl();


        if (url == null || url.isEmpty() || url.equals("#")) {
            logger.warn("Skipping article with invalid URL: " + url);
            return;
        }
        logger.info("Enriching article with URL: " + url);

        try {
            Document doc = Jsoup.connect(article.getUrl()).get();
            String description = getDescription(doc);
            if (description.isEmpty()) {
                logger.debug("Skipping article without a valid description");
                return;
            }
            if(getArticleBody(doc).isBlank()) {
                logger.debug("Skipping article without a valid body");
                return;
            }
            article.setDescription(description);
            article.setTitle(getTitle(doc));
            article.setUrlToImage(getUrlToImage(doc));

            List<City> cities = getCities(doc);
            article.setLocal(!cities.isEmpty());
            article.setCities(cities);

        } catch (IOException e) {
            logger.error("Error while fetching additional data for article: " + article.getUrl() + " - " + e.getMessage());
        }
    }

    private String getDescription(Document doc) {
        Element metaDescription = doc.selectFirst("meta[property=og:description]");
        return metaDescription != null ? metaDescription.attr("content") : "No description available";
    }

    private String getTitle(Document doc) {
        Element metaTitle = doc.selectFirst("meta[property=og:title]");
        return metaTitle != null ? metaTitle.attr("content") : "No title available";
    }

    private String getUrlToImage(Document doc) {
        Element metaImage = doc.selectFirst("meta[property=og:image]");
        return metaImage != null ? metaImage.attr("content") : "No image URL available";
    }

    private List<City> getCities(Document doc) {
        List<String> cityNames = llmRequestCityService.requestCity(getArticleBody(doc));

        if (cityNames == null || cityNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<City> connectedCities = new ArrayList<>();
        for (String cityInfo : cityNames) {
            String[] parts = cityInfo.split(",");
            if (parts.length != 2) {
                continue;
            }

            String trimmedCityName = parts[0].trim();
            String stateCode = parts[1].trim();

            City city = findCityByName(trimmedCityName, stateCode);
            if (city != null) {
                connectedCities.add(city);
            }
        }

        return connectedCities;
    }

    private String getArticleBody(Document doc) {
        doc.select("menu, header, nav, footer").remove();
        return doc.body().text();
    }

    private City findCityByName(String cityName, String stateCode) {
        return cities.stream()
                .filter(city -> city.getName().equalsIgnoreCase(cityName) && city.getStateCode().equalsIgnoreCase(stateCode))
                .findFirst()
                .orElse(null);
    }
}
