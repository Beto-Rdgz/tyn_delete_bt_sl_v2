package com.liverpool.imageValidator;

import com.liverpool.imageValidator.models.SkusToDeleteDTO;
import com.liverpool.imageValidator.service.DataOracleServiceImpl;
import com.liverpool.imageValidator.service.InventoryCleanupService;
import com.liverpool.imageValidator.service.SkuExportService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@SpringBootApplication
@EnableAutoConfiguration
public class DeletteDuplicatesApplication implements CommandLineRunner {

    @Autowired
    @Qualifier("iuoJdbcTemplate")
    private JdbcTemplate iuoJdbcTemplate;

    @Autowired
    @Qualifier("atgJdbcTemplate")
    private JdbcTemplate atgJdbcTemplate;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private DataOracleServiceImpl dataOracleService;

    @Autowired
    private SkuExportService skuExportService;

    @Autowired
    private InventoryCleanupService inventoryCleanupService;

    public static void main(String[] args) {
        ApplicationContext context = SpringApplication.run(DeletteDuplicatesApplication.class, args);
        log.info("Finalizo proceso de Eliminación de duplicados");
        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            if (args == null || args.length == 0) {
                log.error("No se proporcionaron argumentos. Debes usar:");
                log.error("  java -jar app.jar FULL");
                log.error("  o");
                log.error("  java -jar app.jar sku1,sku2,sku3");
                return;
            }
            List<String> skusList;

            if ("FULL".equalsIgnoreCase(args[0])){
                // Obtenemos todos los sku de iuo
                log.info("Modo FULL activado: obteniendo todos los SKUs de IUO...");
                skusList = this.dataOracleService.getIuoSkus();
                log.info("Número de SKUs obtenidos de IUO: {}", skusList.size());
            } else {
                // usamos sku puestos manualmente
                String input = args[0];
                List<String> skusArgs = Arrays.stream(input.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());

                if (skusArgs.isEmpty()) {
                    log.error("Los argumentos no contienen SKUs válidos. Ejemplo correcto:");
                    log.error("  java -jar app.jar sku1,sku2,sku3");
                    return;
                }

                log.info("SKUs proporcionados manualmente: {}", skusArgs.size());
                skusList = this.dataOracleService.getIuoSkusManual(skusArgs);
                log.info("Número de SKUs encontrados en IUO: {}", skusList.size());
            }

            //Obtener candidatos a apagarse
            List<SkusToDeleteDTO> skusForDelete = this.dataOracleService.getValidSkusToRemove(skusList);
            log.info("Número de SKUs candidatos a apagar: {}", skusForDelete.size());

            // separamos los skus en listas y las imprimimos en la ruta
            this.skuExportService.exportSkuLists(skusForDelete);

            // Eliminamos SKUs de las respectivas tablas BT y SL en IUO
            this.dataOracleService.deleteSkusFromFileByType("BT");
            this.dataOracleService.deleteSkusFromFileByType("SL");

        } catch (Exception e) {
            log.error("Error al obtener SKUs: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
