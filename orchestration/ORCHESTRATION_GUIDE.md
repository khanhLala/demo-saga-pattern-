# Hướng Dẫn Chạy Demo Orchestration Pattern

### Chạy 3 Services
Bật các terminal riêng biệt cho mỗi service và chạy lệnh (hoặc Run trong IDE):

- **Order Service (Orchestrator - Port 8081)**:
  ```bash
  cd order-service
  mvn spring-boot:run
  ```
- **Inventory Service (Port 8082)**:
  ```bash
  cd inventory-service
  mvn spring-boot:run
  ```
- **Payment Service (Port 8083)**:
  ```bash
  cd payment-service
  mvn spring-boot:run
  ```

### Kiểm tra luồng hoạt động

#### 1. Luồng Thành Công
Gửi yêu cầu tạo đơn hàng bình thường:
```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 5}"
```
**Kết quả mong đợi:**
- Order Service lưu đơn `PENDING`.
- Gọi Inventory reserve thành công.
- Gọi Payment charge thành công.
- Cập nhật Order sang `COMPLETED`.

#### 2. Luồng Thất Bại tại Payment (Rollback Inventory)
Giả lập lỗi thanh toán bằng cách đặt quantity = 999:
```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 999}"
```
**Kết quả mong đợi:**
- Order Service lưu đơn `PENDING`.
- Gọi Inventory reserve (Trừ kho) thành công.
- Gọi Payment charge báo **Lỗi**.
- **Orchestrator gọi Inventory refund (Hoàn kho)**.
- Cập nhật Order sang `FAILED`.

#### 3. Luồng Thất Bại tại Inventory
Đặt số lượng vượt quá tồn kho (ví dụ: 10000):
```bash
curl -X POST http://localhost:8081/orders -H "Content-Type: application/json" -d "{\"productId\": 1, \"quantity\": 10000}"
```
**Kết quả mong đợi:**
- Order Service lưu đơn `PENDING`.
- Gọi Inventory reserve báo lỗi "Không đủ hàng".
- Cập nhật Order sang `FAILED` ngay lập tức (không gọi Payment).
