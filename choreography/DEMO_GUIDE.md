# Hướng Dẫn Từng Bước Chạy Demo (Choreography Pattern)

Tôi đã code xong khung demo đơn giản cho mô hình **Event-based Choreography** sử dụng Kafka (theo đúng lý thuyết trong file `báo cáo.docx` của bạn). 
Dưới đây là các bước để bạn chạy và trải nghiệm:

### Bước 1: Khởi động Kafka & Zookeeper
Tôi đã tạo file `docker-compose.yml` tại thư mục gốc của project. Bạn chỉ cần mở terminal tại thư mục `c:\demo-tkpmhdv\choreography` và chạy:
```bash
docker-compose up -d
```
*(Đảm bảo Docker Desktop của bạn đang chạy)*

### Bước 2: Cập nhật cấu hình Payment Service (Thủ công)
Do file `payment-service\src\main\resources\application.yaml` đang được mở trong editor của bạn nên hệ thống không thể tự động ghi đè. Bạn hãy mở file đó lên và dán nội dung sau vào:

```yaml
server:
  port: 8083
spring:
  application:
    name: payment-service
  datasource:
    url: jdbc:h2:mem:paymentdb
    driverClassName: org.h2.Driver
    username: sa
    password:
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
      path: /h2-console
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: payment-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

### Bước 3: Chạy 3 Services
Hãy bật 3 tab terminal riêng biệt và chạy lần lượt 3 service (hoặc bạn có thể bấm Run trực tiếp trong IDE):
- Tab 1: `cd order-service` rồi chạy `./mvnw spring-boot:run` (hoặc `mvnw.cmd spring-boot:run`)
- Tab 2: `cd inventory-service` rồi chạy `./mvnw spring-boot:run`
- Tab 3: `cd payment-service` rồi chạy `./mvnw spring-boot:run`

### Bước 4: Chạy Postman / Curl để test luồng Thành Công
Gửi một request tạo Order (số lượng < 999):
```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 5}"
```
**Quan sát Log ở các tab:**
- Order Service tạo đơn (PENDING) -> Ném event `OrderCreatedEvent`.
- Inventory Service trừ kho -> Ném event `InventoryReservedEvent`.
- Payment Service trừ tiền -> Ném event `PaymentBilledEvent`.
- Order Service đổi trạng thái PENDING -> COMPLETED.

### Bước 5: Test luồng Thất Bại (Hiệu ứng Domino ngược / Compensation)
Trong demo, tôi đã code logic giả lập: **nếu quantity = 999 thì Payment tài khoản sẽ không đủ tiền (FAILED)**.
```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 999}"
```
**Quan sát Log ở các tab:**
- Order tạo đơn -> Inventory giữ hàng thành công -> Payment báo lỗi.
- Payment ném ra `PaymentFailedEvent`.
- Khâu **bù trừ (Compensation)** chạy: Inventory nhận được event lỗi, tiến hành **cộng lại số hàng vừa trừ (Hoàn kho)**.
- Order Service đổi trạng thái về FAILED.
