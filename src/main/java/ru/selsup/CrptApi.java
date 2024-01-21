package ru.selsup;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import okhttp3.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Класс для взаимодействия с API Честного знака.
 * Поддерживает ограничение на количество запросов к API в заданном временном интервале и обеспечивает потокобезопасность.
 */
public class CrptApi {
    public static void main(String[] args) {
        CrptApi crptApi = new CrptApi(TimeUnit.SECONDS, 1);
        Document document = new Document();
        String signature = "Подпись";
        crptApi.createDocument(document, signature);
    }
    private static final String API_URL = "https://ismp.crpt.ru/api/v3/lk/documents/create";
    private final OkHttpClient client;
    private final RateLimiter rateLimiter;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.client = new OkHttpClient();
        this.rateLimiter = new RateLimiter(requestLimit, timeUnit);
    }
    /**
     * Выполняет запрос к API для создания документа о вводе товара в оборот. Учитывает ограничение по количеству запросов.
     *
     * @param document  Объект типа Document с информацией о создаваемом документе.
     * @param signature Строка, представляющая подпись для запроса.
     */
    public void createDocument(Document document, String signature) {
        rateLimiter.acquire();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonRequestBody = objectMapper.writeValueAsString(document);
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), jsonRequestBody);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(requestBody)
                    .header("Signature", signature)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    System.out.println("Не удалось создать документ. Код ответа: " + response.code());
                } else {
                    System.out.println("Документ успешно создан!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * Внутренний класс реализует механизм ограничения запросов в заданном временном интервале.
     */
    private static class RateLimiter {
        private final long intervalMillis;
        private final int requestLimit;
        private long lastRequestTime = System.currentTimeMillis();
        private int requestsInInterval = 0;
        public RateLimiter(int requestLimit, TimeUnit timeUnit) {
            this.intervalMillis = timeUnit.toMillis(1);
            this.requestLimit = requestLimit;
        }
        /**
         * Получает разрешение на выполнение запроса, учитывая ограничение на количество запросов в интервале.
         */
        public synchronized void acquire() {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRequestTime >= intervalMillis) {
                requestsInInterval = 0;
                lastRequestTime = currentTime;
            }
            if (requestsInInterval >= requestLimit) {
                long sleepTime = intervalMillis - (currentTime - lastRequestTime);
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                lastRequestTime = System.currentTimeMillis();
                requestsInInterval = 0;
            }
            requestsInInterval++;
        }
    }

    /**
     * Внутренний класс представляет структуру документа для ввода товара в оборот в соответствии с требованиями API.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    private static class Document {
        private Description description;
        private String docId;
        private String docStatus;
        private String docType;
        private boolean importRequest;
        private String ownerInn;
        private String participantInn;
        private String producerInn;
        private String productionDate;
        private String productionType;
        private List<Product> products;
        private String regDate;
        private String regNumber;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public class Product {
        private String certificateDocument;
        private String certificateDocument_date;
        private String certificateDocumentNumber;
        private String ownerInn;
        private String producerInn;
        private String productionDate;
        private String tnvedCode;
        private String uitCode;
        private String uituCode;
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public class Description {
        private String participantInn;
    }
}