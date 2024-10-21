package com.exed1ons.localnewsdeliverer.data;

import com.exed1ons.localnewsdeliverer.domain.City;

import java.util.List;

public interface CityCSVReader {
    List<City> readCities(String filePath);
}
