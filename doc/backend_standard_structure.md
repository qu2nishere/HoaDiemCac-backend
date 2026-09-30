# Cấu trúc Kiến trúc Chuẩn Dự án Backend Hỏa Diệm Các (Java 21 & Spring Boot 3.3.4)

Tài liệu này đặc tả chi tiết kiến trúc mã nguồn chuẩn doanh nghiệp (Production-Ready) của hệ thống Backend **Nhà hàng Lẩu Hỏa Diệm Các** (`HoaDiemCac-backend`), được xây dựng trên nền tảng **Java 21**, **Spring Boot 3.3.4**, **Spring Security (JWT)**, **Spring Data JPA (Hibernate)** và **WebSocket STOMP over SockJS**.

Codebase tuân thủ nghiêm ngặt các nguyên tắc thiết kế **Clean Architecture**, **SOLID**, **Domain-Driven Design (DDD)** và chuẩn hóa xử lý nghiệp vụ thời gian thực tại bàn.

---

## 1. Sơ đồ Cây Thư Mục Thực Tế Toàn Diện (Project Directory Tree)

```text
HoaDiemCac-server/
├── .mvn/wrapper/                      # Cấu hình Maven Wrapper độc lập môi trường
│   ├── maven-wrapper.jar
│   └── maven-wrapper.properties
├── doc/                               # Thư mục tài liệu kỹ thuật & đặc tả hệ thống
│   ├── backend_standard_structure.md  # [Tài liệu này] Kiến trúc và cấu trúc chuẩn backend
│   ├── codebase_client.txt            # Sơ đồ cây thư mục chi tiết của repo client
│   ├── codebase_client_anlyst.md       # Phân tích chuyên sâu kiến trúc frontend client
│   ├── DacTa_Chuong3_UseCase.md       # Đặc tả Use Case chi tiết UC01 - UC38
│   └── ke_hoach_trien_khai_entities.md# Thiết kế chi tiết Domain Entities & cơ sở dữ liệu
├── src/
│   ├── main/
│   │   ├── java/com/hoadiemcat/
│   │   │   ├── config/                # Cấu hình hệ thống, Beans, Security, WebSocket, CORS
│   │   │   │   ├── CloudinaryConfig.java   # Cấu hình SDK Cloudinary upload ảnh món ăn
│   │   │   │   ├── CorsConfig.java         # Cấu hình CORS cho phép Client gọi API & WebSocket
│   │   │   │   ├── DataSeeder.java         # Khởi tạo dữ liệu mẫu (User, Menu, Danh mục, Bàn)
│   │   │   │   ├── OpenApiConfig.java      # Cấu hình Swagger / OpenAPI 3.0 UI
│   │   │   │   ├── SecurityConfig.java     # Cấu hình Spring SecurityFilterChain, Filter JWT
│   │   │   │   ├── WebConfig.java          # Cấu hình WebMvc, Resource Handler
│   │   │   │   └── WebSocketConfig.java    # Cấu hình STOMP Message Broker (/ws, /topic, /app)
│   │   │   │
│   │   │   ├── controller/api/        # Tầng Controller tiếp nhận HTTP REST API endpoints
│   │   │   │   ├── AuthController.java                  # API /api/v1/auth (Đăng nhập nhân sự, Refresh token)
│   │   │   │   ├── DashboardAdminController.java        # API /api/v1/admin/dashboard (Thống kê doanh thu, KPI)
│   │   │   │   ├── EmployeeController.java              # API /api/v1/admin/employees (Quản lý tài khoản nhân viên)
│   │   │   │   ├── HealthCheckController.java           # API /api/v1/health (Kiểm tra trạng thái server)
│   │   │   │   ├── InvoiceAdminController.java          # API /api/v1/admin/invoices (Quản lý hóa đơn, đối soát)
│   │   │   │   ├── KitchenController.java               # API /api/v1/kitchen (Màn hình KDS Bếp FIFO, báo hết món)
│   │   │   │   ├── MenuItemController.java              # API /api/v1/menu-items & /api/v1/categories
│   │   │   │   ├── OrderCustomerController.java         # API /api/v1/customer/orders (Gửi đơn bếp, xem order bàn)
│   │   │   │   ├── TableCustomerController.java         # API /api/v1/customer/tables (Xác thực PIN, gọi phục vụ, quản lý thiết bị)
│   │   │   │   ├── TableQrAdminController.java          # API /api/v1/admin/tables (Quản lý bàn, cấp QR động, xoay PIN)
│   │   │   │   ├── TableTransferCustomerController.java # API /api/v1/customer/tables/transfer (Chuyển & Ghép bàn, Cụm bàn)
│   │   │   │   ├── UploadController.java                # API /api/v1/upload (Tải lên ảnh món ăn)
│   │   │   │   └── WaiterController.java                # API /api/v1/waiter (Màn hình nhân viên phục vụ, thu tiền mặt)
│   │   │   │
│   │   │   ├── dto/                   # Data Transfer Objects - đóng gói dữ liệu trao đổi
│   │   │   │   ├── request/           # DTO nhận vào từ Client (Payload validation)
│   │   │   │   │   ├── CategoryRequest.java             # Tạo / Sửa danh mục món
│   │   │   │   │   ├── CreateOrderRequest.java          # Gửi đơn order đợt mới từ giỏ hàng
│   │   │   │   │   ├── DraftCartItemRequest.java        # Đồng bộ món nháp trong giỏ hàng
│   │   │   │   │   ├── EmployeeCreateRequest.java       # Tạo mới tài khoản nhân viên
│   │   │   │   │   ├── EmployeeUpdateRequest.java       # Cập nhật thông tin nhân viên
│   │   │   │   │   ├── KickDeviceRequest.java           # Chủ bàn đá thiết bị khác ra khỏi bàn
│   │   │   │   │   ├── LoginRequest.java                # Thông tin đăng nhập nhân sự (username, password)
│   │   │   │   │   ├── MenuItemRequest.java             # Thêm / Sửa món ăn (tên, giá, mô tả, ảnh)
│   │   │   │   │   ├── OrderItemRequest.java            # Dòng món ăn trong đợt order
│   │   │   │   │   ├── TableClusterLinkRequest.java     # Ghép nhiều bàn thành Cụm Bàn (Master-Slave)
│   │   │   │   │   ├── TableCreateUpdateRequest.java    # Tạo / Sửa thông tin bàn ăn
│   │   │   │   │   ├── TableDirectTransferRequest.java  # Nhân viên chuyển bàn trực tiếp 1-chạm
│   │   │   │   │   ├── TableTransferConfirmRequest.java # Khách xác nhận nhập mã chuyển/ghép tại bàn đích
│   │   │   │   │   ├── TableTransferRequest.java        # Yêu cầu xuất mã chuyển bàn (TTL 5 phút)
│   │   │   │   │   ├── TransferHostRequest.java         # Nhường quyền Chủ Bàn (Host) cho thiết bị khác
│   │   │   │   │   └── VerifyPasscodeRequest.java       # Nhập mã PIN 4 số vào bàn ăn
│   │   │   │   └── response/          # DTO trả về cho Client
│   │   │   │       ├── ApiResponse.java                 # Format response chuẩn hóa {code, message, data, timestamp}
│   │   │   │       ├── CategoryResponse.java            # Thông tin danh mục món
│   │   │   │       ├── DashboardSummaryResponse.java    # Tổng quan doanh thu, số khách, top món
│   │   │   │       ├── DraftCartItemResponse.java       # Món nháp trong giỏ hàng cộng tác
│   │   │   │       ├── EmployeeResponse.java            # Thông tin nhân sự (không lộ password)
│   │   │   │       ├── InvoiceResponse.java             # Chi tiết hóa đơn thanh toán
│   │   │   │       ├── JwtAuthResponse.java             # Token JWT và quyền hạn sau khi đăng nhập
│   │   │   │       ├── MenuItemResponse.java            # Thông tin chi tiết món ăn
│   │   │   │       ├── OrderItemResponse.java           # Trạng thái từng món ăn (COOKING, SERVED...)
│   │   │   │       ├── OrderResponse.java               # Đợt gọi món kèm danh sách món
│   │   │   │       ├── TableClusterResponse.java        # Thông tin Cụm Bàn (Master Table + Slaves)
│   │   │   │       ├── TableDeviceResponse.java         # Danh sách thiết bị kết nối vào bàn (Host/Member)
│   │   │   │       ├── TableQrResponse.java             # Chi tiết bàn ăn kèm Dynamic QR và trạng thái khóa
│   │   │   │       ├── TableTransferConfirmResponse.java# Kết quả xác nhận chuyển/ghép bàn thành công
│   │   │   │       ├── TableTransferResponse.java       # Mã chuyển bàn sinh ra kèm thời hạn TTL
│   │   │   │       └── VerifyPasscodeResponse.java      # Kết quả xác thực PIN và phân quyền Host/Member
│   │   │   │
│   │   │   ├── entity/                # JPA Domain Entities đại diện các bảng trong Database
│   │   │   │   ├── AuditLog.java           # Nhật ký kiểm toán can thiệp của Quản lý (UC13)
│   │   │   │   ├── BaseEntity.java         # Lớp cơ sở chứa id, createdAt, updatedAt, audit hooks
│   │   │   │   ├── CallStaffLog.java       # Lịch sử chuông gọi phục vụ & thanh toán tại bàn (UC07, UC14)
│   │   │   │   ├── Cart.java               # Giỏ hàng cộng tác thời gian thực của bàn (UC04)
│   │   │   │   ├── CartItem.java           # Món ăn tạm trong giỏ hàng
│   │   │   │   ├── Category.java           # Danh mục món ăn (UC02, UC25)
│   │   │   │   ├── Invoice.java            # Hóa đơn thanh toán & doanh thu (UC15, UC30, UC31, UC32)
│   │   │   │   ├── MenuItem.java           # Món ăn trong thực đơn nhà hàng (UC02, UC10, UC24)
│   │   │   │   ├── Order.java              # Đợt gọi món (Order Round) gửi vào bếp (UC05)
│   │   │   │   ├── OrderItem.java          # Món ăn cụ thể trong đợt gọi kèm trạng thái chế biến (UC18)
│   │   │   │   ├── RestaurantTable.java    # Bàn ăn, Dynamic QR, xoay PIN, cờ khóa, Cụm bàn (UC11, UC27)
│   │   │   │   ├── TableSessionDevice.java # Thiết bị khách hàng tại bàn, phân quyền Host/Member (UC35)
│   │   │   │   ├── TableTransfer.java      # Giao dịch Chuyển/Ghép bàn áp dụng 2-Phase Lock (UC33, UC34)
│   │   │   │   ├── User.java               # Tài khoản quản trị, nhân viên, bếp (UC09, UC36)
│   │   │   │   └── enums/                  # Các Enum định danh trạng thái nghiệp vụ
│   │   │   │       ├── CallStaffStatus.java     # PENDING, RESOLVED, CANCELLED
│   │   │   │       ├── CallStaffType.java       # CALL_STAFF, PAYMENT_REQUEST, ICE_WATER, UTENSILS, OTHER
│   │   │   │       ├── OrderItemStatus.java     # COOKING, SERVED, CANCELLED (QĐ8)
│   │   │   │       ├── OrderStatus.java         # PENDING, COOKING, COMPLETED, CANCELLED
│   │   │   │       ├── PaymentMethod.java       # CASH, VIETQR
│   │   │   │       ├── PaymentStatus.java       # PENDING, PAID, CANCELLED, FAILED
│   │   │   │       ├── Role.java                # ADMIN, MANAGER, KITCHEN, STAFF, USER
│   │   │   │       ├── Status.java              # ACTIVE, INACTIVE, PENDING, DELETED
│   │   │   │       ├── TableArea.java           # COMMON (Sảnh chung), VIP (Phòng VIP)
│   │   │   │       ├── TableStatus.java         # AVAILABLE, OCCUPIED, CLEANING (QĐ7)
│   │   │   │       ├── TransferStatus.java      # PENDING, COMPLETED, CANCELLED, EXPIRED
│   │   │   │       └── TransferType.java        # MOVE (Chuyển bàn 1:1), MERGE (Ghép bàn N:1)
│   │   │   │
│   │   │   ├── exception/             # Xử lý ngoại lệ tập trung (Global Exception Handling)
│   │   │   │   ├── AppException.java              # Ngoại lệ nghiệp vụ chứa ErrorCode
│   │   │   │   ├── ErrorCode.java                 # Bộ mã lỗi chuẩn hóa kèm HTTP Status code
│   │   │   │   ├── GlobalExceptionHandler.java    # @RestControllerAdvice bắt và format mọi ngoại lệ
│   │   │   │   └── ResourceNotFoundException.java # Ngoại lệ khi không tìm thấy tài nguyên (HTTP 404)
│   │   │   │
│   │   │   ├── repository/            # Tầng truy cập dữ liệu (Spring Data JPA Repositories)
│   │   │   │   ├── CallStaffLogRepository.java
│   │   │   │   ├── CartRepository.java
│   │   │   │   ├── CategoryRepository.java
│   │   │   │   ├── InvoiceRepository.java
│   │   │   │   ├── MenuItemRepository.java
│   │   │   │   ├── OrderItemRepository.java
│   │   │   │   ├── OrderRepository.java
│   │   │   │   ├── RestaurantTableRepository.java
│   │   │   │   ├── TableSessionDeviceRepository.java
│   │   │   │   ├── TableTransferRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   │
│   │   │   ├── security/              # Tầng bảo mật, phân quyền và JWT filter
│   │   │   │   ├── CustomUserDetailsService.java  # Load UserDetails từ Database theo username/email
│   │   │   │   ├── JwtAuthenticationEntryPoint.java# Xử lý trả về lỗi HTTP 401 khi chưa xác thực
│   │   │   │   ├── JwtAuthenticationFilter.java   # Filter trích xuất Bearer Token & xác thực SecurityContext
│   │   │   │   └── JwtTokenProvider.java          # Sinh và giải mã JSON Web Token (HMAC-SHA512)
│   │   │   │
│   │   │   ├── service/               # Tầng giao diện nghiệp vụ (Interfaces)
│   │   │   │   ├── AuthService.java               # Nghiệp vụ xác thực & cấp JWT token
│   │   │   │   ├── DashboardService.java          # Nghiệp vụ tổng hợp báo cáo & doanh thu
│   │   │   │   ├── EmployeeService.java           # Nghiệp vụ quản lý tài khoản nhân viên
│   │   │   │   ├── InvoiceService.java            # Nghiệp vụ hóa đơn & đóng bàn thanh toán
│   │   │   │   ├── MailService.java               # Nghiệp vụ gửi email thông báo bất đồng bộ
│   │   │   │   ├── MenuItemService.java           # Nghiệp vụ quản lý món ăn và danh mục
│   │   │   │   ├── OrderService.java              # Nghiệp vụ xử lý order, đợt gọi món, KDS Bếp
│   │   │   │   ├── TableQrService.java            # Nghiệp vụ bàn ăn, Dynamic QR, PIN xoay vòng, Host/Member
│   │   │   │   ├── TableTransferService.java      # Nghiệp vụ Chuyển & Ghép bàn, Cụm Bàn Master-Slave
│   │   │   │   ├── UploadService.java             # Nghiệp vụ tải lên media / Cloudinary
│   │   │   │   └── impl/              # Các lớp hiện thực nghiệp vụ (Implementations)
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       ├── DashboardServiceImpl.java
│   │   │   │       ├── EmployeeServiceImpl.java
│   │   │   │       ├── InvoiceServiceImpl.java
│   │   │   │       ├── MailServiceImpl.java
│   │   │   │       ├── MenuItemServiceImpl.java
│   │   │   │       ├── OrderServiceImpl.java
│   │   │   │       ├── TableQrServiceImpl.java
│   │   │   │       ├── TableTransferServiceImpl.java
│   │   │   │       └── UploadServiceImpl.java
│   │   │   │
│   │   │   ├── util/                  # Tiện ích dùng chung
│   │   │   │   └── AppUtils.java                  # Các hàm helper, định dạng, sinh mã ngẫu nhiên
│   │   │   │
│   │   │   └── HoaDiemCatApplication.java         # Điểm khởi chạy ứng dụng Spring Boot
│   │   │
│   │   └── resources/                 # Cấu hình tài nguyên hệ thống
│   │       ├── application.properties             # Cấu hình chính (Datasource, JWT, Cloudinary, Mail, WS)
│   │       └── templates/                         # Templates email thông báo
│   │
│   └── test/                          # Kiểm thử tự động (Unit Test & Integration Test)
│       ├── java/com/hoadiemcat/
│       │   ├── HoaDiemCatApplicationTests.java    # Kiểm tra ngữ cảnh Spring Boot Context nạp thành công
│       │   ├── controller/                        # Test tầng Controller (MockMvc)
│       │   │   ├── MenuItemControllerTest.java
│       │   │   └── UploadControllerTest.java
│       │   └── service/                           # Test tầng Service & Business Logic
│       │       ├── MenuItemServiceTest.java
│       │       ├── TableQrServiceTest.java
│       │       ├── TableTransferServiceTest.java  # Bộ test toàn diện chuyển/ghép bàn & edge cases
│       │       └── UploadServiceTest.java
│       └── resources/
│           └── application-test.properties        # Cấu hình H2 In-Memory Database cho chạy test
│
├── uploads/                           # Thư mục lưu trữ media cục bộ (fallback khi không dùng Cloudinary)
├── .env.example                       # Khai báo mẫu biến môi trường bảo mật
├── .gitignore                         # Loại bỏ target, uploads, .env khỏi Git
├── pom.xml                            # File quản lý Maven dependencies (Spring Boot 3.3.4, Java 21)
└── README.md                          # Hướng dẫn cài đặt và khởi chạy dự án
```

---

## 2. Các Nguyên Tắc Vàng Trong Thiết Kế Hệ Thống

### 2.1. Phân Tầng Tuyệt Đối (Strict Layered Architecture)
- **Controller:** Đóng vai trò lớp tiếp nhận giao tiếp (Boundary Layer). Nhiệm vụ duy nhất là ánh xạ HTTP Request, kiểm tra `@Valid`, gọi Service tương ứng và đóng gói dữ liệu vào `ApiResponse<T>`. Tuyệt đối không chứa logic tính toán nghiệp vụ hay truy vấn SQL trực tiếp.
- **Service:** Đóng vai trò trái tim nghiệp vụ (Core Business Logic). Chịu trách nhiệm thực thi các nghiệp vụ cốt lõi, kiểm tra ràng buộc (QĐ1 – QĐ10), quản lý giao dịch `@Transactional`, bắn tin nhắn qua WebSocket STOMP và ghi nhật ký kiểm toán `AuditLog`.
- **Repository:** Đóng vai trò giao tiếp cơ sở dữ liệu qua Spring Data JPA. Định nghĩa các câu truy vấn tối ưu, hỗ trợ phân trang `Pageable`, sắp xếp và tối ưu hóa các Index đã định nghĩa trên Entities.

### 2.2. Giao Tiếp 100% Qua DTO (Data Transfer Object)
- Mọi API nhận đầu vào bằng **Request DTO** (được kiểm tra hợp lệ bằng Jakarta Bean Validation: `@NotBlank`, `@NotNull`, `@Min`, `@Max`, `@Pattern`).
- Mọi API trả kết quả qua **Response DTO** được bọc trong `ApiResponse<T>`.
- **Tuyệt đối không trả JPA Entities trực tiếp ra Controller**, ngăn ngừa:
  - Lộ lọt cấu trúc bảng dữ liệu nội bộ và các trường nhạy cảm (`password`, `failedAttempts`).
  - Lỗi `LazyInitializationException` khi serialize JSON ngoài phạm vi giao dịch.
  - Vòng lặp tuần hoàn (Infinite Circular Reference) giữa các thực thể có quan hệ 2 chiều (`Order` <-> `OrderItem`, `Table` <-> `TableSessionDevice`).

### 2.3. Cơ Chế Khóa Tạm Thời 2 Giai Đoạn (2-Phase Lock) Cho Chuyển & Ghép Bàn
- Nhằm khắc phục triệt để nguy cơ mất giỏ hàng đang chọn của khách:
  1. **Giai đoạn 1 (Request Transfer):** Sinh mã chuyển bàn có hiệu lực trong 5 phút (TTL 5 mins). Đặt cờ `isOrderLocked = true` tại bàn cũ để chống gửi thêm món phát sinh, nhưng **giữ nguyên vẹn 100% giỏ hàng và danh sách đợt order cũ**.
  2. **Giai đoạn 2 (Confirm Transfer):** Thực hiện tại bàn mới. Khách/Nhân viên quét QR hoặc nhập mã PIN bàn mới. Hệ thống tiến hành di dời toàn bộ đợt order (`Order`, `OrderItem`), hợp nhất giỏ hàng sang phiên mới, cập nhật trạng thái bàn cũ sang `CLEANING` và thông báo qua WebSocket cho tất cả thiết bị đồng bộ sang bàn mới.

### 2.4. Lá Chắn Chống Dò Mã PIN (Brute-Force Shield) & Passcode Rotation
- Mã PIN 4 chữ số (`currentPasscode`) của bàn ăn được sinh ngẫu nhiên an toàn (`SecureRandom`).
- Nếu khách nhập sai mã PIN quá 5 lần liên tiếp (`failedAttempts >= 5`):
  - Bàn bị khóa tạm thời trong 60 giây (`lockedUntil = now + 60s`).
  - API từ chối xác thực và trả về mã lỗi `TABLE_PASSCODE_LOCKED` kèm số giây còn lại phải chờ.
- Khi bàn kết thúc thanh toán hoặc nhân viên bấm cấp lại QR, mã PIN tự động xoay vòng và reset bộ đếm lỗi về 0.

### 2.5. Phân Quyền Chủ Bàn (Host) vs Thành Viên (Member) Tại Bàn Ăn
- Thiết bị đầu tiên quét QR và nhập PIN thành công sẽ được hệ thống gán quyền **Chủ Bàn (Host Device)**.
- Các thiết bị quét sau trong cùng phiên sẽ là **Thành viên (Member Device)**.
- Các quyền đặc quyền chỉ dành cho Chủ Bàn:
  - Bấm "Gửi đơn vào bếp".
  - Yêu cầu xuất mã Chuyển bàn / Ghép bàn.
  - Đá thiết bị lạ ra khỏi bàn (`KickDeviceRequest`).
  - Nhường quyền Chủ Bàn cho thiết bị khác (`TransferHostRequest`).
- Thu ngân/Quản lý có quyền tối cao trên giao diện POS để can thiệp reset Host hoặc kick thiết bị khi cần (`AdminTableDevicesModal`).

### 2.6. Hệ Thống Realtime WebSocket STOMP Đa Kênh
Hệ thống sử dụng STOMP over SockJS tại endpoint `/ws`:
- `/topic/tables`: Cập nhật trạng thái thời gian thực của toàn bộ sơ đồ bàn (Trống, Có khách, Dọn dẹp, Cụm bàn).
- `/topic/table/{sessionToken}` & `/topic/table/{tableNumber}/status`: Thông báo sự kiện riêng của bàn (Khóa order, Chuyển bàn, Đóng bàn).
- `/topic/kitchen/orders`: Bắn đợt order mới vào màn hình Bếp KDS theo thứ tự FIFO, cập nhật số bàn khi có chuyển bàn.
- `/topic/waiter/orders`: Thông báo cho nhân viên phục vụ khi món ăn chế biến xong cần bưng ra bàn.
- `/topic/menu-items`: Đồng bộ trạng thái Còn hàng / Hết hàng / Restock tức thì giữa Bếp KDS, Khách hàng và Quản lý thực đơn.

### 2.7. Xử Lý Ngoại Lệ Tập Trung (Centralized Exception Handling)
- Mọi lỗi nghiệp vụ đều được biểu diễn bằng `AppException(ErrorCode)`.
- `GlobalExceptionHandler` sử dụng `@RestControllerAdvice` để bắt:
  - `AppException`: Trả về HTTP Status và message chuẩn tương ứng từ `ErrorCode`.
  - `MethodArgumentNotValidException`: Trích xuất chi tiết vi phạm validate trên từng field DTO.
  - `BadCredentialsException` / `AccessDeniedException`: Xử lý lỗi bảo mật và phân quyền.
  - `Exception`: Bắt các lỗi không lường trước và trả về HTTP 500 kèm mã `INTERNAL_SERVER_ERROR`.
