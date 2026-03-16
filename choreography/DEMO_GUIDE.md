# Hướng Dẫn Từng Bước Chạy Demo (Choreography Pattern)

### Bước 1: Khởi động Kafka & Zookeeper
```bash
docker-compose up -d
```
*(Đảm bảo Docker Desktop của bạn đang chạy)*

### Bước 2: Chạy 3 Services
Hãy bật 3 tab terminal riêng biệt và chạy lần lượt 3 service 
- Tab 1: `cd order-service` rồi chạy `./mvnw spring-boot:run` (hoặc `mvnw.cmd spring-boot:run`)
- Tab 2: `cd inventory-service` rồi chạy `./mvnw spring-boot:run`
- Tab 3: `cd payment-service` rồi chạy `./mvnw spring-boot:run`

### Bước 3: Chạy Postman / Curl để test luồng Thành Công
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
