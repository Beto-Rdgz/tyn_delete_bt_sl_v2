package com.liverpool.imageValidator.service;

import com.liverpool.imageValidator.models.SkusToDeleteDTO;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface DataOracleService {

    List<String> getIuoSkus();

    List<SkusToDeleteDTO> getValidSkusToRemove(List<String> skusList);

    List<String> getIuoSkusManual(List<String> skusArgs);

}
