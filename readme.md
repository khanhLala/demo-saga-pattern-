# Saga Pattern: Orchestration & Choreography
---

## 1. Bối cảnh ra đời của Saga Pattern

### Slide 1: 1.1 Transaction trong kiến trúc Monolithic
- Trong kiến trúc Monolithic, tất cả các logic nghiệp vụ đều nằm trong 1 ứng dụng và dùng chung 1 database duy nhất.
- Một transaction trong monolithic luôn đảm bảo **ACID**: (yêu cầu hình ảnh trong slide)
    - **A: Atomicity (Tính nguyên tử):** Một transaction gồm các bước nhỏ, là một khối thống nhất không thể tách rời, các bước phải được thực hiện cùng nhau, nếu 1 bước lỗi thì transaction không được thực hiện và dữ liệu trong DB phải nguyên vẹn như khi chưa thực hiện.
    - **C: Consistency (Tính nhất quán):** Dữ liệu luôn phải tuân thủ các quy tắc đã quy định trước. Dữ liệu trước và sau thực hiện transaction đều phải hợp lệ theo các quy tắc này. VD: khi chuyển tiền mà sau giao dịch số tiền âm => không hợp lệ, transaction bị hủy.
    - **I: Isolation (Tính cô lập):** Nhiều transaction chạy song song không làm ảnh hưởng lẫn nhau. Ví dụ: Cùng 1 tài khoản có 2 người rút tiền cùng lúc => người thực hiện trước thì transaction được xử lý trước, người còn lại phải đợi transaction trước xong mới được tiếp tục xử lý.
    - **D: Durability (Tính bền vững):** Sau khi transaction thành công thì dữ liệu được lưu lại vĩnh viễn vào ổ cứng. Dù có bất kỳ sự cố nào xảy ra thì dữ liệu này vẫn phải bảo đảm nguyên vẹn hoặc có thể backup lại được.
- Transaction trong mô hình monolithic luôn bảo đảm dữ liệu được nhất quán rất mạnh (strong consistency), nếu chỉ có một bước nào đó trong transaction bị lỗi thì tất cả sẽ được rollback tự động.

*End slide 1*

### Slide 2: 1.2 Transaction trong kiến trúc microservices
- Trong kiến trúc microservices, mỗi yêu cầu (request) thường đi qua nhiều service khác nhau, mỗi service có DB riêng, nghĩa là request đi đến service A, service A thực hiện thành công, lưu vào DB của A rồi mới tiếp tục thực hiện service B. (yêu cầu hình ảnh sơ đồ kiểu: service A có DB A, service B có DB B…)
- Tính nguyên tử (atomicity), tính nhất quán (consistency) và tính cô lập (isolation) bị phá vỡ:
    - **Tính nguyên tử bị phá vỡ:** Khi một hành động thực hiện nhưng gặp lỗi, ví dụ: service A thực hiện thành công => DB của A lưu dữ liệu, nhưng đến service B bị lỗi thì DB của A không tự động rollback lại dữ liệu như ban đầu được.
    - **Tính nhất quán bị phá vỡ:** Nhưng từ nhất quán mạnh trở thành nhất quán cuối cùng: Service A thực hiện xong hành động, lưu vào DB của A, dữ liệu đã thay đổi mặc dù chuỗi transaction chưa thực hiện xong.
    - **Tính cô lập bị phá vỡ:** Giữa thời gian các service thực hiện, có khoảng thời gian trống. Người dùng có thể nhìn thấy dữ liệu sau khi service A thành công mặc dù khi đó toàn bộ hành động chưa thực hiện thành công => dữ liệu sẽ bị lệch trong một khoảng thời gian dù rất ngắn.

**Giải pháp:**
- Chấp nhận dữ liệu chỉ có sự nhất quán cuối cùng (eventual consistency): dữ liệu có thể bị lệch trong một khoảng thời gian ngắn, nhưng cuối cùng thì dữ liệu vẫn phải chính xác.
- Sử dụng Saga Pattern.

*End Slide 2*

### Slide 3 + 4: 1.3 Saga Pattern

**Slide 3**
- Saga Pattern là một mẫu/thiết kế dùng để đảm bảo tính nhất quán dữ liệu trong kiến trúc Microservices. Nó chia một transaction lớn thành các transactions nhỏ nối tiếp nhau.
- Saga Pattern vận hành theo công thức (P đọc là Process khi thuyết trình) P1 → P2 → … → Pn, với Pi (1 < i ≤ n).
- Khi xảy ra lỗi, do không thể rollback như monolithic, nó sẽ chạy những transactions theo ý nghĩa bù trừ các transactions đã thực hiện, ví dụ lỗi ở Pk thì nó sẽ chạy Pk → Pk-1 →….P1.

*End slide 3*

**Slide 4**
- Ví dụ: Transaction lớn gồm 3 service Order, Inventory và Payment.
- **Luồng bình thường:** Order tạo order → Inventory kiểm tra và trừ kho → Payment thanh toán thành công.
- **Luồng lỗi:** Order tạo order → Inventory kiểm tra và trừ kho → Payment thanh toán, gặp lỗi → Inventory hoàn lại số lượng của kho → Order cập nhật trạng thái thất bại.

*End slide 4*

---

## 2. Orchestration Pattern

### Slide 5: Orchestration Pattern
- Orchestration Pattern là một dạng mẫu thiết kế phân tán, trong đó có một thành phần điều phối (orchestrator). Nó nắm giữ kịch bản triển khai một hoạt động, bao gồm các bước và thứ tự thực hiện trong luồng hoạt động, cách xử lý bù (compensation) nếu xảy ra lỗi.
- Thông thường service khởi tạo quy trình sẽ được chọn làm orchestrator.
- **Thành phần tham gia:** Các services. Các services này không biết và cũng không cần biết bản thân đang nằm trong quy trình nào. Chúng chỉ cần nhận lệnh, làm và báo cáo lại kết quả.
- **Cơ chế:** Command based. Orchestrator gửi lệnh đến các service, các service xử lý sau đó gửi reply về orchestrator.

*End slide 5*

### Slide 6: Luồng hoạt động bình thường
(Yêu cầu vẽ workflow trích xuất sang ảnh cho đẹp và đủ thông tin để cho vào slide)
Ví dụ luồng hoạt động với 3 service: Order, Inventory và Payment, xét hoạt động tạo đơn hàng và thanh toán. Chọn OrderService là Orchestrator. (Ảnh workflow 1)

- **Bước 1:** Client tạo Order → OrderService lưu order mới (POST /order/create) với status là PENDING. Gán transactionId là id cho workflow này.
- **Bước 2:** Orchestrator (chính là OrderService) gọi InventoryService (POST /inventory/reserve), truyền theo transactionId và thông tin order, product và số lượng. InventoryService sẽ kiểm tra số lượng và trừ đi trong kho (nếu đủ), gửi lại phản hồi cho Orchestrator.
- **Bước 3:** Orchestrator nhận phản hồi từ InventoryService, sau đó Orchestrator gọi PaymentService (POST /payment/charge), truyền theo transactionId, thông tin order và thanh toán, nếu thành công, thực hiện lưu payment record vào DB, sau đó reply orchestrator trạng thái payment.
- **Bước 4:** Orchestrator nhận phản hồi từ PaymentService, sau đó sửa trạng thái của order từ PENDING thành DONE (Orchestrator không cần gọi đến OrderService do OrderService chính là Orchestrator). Truyền theo paymentId, transactionId và orderId.

*Hết Slide 6*

### Slide 7: Luồng hoạt động lỗi
(Ảnh workflow 2) - Lỗi ở bước kiểm tra kho:
- Bước 1, 2 tương tự như quy trình chuẩn.
- **Bước 3:** Orchestrator nhận phản hồi từ InventoryService sau đó sửa trạng thái PENDING thành FAILED. Truyền theo transactionId và orderId.

(Ảnh workflow 3, có thể để ở slide thứ 8) - Lỗi ở bước Payment:
- Bước 1, 2, 3 tương tự như quy trình chuẩn.
- **Bước 4:** Orchestrator nhận phản hồi từ PaymentService, sau đó gọi InventoryService (POST /inventory/refund). Truyền theo transactionId và orderId để tìm ra lần trừ kho tương ứng, sau đó trả lại số lượng về kho theo số lượng đã trừ tương ứng rồi gửi reply đến Orchestrator.
- **Bước 5:** Orchestrator nhận reply từ InventoryService, sau đó cập nhật trạng thái order từ PENDING thành FAILED.

*Hết slide 7*

### Một số lưu ý khi triển khai
- Orchestration cần lưu trạng thái saga (id, bước/trạng thái hiện tại) để có thể resume khi gặp lỗi và truy vết lịch sử hành động, đồng thời kiểm tra transactionId khi thực hiện ở mỗi service để tránh trùng lặp.
- **Phân biệt lỗi cần rollback/retry:**
    - **Retry:** Thực hiện với các request gặp lỗi tạm thời như lỗi về mạng hoặc service unavailable. Nếu sau nhiều lần retry không được thì phải lưu lại các request này vào 1 file log.
    - **Rollback:** Với những lỗi nằm trong luồng nghiệp vụ, ví dụ như refund inventory nếu payment lỗi thì việc rollback là cần thiết. Nhưng với những lỗi như khi gửi thông báo cho người dùng về việc order thành công hay thất bại thì đây là lỗi không cần rollback, chỉ cần ghi lại lỗi và retry.

### Slide 8: Ưu và nhược điểm
- **Ưu điểm:**
    - Luồng nghiệp vụ được kiểm soát tập trung tại orchestrator => dễ quan sát, triển khai, kiểm tra, truy vấn và debug.
    - Phù hợp với những nghiệp vụ phức tạp, cần kiểm soát chặt chẽ các điều kiện.
- **Nhược điểm:**
    - Biến orchestrator thành điểm tập trung xử lý logic, làm nó trở nên phức tạp khi số services tăng cao hoặc luồng nghiệp vụ dài.
    - Gây ra sự phụ thuộc (coupling) giữa orchestrator và các service.

---

## 3. Choreography Pattern

### Slide 9: Choreography Pattern
- Choreography Pattern là một mẫu thiết kế phân tán không có orchestrator trung tâm.
- **Cơ chế:** Event based: dựa trên sự kiện. Các services giao tiếp với nhau thông qua events qua message broker như Kafka hay RabbitMQ. Trong message broker, mỗi service sẽ có một topic riêng để publish (gửi) sự kiện lên. Các services cũng lắng nghe sự kiện từ những topics của các services khác. Mỗi service publish event khi hoàn thành nhiệm vụ, các service khác subscribe và tiếp tục workflow.

### Slide 10 + 11: Luồng hoạt động bình thường
Ví dụ luồng hoạt động với 3 service: Order, Inventory và Payment, xét hoạt động tạo đơn hàng và thanh toán.
- **OrderService:** Publish gói tin lên kênh `order-events`, subscribe kênh `inventory-events` và `payment-events` để lắng nghe.
- **InventoryService:** Publish gói tin lên kênh `inventory-events` và lắng nghe ở 2 kênh `order-events` và `payment-events`.
- **PaymentService:** Publish lên 2 kênh `payment-events` và lắng nghe ở kênh `inventory-events`.
(Nên tạo thêm 1 slide vẽ 1 hình ảnh chỉ rõ mối quan hệ giữa các service và topics. Các topics nằm trong message broker Kafka và vẽ thêm các DB tương ứng với từng service.)

- **Bước 1:** Khách hàng gọi API POST /orders. OrderService tiếp nhận, lưu đơn hàng vào Database với trạng thái PENDING và sinh ra một transactionId. OrderService đóng gói một gói tin có tên là OrderCreatedEvent (Sự kiện Đơn hàng đã được tạo), đính kèm transactionId và danh sách món hàng. Nó ném sự kiện này lên Topic `order-events`. Lúc này, OrderService đi làm việc khác, không chờ đợi ai cả.
- **Bước 2:** InventoryService vốn đang lắng nghe ở topic `order-events`. Nó bắt được gói tin OrderCreatedEvent. Nó sẽ đọc data, chui vào Database của nó để trừ số lượng sản phẩm. Trừ kho thành công, InventoryService ném một gói tin InventoryReservedEvent (Sự kiện Kho đã giữ hàng) lên topic `inventory-events`.
- **Bước 3:** PaymentService đang theo dõi Topic `inventory-events`. Nó tóm được gói tin InventoryReservedEvent. Nó thực hiện thao tác thanh toán. Trừ tiền thành công, nó ném gói tin PaymentBilledEvent (Sự kiện Đã thu tiền) lên topic `payment-events`.
- **Bước 4:** OrderService vẫn luôn lắng nghe ở Topic `payment-events`. Nó bắt được sự kiện PaymentBilledEvent. Nó truy vấn DB của nó, tìm đúng đơn và cập nhật trạng thái từ PENDING sang COMPLETED.

### Slide 12 + 13: Luồng hoạt động lỗi
- **Bước 1 & Bước 2:** Diễn ra bình thường như trên. Kho đã giữ hàng và bắn ra InventoryReservedEvent.
- **Bước 3:** Sự cố xảy ra: PaymentService tóm được event giữ hàng, tiến hành trừ tiền. Nhưng cổng thanh toán báo "Không đủ số dư". Lúc này PaymentService ném ra một gói tin PaymentFailedEvent (Sự kiện Thanh toán thất bại) lên Topic `payment-events`.
- **Bước 4:** 
    - Tại InventoryService đang lắng nghe Topic `payment-events` tóm được PaymentFailedEvent. Nó tra mã đơn hàng, lôi hàng đã giữ ra cộng ngược lại vào kho.
    - Tại OrderService bắt được PaymentFailedEvent. Nó tra DB, đổi đơn tương ứng từ PENDING sang FAILED.

### Slide 14: Ưu – nhược điểm
- **Ưu điểm:**
    - Không có single point of orchestration, tránh được việc service bị sập. Giống như việc điều tiết giao thông ở ngã tư, khi có cảnh sát thực hiện thì hoạt động mượt mà, nhưng khi cảnh sát không thực hiện được nhiệm vụ đó nữa thì sẽ gây ra tê liệt ách tắc giao thông. Với choreography, nó giống như 4 đèn giao thông ở 4 phía, các màu đèn giống như các gói tin để con người biết nên đi hay không.
    - Loose coupling tối đa giữa các services. Service này không cần biết có service kia hay không mà chỉ cần bắt gói tin thông qua message broker.
    - Có thể mở rộng (Scale) từng service độc lập.
    - Phản ứng nhanh với events.
- **Nhược điểm:**
    - Khó debug và test (do events phân tán, không có nơi nào lưu lại toàn bộ vòng đời của transaction từ A-Z).
    - Phức tạp trong việc đảm bảo thứ tự sự kiện. Ví dụ: luồng hoạt động ở trên bị lỗi ở chỗ Payment, thì kho vừa hoàn hàng, order vừa cập nhật thành FAILED, phải bảo đảm được kho hoàn hàng xong thì order mới FAILED.
    - Cần cơ sở hạ tầng công nghệ mạnh (như message broker, hay hệ thống giám sát).

### Slide 15: So sánh orchestration và choreography
| Tiêu chí | Saga Orchestration | Saga Choreography |
| :--- | :--- | :--- |
| **Cơ chế hoạt động** | Command driven | Event driven |
| **Điều khiển** | Thông qua một orchestrator để điều phối | Phân tán, không có người điều phối |
| **Giao tiếp** | Thường dùng lệnh đồng bộ như RESTful API / gRPC | Bất đồng bộ thông qua message broker |
| **Tính độc lập** | Trung bình, orchestrator phải biết được tên, địa chỉ của dịch vụ khác | Không phụ thuộc nhau |
| **Theo dõi tiến độ** | Ngay trong log/CSDL của orchestrator | Phải truy vết theo từng service |
| **Khả năng mở rộng** | Trung bình, do phải sửa code trong orchestrator | Dễ dàng do chỉ cần lắng nghe sự kiện chứ không phụ thuộc vào service khác |

---

## 4. So sánh và lựa chọn các mẫu thiết kế
- **Với Monolithic:** Có độ phức tạp thấp, dễ debug, tuy nhiên khó mở rộng và sự phụ thuộc cao giữa các module. Do đó sử dụng với hệ thống nhỏ, không quá nhiều người dùng, yêu cầu tốc độ phát triển nhanh, không có nhiều nhu cầu mở rộng (scale) phức tạp.
- **Với Orchestration:** Độ phức tạp trung bình, khả năng debug và scale trung bình, độ phụ thuộc giữa các module trung bình. Do đó có thể sử dụng khi cần kiểm soát luồng nghiệp vụ rõ ràng, khi logic xử lý nghiệp vụ phức tạp với nhiều điều kiện phân nhánh.
- **Với Choreography:** Độ phức tạp cao, khó debug, dễ mở rộng, độ phụ thuộc giữa các module thấp. Do đó phù hợp với hệ thống cần giảm sự phụ thuộc tối đa, cần scale mở rộng độc lập từng service.
