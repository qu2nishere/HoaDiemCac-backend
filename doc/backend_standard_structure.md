# Cấu trúc thư mục chuẩn cho dự án Backend (Java Spring Boot)

Cấu trúc dưới đây được thiết kế dựa trên các nguyên tắc thiết kế phần mềm hiện đại, giúp dự án dễ dàng bảo trì, mở rộng và làm việc nhóm hiệu quả. Nó rất phù hợp làm boilerplate / template cho các dự án khởi tạo mới.

## Sơ đồ cấu trúc thư mục

```text
my-backend-project/
├── .mvn/                   # Thư mục cấu hình cho Maven wrapper
├── src/
│   ├── main/
│   │   ├── java/com/yourcompany/project/
│   │   │   ├── config/           # Các cấu hình chung của hệ thống (Security, Cors, Swagger, Bean config...)
│   │   │   ├── controller/       # Lớp giao tiếp với Client, chứa các REST API endpoints
│   │   │   │   └── api/          # (Tùy chọn) Có thể chia version API như v1, v2
│   │   │   ├── dto/              # Data Transfer Objects - object dùng truyền tải dữ liệu giữa client và server
│   │   │   │   ├── request/      # DTO cho input (nhận từ client)
│   │   │   │   └── response/     # DTO cho output (trả về client)
│   │   │   ├── entity/           # Chứa các class map với table trong Database (JPA Entities)
│   │   │   │   └── enums/        # Các Enum dùng trong hệ thống
│   │   │   ├── repository/       # Lớp giao tiếp với Database (Spring Data JPA)
│   │   │   ├── service/          # Lớp chứa Business Logic
│   │   │   │   └── impl/         # Các class implement interface của Service
│   │   │   ├── exception/        # Custom Exception và Global Exception Handler (bắt lỗi tập trung)
│   │   │   ├── security/         # Xử lý bảo mật, JWT, Authentication, Authorization filters
│   │   │   ├── mapper/           # Lớp chuyển đổi qua lại giữa Entity và DTO (thường dùng MapStruct)
│   │   │   ├── util/             # Các tiện ích chung, helper functions (StringUtil, DateUtil...)
│   │   │   ├── constant/         # Chứa các hằng số (Constants) của hệ thống
│   │   │   └── Application.java  # File main để chạy ứng dụng Spring Boot
│   │   └── resources/
│   │       ├── application.yml   # (hoặc application.properties) File cấu hình chính
│   │       ├── application-dev.yml # Cấu hình cho môi trường Development
│   │       ├── application-prod.yml# Cấu hình cho môi trường Production
│   │       ├── static/           # Chứa các file static (ít dùng cho thuần REST API)
│   │       └── templates/        # Chứa template (ví dụ email template)
│   └── test/                     # Thư mục chứa Unit Test và Integration Test
│       ├── java/com/yourcompany/project/
│       │   ├── controller/
│       │   ├── service/
│       │   └── repository/
│       └── resources/            # Cấu hình riêng cho lúc chạy test (ví dụ dùng H2 in-memory DB)
├── .env                          # File chứa các biến môi trường nhạy cảm (DB password, Secret keys...) - KHÔNG commit lên Git
├── .gitignore                    # Khai báo các file/thư mục không đưa lên Git
├── pom.xml                       # (Hoặc build.gradle) File quản lý thư viện phụ thuộc (Dependencies)
└── README.md                     # Tài liệu hướng dẫn setup, run dự án cho người mới vào
```

## Các nguyên tắc vàng cần tuân thủ

1. **Controller "mỏng", Service "dày":**
   - **Controller:** Chỉ làm nhiệm vụ điều hướng. Nó nhận Request, kiểm tra tính hợp lệ cơ bản, gọi Service để xử lý, và đóng gói kết quả thành Response để trả về. **KHÔNG** viết logic xử lý dữ liệu ở đây.
   - **Service:** Chứa toàn bộ Business Logic (nghiệp vụ của ứng dụng). Service gọi Repository để tương tác với dữ liệu.

2. **Giao tiếp qua DTO (Data Transfer Object):**
   - Luôn sử dụng DTO ở tầng Controller để nhận và trả dữ liệu.
   - **KHÔNG** dùng trực tiếp Entity (class đại diện cho bảng trong Database) để trả về cho Client. Điều này giúp ẩn đi cấu trúc database, tăng tính bảo mật và giúp API không bị vỡ (break) khi cấu trúc database thay đổi.

3. **Dependency Inversion (Sử dụng Interface cho Service):**
   - Tầng Controller nên gọi tới Interface của Service, thay vì Implementation trực tiếp. Class implement thực tế (ở thư mục `impl`) sẽ được Spring tự động inject vào. Việc này giúp code lỏng lẻo (loose coupling) và cực kỳ dễ dàng khi viết Unit Test (mock data).

4. **Global Exception Handling (Xử lý lỗi tập trung):**
   - Thay vì dùng `try-catch` lặp lại ở mọi function trong Controller, hãy văng ra một Exception (ví dụ: `ResourceNotFoundException`) ở tầng Service.
   - Dùng một class có anotation `@ControllerAdvice` ở thư mục `exception` để "tóm" tất cả các Exception này lại và trả về format lỗi chuẩn hóa (ví dụ luôn trả về mã code, message rõ ràng) cho client.

5. **Phân tách môi trường rõ ràng:**
   - Sử dụng các file `application-{profile}.yml` (như dev, prod) để cấu hình khác nhau cho từng môi trường (ví dụ môi trường Dev trỏ vào DB local, môi trường Prod trỏ vào DB cloud).

6. **Bảo mật file nhạy cảm:**
   - Các thông tin như Password Database, API Key, JWT Secret... phải được đưa vào file `.env` và dùng thư viện đọc lên. Tuyệt đối đưa file `.env` vào `.gitignore` để không bị lộ mã nguồn.
