package com.liverpool.imageValidator.service;

import com.liverpool.imageValidator.models.SkusToDeleteDTO;
import com.liverpool.imageValidator.utils.RestTemplateUrl;
import com.liverpool.imageValidator.utils.Util;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service
@ConfigurationProperties(prefix = "url")
public class SkuExportService {

    private static final Path BASE_DIR_EXECUTION = Paths.get("files", "execution", "skuList");
    private static final Path BASE_DIR_DELETE = Paths.get("files", "deleteDB", "skuList");
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");



    private String URL_BT;
    private String URL_SL;
    private String URL_INVOKE;
    private String user_name;
    private String password;

    public List<String> exportSkuLists(List<SkusToDeleteDTO> skusForDelete) {

        ResponseEntity<String> onlineResponse = null;
        ResponseEntity<String> btvResponse = null;

        if (skusForDelete == null || skusForDelete.isEmpty()) {
            log.info("No hay SKUs para procesar. Limpio archivos previos en deleteDB y retorno.");

            // Borra archivos previos en DIR_DELETE (BT y SL)
            try {
                Path dirBt = BASE_DIR_DELETE.resolve("BT");
                Path dirSl = BASE_DIR_DELETE.resolve("SL");

                // Archivos que queremos eliminar si existen
                Path fileDelBt = dirBt.resolve("BigTicket_List_Delete.txt");
                Path fileDelSl = dirSl.resolve("SoftLine_List_Delete.txt");

                // Borra si existen
                Files.deleteIfExists(fileDelBt);
                Files.deleteIfExists(fileDelSl);

                log.info("Archivos previos de deleteDB eliminados (si existían).");
            } catch (IOException e) {
                log.warn("No fue posible eliminar archivos previos en deleteDB: {}", e.getMessage(), e);
            }

            return Collections.emptyList();
        }

        // Separación inicial
        List<String> btList = skusForDelete.stream()
                .filter(s -> s.getProductType() == 0)
                .map(SkusToDeleteDTO::getSkuId)
                .collect(Collectors.toList());

        List<String> slList = skusForDelete.stream()
                .filter(s -> s.getProductType() == 1)
                .map(SkusToDeleteDTO::getSkuId)
                .collect(Collectors.toList());


        //logic to call methods
        if (!btList.isEmpty()) {
            log.info("inside of bigTicket response " + Util.makeUrl(URL_BT, btList));
            btvResponse = RestTemplateUrl.callMethod(Util.makeUrl(URL_BT, btList) + "&change", user_name, password);
        }


        if (!slList.isEmpty()) {
            log.info("inside of Online response " + Util.makeUrl(URL_SL, slList));
            onlineResponse = RestTemplateUrl.callMethod(Util.makeUrl(URL_SL, slList) + "&change", user_name, password);
        }

        if (onlineResponse != null || btvResponse != null) {
            if ((onlineResponse.getStatusCode().equals(HttpStatus.OK) || btvResponse.getStatusCode().equals(HttpStatus.OK)) ||
                    (onlineResponse.getStatusCode().equals(HttpStatus.OK) && btvResponse.getStatusCode().equals(HttpStatus.OK))) {
                ResponseEntity<String> invokeResponse = RestTemplateUrl.callMethod(URL_INVOKE, user_name, password);
                if (!invokeResponse.getStatusCode().is2xxSuccessful()) {
                    log.info("Error to pushing values " + invokeResponse.getStatusCode());
                } else {
                    log.info("Pushing values successfully " + invokeResponse.getStatusCode());
                }

            }
        }

        List<String> othersList = skusForDelete.stream()
                .filter(s -> s.getProductType() != 0 && s.getProductType() != 1)
                .map(SkusToDeleteDTO::getSkuId)
                .collect(Collectors.toList());

        log.info("--Total Productos de BD");
        log.info("Productos BT: {}", btList.size());
        log.info("Productos SL: {}", slList.size());
        log.info("Otros Productos: {}", othersList.size());

        // Detección y movimiento de duplicados entre BT y SL hacia Others
        Set<String> btSet = new HashSet<>(btList);
        Set<String> slSet = new HashSet<>(slList);
        Set<String> othersSet = new HashSet<>(othersList);

        Set<String> duplicated = new HashSet<>(btSet);
        Set<String> duplicatedOthers = new HashSet<>(othersList);
        duplicated.retainAll(slSet);

        log.info("Productos duplicados (existe en BT y SL): {}", duplicated.size());

        othersSet.addAll(duplicated);
        btSet.removeAll(duplicated);
        btSet.removeAll(duplicatedOthers);
        slSet.removeAll(duplicated);
        slSet.removeAll(duplicatedOthers);

        List<String> finalBt = new ArrayList<>(btSet);
        List<String> finalSl = new ArrayList<>(slSet);
        List<String> finalOthers = new ArrayList<>(othersSet);

        Collections.sort(finalBt);
        Collections.sort(finalSl);
        Collections.sort(finalOthers);

        log.info("-- Totales después de mover duplicados a others");
        log.info("Productos BT en archivo: {}", finalBt.size());
        log.info("Productos SL en archivo: {}", finalSl.size());
        log.info("Otros Productos en archivo: {}", finalOthers.size());

        LocalDateTime now = LocalDateTime.now();
        String ts = now.format(TS_FMT);

        try {
            // Rutas de execution (histórico)
            Path dirBT = BASE_DIR_EXECUTION.resolve("BT");
            Path dirSL = BASE_DIR_EXECUTION.resolve("SL");
            Path dirOthers = BASE_DIR_EXECUTION.resolve("Others");

            // Rutas para delete
            Path dirDelBT = BASE_DIR_DELETE.resolve("BT");
            Path dirDelSL = BASE_DIR_DELETE.resolve("SL");

            // Crear directorios si no existen
            Files.createDirectories(dirBT);
            Files.createDirectories(dirSL);
            Files.createDirectories(dirOthers);
            Files.createDirectories(dirDelBT);
            Files.createDirectories(dirDelSL);

            // Archivos de histórico (con timestamp)
            Path fileBt = dirBT.resolve("BigTicket_List_" + ts + ".txt");
            Path fileSl = dirSL.resolve("SoftLine_List_" + ts + ".txt");
            Path fileOthers = dirOthers.resolve("Others_List_" + ts + ".txt");

            // Archivos para delete
            Path fileDelBt = dirDelBT.resolve("BigTicket_List_Delete.txt");
            Path fileDelSl = dirDelSL.resolve("SoftLine_List_Delete.txt");

            // Eliminar archivos previos de delete
            deleteIfExistsQuietly(fileDelBt);
            deleteIfExistsQuietly(fileDelSl);

            // Escribir archivos históricos
            writeListToFile(finalBt, fileBt);
            writeListToFile(finalSl, fileSl);
            writeListToFile(finalOthers, fileOthers);

            // Escribir archivos de delete
            writeListToFile(finalBt, fileDelBt);
            writeListToFile(finalSl, fileDelSl);

            log.info("Archivos escritos correctamente:");
            log.info("BT -> {}", fileBt.toAbsolutePath());
            log.info("SL -> {}", fileSl.toAbsolutePath());
            log.info("Others -> {}", fileOthers.toAbsolutePath());
            log.info("BT delete -> {}", fileDelBt.toAbsolutePath());
            log.info("SL delete -> {}", fileDelSl.toAbsolutePath());

            return finalSl;
        } catch (IOException e) {
            log.error("Error al crear directorios o escribir archivos: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private void writeListToFile(List<String> list, Path path) throws IOException {
        Files.createDirectories(path.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            for (String sku : list) {
                writer.write(sku);
                writer.newLine();
            }
            writer.flush();
        }
    }

    private void deleteIfExistsQuietly(Path path) {
        try {
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("Archivo previo eliminado: {}", path.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("No se pudo eliminar archivo previo {} : {}", path.toAbsolutePath(), e.getMessage());
            // no throw; queremos continuar y sobreescribir si es posible
        }
    }

}
