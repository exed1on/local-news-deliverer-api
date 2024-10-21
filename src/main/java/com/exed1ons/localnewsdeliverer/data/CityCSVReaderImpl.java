package com.exed1ons.localnewsdeliverer.data;

import com.exed1ons.localnewsdeliverer.domain.City;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CityCSVReaderImpl implements CityCSVReader {

    private static final Logger logger = LoggerFactory.getLogger(CityCSVReaderImpl.class);

    public List<City> readCities(String filePath) {
        var lines = readFromFile(filePath);
        if (lines.isEmpty()) {
            logger.warn("No lines found in the CSV file.");
            return Collections.emptyList();
        }

        String headerLine = lines.remove(0);
        Map<String, Integer> headerMap = mapHeader(headerLine);

        return lines
                .stream()
                .map(line -> mapToCity(line, headerMap))
                .collect(Collectors.toList());
    }

    private City mapToCity(String line, Map<String, Integer> headerMap) {
        var cityData = parseCSVLine(line);

        Long id = Long.parseLong(cityData[headerMap.get("id")]);
        String name = cityData[headerMap.get("city")];
        String stateName = cityData[headerMap.get("state_name")];
        String stateCode = cityData[headerMap.get("state_id")];

        return City.builder()
                .id(id)
                .name(name)
                .stateName(stateName)
                .stateCode(stateCode)
                .build();
    }

    protected List<String> readFromFile(String pathOfFile) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(pathOfFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            logger.error("Error while reading CSV file: " + e.getMessage());
        }
        return lines;
    }

    private String[] parseCSVLine(String line) {
        return line.replace("\"", "").split(",");
    }

    private Map<String, Integer> mapHeader(String headerLine) {
        String[] headers = parseCSVLine(headerLine);
        Map<String, Integer> headerMap = new HashMap<>();

        for (int i = 0; i < headers.length; i++) {
            headerMap.put(headers[i], i);
        }
        return headerMap;
    }
}
