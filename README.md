# HỆ THỐNG NGÂN HÀNG SỐ RIKKEIBANK API (MICROSERVICES ARCHITECTURE)

Dự án Mini Project môn Microservices (IT214 - SS18) - Xây dựng nền tảng Web Service Microservice phân tán chuẩn ngân hàng số (Fintech) cho chi nhánh ngân hàng bán lẻ **RikkeiBank**.

---

## 1. TỔNG QUAN KIẾN TRÚC & SO SÁNH MONOLITHIC VS MICROSERVICES

### 1.1 Vì sao chuyển đổi từ Monolithic sang Microservices (MSA)?
| Tiêu chí | Hệ thống Cũ (Monolithic) | Hệ thống Mới (RikkeiBank Microservices) |
| :--- | :--- | :--- |
| **Mở rộng (Scalability)** | Phải nhân bản toàn bộ ứng dụng cồng kềnh khi lưu lượng chuyển khoản tăng đột biến. | Mở rộng độc lập từng service (ví dụ scale riêng `transaction-service` vào giờ cao điểm). |
| **Cô lập lỗi (Fault Isolation)** | Một module gặp sự cố (như gửi email/SMS bị treo) có thể làm tê liệt toàn bộ ngân hàng. | Lỗi được cô lập; Circuit Breaker ngăn lỗi dây chuyền (Cascading Failure), giao dịch tài chính vẫn an toàn. |
| **Cơ sở dữ liệu (Database)** | Dùng chung 1 DB khổng lồ, nghẽn cổ chai (bottleneck), xung đột khóa bảng. | **Database-per-service**: mỗi service quản lý cơ sở dữ liệu riêng, bảo mật và độc lập. |
| **Tốc độ triển khai (CI/CD)** | Mỗi lần sửa 1 tính năng nhỏ phải build và deploy lại cả hệ thống. | Triển khai độc lập từng microservice mà không làm gián đoạn các dịch vụ khác. |

---

## 2. BẢN ĐỒ MICROSERVICES & CỔNG KẾT NỐI

| Tên Service | Port | Công nghệ chính | Trách nhiệm nghiệp vụ |
| :--- | :--- | :--- | :--- |
| **`discovery-service`** | `8761` | Eureka Server | Service Registry & Discovery, giám sát sức khỏe dịch vụ |
| **`config-service`** | `8888` | Spring Cloud Config Server | Quản lý cấu hình tập trung cho toàn bộ hệ thống |
| **`gateway-service`** | `8080` | Spring Cloud Gateway, JWT Filter | Điểm đón duy nhất (Single Entry Point), xác thực JWT, RBAC, định tuyến |
| **`identity-service`** | `8081` | Spring Security, JJWT, Redis | Đăng nhập/Đăng ký, JWT Access/Refresh Token, Force Logout |
| **`customer-service`** | `8082` | Spring Data JPA, Redis Cache | Quản lý hồ sơ Khách hàng & Nhân viên/Giao dịch viên (CRUD) |
| **`account-service`** | `8083` | Spring Data JPA, Redis Cache | Danh mục Loại tài khoản, Tài khoản, thực hiện Debit/Credit/Compensate |
| **`transaction-service`**| `8084` | OpenFeign, Resilience4j, Kafka | **Saga Orchestrator** chuyển khoản, lịch sử giao dịch ngày của TELLER |
| **`notification-service`**| `8085`| WebFlux Reactive, Kafka Consumer| Tiêu thụ sự kiện từ Kafka, SSE stream biến động số dư thời gian thực |
| **`common-library`** | N/A | DTO, Event, Exception | Thư viện dùng chung chứa DTOs, Kafka Events, Global Exception |

---

## 3. SƠ ĐỒ LUỒNG SAGA PATTERN & CƠ CHẾ ROLLBACK (COMPENSATING)

### 3.1 Luồng Thành Công (Happy Path)
```
Client --(POST /transfer)--> Gateway (8080) --> Transaction-Service (8084)
                                                        │
                             ┌──────────────────────────┴──────────────────────────┐
                             ▼ (Bước 1: Trừ tiền)                                  ▼ (Bước 2: Cộng tiền)
                   Account-Service (8083)                                Account-Service (8083)
                     Tài khoản Nguồn (-Số tiền)                            Tài khoản Đích (+Số tiền)
                             │                                                     │
                             └──────────────────────────┬──────────────────────────┘
                                                        ▼ (Giao dịch hoàn tất - COMPLETED)
                                          Gửi Kafka Event: TransferCompletedEvent
                                                        │
                                                        ▼
                                          Notification-Service (8085)
                                          (Gửi thông báo biến động số dư qua SSE / Log)
```

### 3.2 Kịch Bản Gặp Lỗi & Rollback Tự Động (Compensating Transaction)
Khi Bước 2 (Cộng tiền vào tài khoản đích) thất bại do lỗi tài khoản đích bị khóa / mạng đứt / hoặc mô phỏng lỗi:
```
Transaction-Service (8084) ──(Bước 1: Trừ tiền thành công)──► Account-Service: TK nguồn đã trừ tiền
        │
        ├──(Bước 2: Cộng tiền thất bại!) ──► BỊ LỖI
        │
        └──► [KÍCH HOẠT SAGA COMPENSATING ACTION]
             Transaction-Service gọi: POST /api/v1/accounts/compensate-debit
             Account-Service thực hiện: HOÀN TRẢ LẠI TIỀN VÀO TÀI KHOẢN NGUỒN!
             Trạng thái Transaction: FAILED_ROLLEDBACK
             Gửi Kafka Event: TransferRollbackEvent
             ==> KẾT QUẢ: SỐ DƯ TÀI KHOẢN NGUỒN ĐƯỢC BẢO TOÀN NGUYÊN VẸN!
```

---

## 4. HƯỚNG DẪN KHỞI CHẠY HỆ THỐNG

### Cách 1: Khởi chạy nhanh không cần cài đặt ngoài (Chế độ In-memory H2 Zero-setup)
Hệ thống được thiết kế với cơ chế tự khởi tạo cơ sở dữ liệu in-memory H2 độc lập cho từng service, đồng thời nạp sẵn dữ liệu mẫu.

1. **Build toàn bộ dự án và chạy Unit Tests:**
   ```bash
   .\gradlew.bat clean build
   ```

2. **Khởi chạy toàn bộ 8 Microservices:**
   Chỉ cần click đúp vào file `run-all-services.bat` hoặc chạy:
   ```cmd
   run-all-services.bat
   ```
   Script sẽ tự động mở 8 cửa sổ cmd cho 8 microservice theo đúng thứ tự khởi động chuẩn:
   - `Discovery Service` (Eureka: http://localhost:8761)
   - `Config Service` (Port: 8888)
   - `Gateway Service` (Port: 8080)
   - `Identity Service` (Port: 8081)
   - `Customer Service` (Port: 8082)
   - `Account Service` (Port: 8083)
   - `Transaction Service` (Port: 8084)
   - `Notification Service` (Port: 8085)

3. **Dừng toàn bộ hệ thống khi xong:**
   Chạy script:
   ```cmd
   stop-all-services.bat
   ```

---

### Cách 2: Khởi chạy với Docker Infrastructure (MySQL, Redis, Kafka)
1. **Bật hạ tầng container:**
   ```bash
   docker-compose up -d
   ```
   Container gồm có:
   - `MySQL 8.0` (Port 3306 - tự động tạo 4 DB: `identity_db`, `customer_db`, `account_db`, `transaction_db`)
   - `Redis 7` (Port 6379)
   - `Zookeeper` (Port 2181) & `Apache Kafka` (Port 9092)

2. **Chạy các service với profile `docker`:**
   ```bash
   .\gradlew.bat bootRun --args='--spring.profiles.active=docker'
   ```

---

## 5. DỮ LIỆU MẪU ĐƯỢC KHỞI TẠO TỰ ĐỘNG

| Tài khoản | Username | Password | Role | Quyền hạn & Mô tả |
| :--- | :--- | :--- | :--- | :--- |
| **Quản trị viên** | `admin` | `admin123` | `ROLE_ADMIN` | Quản trị toàn hệ thống, CRUD Khách hàng/Nhân viên/Loại tài khoản, Force Logout |
| **Giao dịch viên**| `teller1` | `teller123` | `ROLE_TELLER` | Giao dịch viên chi nhánh Cầu Giấy, chuyển tiền tại quầy, xem lịch sử GD trong ngày |
| **Khách hàng 1** | `customer1` | `customer123` | `ROLE_CUSTOMER` | Nguyễn Văn An - Sở hữu TK `1011223344` (Số dư ban đầu: 10,000,000 VND) |
| **Khách hàng 2** | `customer2` | `customer123` | `ROLE_CUSTOMER` | Trần Thị Bình - Sở hữu TK `1022334455` (Số dư ban đầu: 5,000,000 VND) |

---

## 6. HƯỚNG DẪN TEST VÀ THUYẾT TRÌNH BẰNG POSTMAN COLLECTION

File Postman Collection được xuất sẵn tại thư mục gốc:
**`RikkeiBank_API_Collection.postman_collection.json`**

### Các bước demo thuyết trình:
1. **Bước 1 (Đăng nhập):**
   - Mở Postman, Import file `RikkeiBank_API_Collection.postman_collection.json`.
   - Vào thư mục `01. Identity & Auth Service` -> Chạy request `1.1 Login as ADMIN`, `1.2 Login as TELLER`, `1.3 Login as CUSTOMER`. Token JWT sẽ được tự động lưu vào Collection Variables (`admin_token`, `teller_token`, `customer_token`).
2. **Bước 2 (Trải nghiệm phiên đăng nhập liền mạch - Refresh Token):**
   - Chạy `1.5 Seamless Session - Refresh Token`: Khách hàng lấy lại token mới mà không cần nhập lại mật khẩu.
3. **Bước 3 (Admin cưỡng chế thu hồi quyền truy cập - Force Logout):**
   - Chạy `1.6 Admin Force Logout User`: ADMIN ép tài khoản customer1 đăng xuất lập tức. Toàn bộ token trước đó bị vô hiệu hóa qua Blacklist.
4. **Bước 4 (Danh mục & Caching Redis):**
   - Chạy các request trong `02. Customer & Staff Catalog Service` và `03. Account & Catalog Service`. Lần gọi đầu query database, lần thứ hai lấy trực tiếp từ Cache (Cache-Aside).
5. **Bước 5 (Trình diễn Chuyển khoản Thành Công qua Saga):**
   - Chạy `4.1 [Saga Happy Path] Customer Transfer Money`: Chuyển 500,000 VND từ `1011223344` sang `1022334455`.
   - Kiểm tra số dư: TK `1011223344` còn 9,500,000 VND, TK `1022334455` tăng lên 5,500,000 VND. Trạng thái giao dịch: `COMPLETED`.
6. **Bước 6 (Trình diễn SAGA ROLLBACK khi gặp lỗi):**
   - Chạy `4.2 [Saga Rollback Demo] Transfer with Simulated Failure`: Request có cờ `simulateFailure: true`.
   - Hệ thống thực hiện trừ tiền TK nguồn -> Gọi cộng tiền TK đích bị lỗi -> Saga kích hoạt Compensating Debit hoàn trả tiền.
   - Kết quả: Trạng thái giao dịch `FAILED_ROLLEDBACK`, số dư tài khoản `1011223344` không bị hao hụt một xu nào!
7. **Bước 7 (Giao dịch viên xem báo cáo trong ngày):**
   - Chạy `4.4 Teller View Daily Transactions`: Giao dịch viên xem toàn bộ giao dịch mình đã phục vụ trong ngày.
8. **Bước 8 (Thông báo bất đồng bộ & WebFlux SSE):**
   - Chạy `5.1 Get Customer Balance Fluctuation Notifications` hoặc kết nối `5.3 WebFlux SSE Realtime Stream`.
9. **Bước 9 (Kiểm tra Phân quyền Security & Khách vãng lai):**
   - Chạy `6.1 Anonymous Access`: Khách chưa đăng nhập bị chặn `401 Unauthorized`.
   - Chạy `6.2 Customer Accessing Admin API`: Khách hàng gọi API của Admin bị chặn `403 Forbidden`.

---

## 7. BÁO CÁO KIỂM THỬ VÀ ĐỘ BAO PHỦ MÃ NGUỒN (JACOCO)
Tất cả các dịch vụ cốt lõi đều được viết Unit Test toàn diện cho Service & Controller:
- `AccountServiceTest.java`: Kiểm thử trừ tiền, cộng tiền, kiểm tra số dư tối thiểu, hoàn tiền Saga.
- `SagaTransferOrchestratorTest.java`: Kiểm thử luồng Saga Happy Path và luồng Saga Rollback khi Bước 2 gặp ngoại lệ.
- `AuthServiceTest.java`: Kiểm thử đăng ký, đăng nhập sai pass, mã hóa BCrypt, Force Logout.
- `CustomerServiceTest.java`: Kiểm thử tìm kiếm hồ sơ và chống trùng lặp CCCD/Email.

Để xem báo cáo chi tiết độ bao phủ:
```bash
.\gradlew.bat test jacocoTestReport
```
File báo cáo HTML sinh ra tại:
`<service-name>/build/reports/jacoco/test/html/index.html`
