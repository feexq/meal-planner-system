package com.feex.mealplannersystem.service;

import com.feex.mealplannersystem.dto.delivery.DeliveryLocationDto;
import java.util.List;

public interface DeliveryService {
    List<DeliveryLocationDto> searchCities(String cityName);
    List<DeliveryLocationDto> getWarehouses(String cityRef);
}