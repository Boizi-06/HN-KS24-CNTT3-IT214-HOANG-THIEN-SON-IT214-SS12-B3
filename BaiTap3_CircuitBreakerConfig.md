# BÀI TẬP 3: LẬP TRÌNH NGẮT MẠCH VÀ BẪY "MINIMUM CALLS"

---

## PHẦN 1: BÁO CÁO PHÂN TÍCH

### Câu hỏi: Đêm đó có đúng 5 khách hàng thanh toán, cả 5 đều bị lỗi (Tỷ lệ lỗi 100%). Nhưng Circuit Breaker KHÔNG HỀ MỞ. Tại sao?

### Nguyên nhân cốt lõi: Bẫy cấu hình `minimumNumberOfCalls`

Trong thư viện **Resilience4j**, cơ chế hoạt động của Circuit Breaker được kiểm soát bởi hai thông số thống kê liên quan mật thiết với nhau:
1. `slidingWindowSize`: Kích thước cửa sổ trượt (ở đây cấu hình là **20** request).
2. `minimumNumberOfCalls`: **Số lượng cuộc gọi tối thiểu bắt buộc phải tích lũy đủ** trong Sliding Window trước khi Circuit Breaker bắt đầu tính toán tỷ lệ lỗi (`failureRateThreshold`) để quyết định xem có Mở mạch (OPEN) hay không.

### Điều gì đã xảy ra trong thực tế?
1. **Giá trị mặc định của `minimumNumberOfCalls`:**
   - Theo tài liệu của Resilience4j, nếu lập trình viên **không cấu hình tường minh** `minimumNumberOfCalls`, hệ thống sẽ tự động gán giá trị mặc định của nó **bằng đúng `slidingWindowSize`** (tức là **20**).
2. **Số lượng request ban đêm chưa đạt ngưỡng kích hoạt:**
   - Ban đêm lưu lượng thấp, chỉ có đúng **5 request** được gửi đi.
   - Mặc dù cả 5 request đều thất bại (tỷ lệ lỗi thực tế là $5/5 = 100\%$, vượt xa ngưỡng $50\%$), nhưng:
     $$\text{Số request thực tế } (5) < \text{minimumNumberOfCalls } (20)$$
3. **Hệ quả:**
   - Circuit Breaker coi tập mẫu hiện tại là **chưa đủ độ tin cậy thống kê** để đánh giá sức khỏe của service.
   - Do đó, Circuit Breaker **bỏ qua hoàn toàn việc tính toán Failure Rate**, tiếp tục duy trì trạng thái **CLOSED (Đóng mạch)**.
   - Kết quả: Hệ thống StoreX tiếp tục nã request lỗi sang Ngân hàng đối tác, vi phạm nghiêm trọng cam kết: *"Nếu lỗi quá 50% phải ngắt kết nối trong 30s"*.

---

## PHẦN 2: TRIỂN KHAI FILE `application.yml` HOÀN CHỈNH

### 1. Nguyên tắc giải quyết bài toán:
- Ta cần cấu hình tường minh `minimumNumberOfCalls` với một giá trị nhỏ hơn hoặc bằng 5 (ví dụ: **5**) để ngay khi 5 request ban đêm bị lỗi, Cầu dao lập tức MỞ MẠCH.
- **Ràng buộc bất biến của Resilience4j:**
  $$\mathbf{minimumNumberOfCalls \ge permittedNumberOfCallsInHalfOpenState}$$
  - Đề bài quy định: Ở trạng thái `HALF_OPEN`, chỉ cho phép thả **3** request trinh sát (`permittedNumberOfCallsInHalfOpenState = 3`).
  - Do đó, `minimumNumberOfCalls` bắt buộc phải $\ge 3$.
  - Lựa chọn tối ưu: **`minimumNumberOfCalls = 5`** (vừa thỏa mãn $\ge 3$, vừa đạt $100\%$ nhạy bén khi 5 request đêm bị lỗi).

---

### 2. Nội dung file `application.yml` hoàn chỉnh

File: `src/main/resources/application.yml`

```yaml
server:
  port: 8082

spring:
  application:
    name: payment-service

# =========================================================================
# CẤU HÌNH RESILIENCE4J CIRCUIT BREAKER CHO INSTANCE 'bankClient'
# =========================================================================
resilience4j:
  circuitbreaker:
    instances:
      bankClient:
        # 1. Kiểu Sliding Window dựa trên số lượng request
        slidingWindowType: COUNT_BASED

        # 2. Kích thước Sliding Window = 20 request gần nhất
        slidingWindowSize: 20

        # 3. KHẮC PHỤC BẪY "MINIMUM CALLS":
        # Số lượng request tối thiểu cần thu thập để bắt đầu tính toán tỷ lệ lỗi.
        # - Đảm bảo nguyên tắc: minimumNumberOfCalls (5) >= permittedNumberOfCallsInHalfOpenState (3)
        # - Khi có 5 khách đêm bị lỗi: 5 >= 5 (đạt điều kiện), tỷ lệ lỗi là 100% > 50%,
        #   Cầu dao sẽ LẬP TỨC MỞ MẠCH (OPEN) trong 30s đúng yêu cầu ngân hàng.
        minimumNumberOfCalls: 5

        # 4. Ngưỡng tỷ lệ lỗi kích hoạt Mở mạch = 50%
        failureRateThreshold: 50

        # 5. Thời gian ngắt kết nối ở trạng thái OPEN = 30 giây
        waitDurationInOpenState: 30s

        # 6. Ở trạng thái HALF_OPEN: chỉ cho phép 3 request đi qua để trinh sát
        permittedNumberOfCallsInHalfOpenState: 3

        # 7. Tự động chuyển từ OPEN sang HALF_OPEN sau khi hết 30 giây
        automaticTransitionFromOpenToHalfOpenEnabled: true

logging:
  level:
    com.storex.payment: INFO
    io.github.resilience4j: DEBUG
```
