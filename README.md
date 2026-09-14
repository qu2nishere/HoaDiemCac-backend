# HoaDiemCat Backend (Spring Boot)

Hệ thống Backend cho dự án HoaDiemCat được xây dựng bằng **Java 21** và **Spring Boot 3.3.4**, tuân thủ các tiêu chuẩn thiết kế phần mềm hiện đại (Controller mỏng - Service dày, DTO tách biệt, Global Exception Handling, bảo mật JWT, Swagger UI OpenAPI 3).

---

## 📁 Cấu trúc thư mục dự án

```text
HoaDiemCat-backend/
├── .mvn/wrapper/             # Cấu hình Maven Wrapper
├── src/
│   ├── main/
│   │   ├── java/com/hoadiemcat/
│   │   │   ├── config/       # Cấu hình hệ thống (Security, CORS, OpenAPI Swagger)
│   │   │   ├── controller/   # REST API Endpoints
│   │   │   │   └── api/      # Versioned APIs (v1, v2)
│   │   │   ├── dto/          # Data Transfer Objects
│   │   │   │   ├── request/  # DTO nhận từ client
│   │   │   │   └── response/ # DTO trả về cho client (ApiResponse chuẩn hóa)
│   │   │   ├── entity/       # JPA Entities map với các bảng MySQL
│   │   │   │   └── enums/    # Các Enum (Role, Status...)
│   │   │   ├── repository/   # Giao tiếp với MySQL (Spring Data JPA)
│   │   │   ├── service/      # Business Logic interfaces
│   │   │   │   └── impl/     # Service implementations
│   │   │   ├── exception/    # Custom Exceptions & GlobalExceptionHandler (@RestControllerAdvice)
│   │   │   ├── security/     # JWT Token Provider, JWT Filter & EntryPoint
│   │   │   ├── util/         # Tiện ích chung (Date, String...)
│   │   │   ├── constant/     # Hằng số hệ thống (AppConstants)
│   │   │   └── HoaDiemCatApplication.java  # Main application runner
│   │   └── resources/
│   │       ├── application.yml.example     # File cấu hình mẫu (commit lên git)
│   │       ├── application.yml             # Cấu hình chính (bị gitignore)
│   │       ├── application-dev.yml         # Cấu hình môi trường Development
│   │       ├── application-prod.yml        # Cấu hình môi trường Production
│   │       ├── static/                     # Static resources
│   │       └── templates/                  # Email/HTML templates
│   └── test/
│       ├── java/com/hoadiemcat/            # Unit Test & Integration Test
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   └── HoaDiemCatApplicationTests.java
│       └── resources/
│           └── application-test.yml        # Cấu hình test dùng H2 in-memory DB
├── .env.example              # File mẫu khai báo biến môi trường (commit lên git)
├── .env                      # File biến môi trường thực tế (bị gitignore)
├── .gitignore                # Khai báo file bỏ qua khi commit (target, .env, application.yml...)
├── mvnw.cmd                  # Maven Wrapper script cho Windows
├── pom.xml                   # File quản lý Dependencies và Plugins
└── README.md                 # Tài liệu hướng dẫn dự án
```

---

## 🛠️ Thư viện và Tiện ích tích hợp (Dependencies)

| Dependency | Mục đích |
| :--- | :--- |
| **Spring Boot Starter Web** | Xây dựng RESTful Web APIs |
| **Spring Boot Starter Data JPA** | Hibernate & Spring Data giao tiếp với cơ sở dữ liệu |
| **MySQL Connector/J** | Driver kết nối MySQL 8.x |
| **Lombok** | Giảm thiểu code getter, setter, builder, constructor |
| **Spring Boot Starter Validation** | Xác thực dữ liệu đầu vào (`@Valid`, `@NotNull`, `@NotBlank`, ...) |
| **Spring Security & JJWT (0.12.6)** | Xác thực và phân quyền bằng JSON Web Token (Stateless) |
| **SpringDoc OpenAPI (Swagger UI)** | Tự động sinh tài liệu API trực quan tại `/swagger-ui.html` |
| **Springboot Dotenv** | Tự động nạp biến môi trường từ file `.env` vào Spring properties |
| **H2 Database & Spring Boot Test** | Hỗ trợ Unit Test và Integration Test độc lập với DB thật |

---

## ⚙️ Hướng dẫn cài đặt và chạy dự án

### 1. Yêu cầu môi trường
- **Java JDK 21** trở lên.
- **MySQL Server** (phiên bản 8.x).

### 2. Cấu hình biến môi trường
Sao chép `.env.example` thành `.env` (nếu chưa có) và cập nhật thông tin kết nối Database của bạn:
```bash
# Server Configuration
SERVER_PORT=8080

# MySQL Database Configuration
DB_HOST=localhost
DB_PORT=3306
DB_NAME=hoadiemcat_db
DB_USERNAME=root
DB_PASSWORD=root

# JWT Security
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION=86400000
```

> **Lưu ý:** Tạo sẵn database trong MySQL:
> ```sql
> CREATE DATABASE hoadiemcat_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
> ```

### 3. Build và chạy ứng dụng

Sử dụng Maven Wrapper có sẵn trong thư mục dự án:

```powershell
# Chạy ứng dụng trực tiếp
.\mvnw.cmd spring-boot:run

# Hoặc build file jar
.\mvnw.cmd clean package
java -jar target/hoadiemcat-backend-0.0.1-SNAPSHOT.jar
```

### 4. Kiểm tra ứng dụng hoạt động
- **Health Check Endpoint:** [http://localhost:8080/api/v1/public/health](http://localhost:8080/api/v1/public/health)
- **Swagger UI (Tài liệu API):** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON Docs:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 📐 Các nguyên tắc cần lưu ý khi phát triển

1. **Controller mỏng, Service dày:** Mọi logic xử lý dữ liệu và nghiệp vụ phải đặt ở Service, Controller chỉ làm nhiệm vụ tiếp nhận và phản hồi HTTP.
2. **Luôn sử dụng DTO:** Không expose Entity ra Controller. Dữ liệu request truyền qua `dto/request`, dữ liệu response trả về thông qua `dto/response` bọc trong `ApiResponse<T>`.
3. **Sử dụng Interface cho Service:** Luôn tạo Interface trong `service/` và class thực thi trong `service/impl/`.
4. **Xử lý Exception tập trung:** Ném ra `AppException(ErrorCode)` hoặc custom exception tương ứng, để `GlobalExceptionHandler` bắt và định dạng response chuẩn cho client.
