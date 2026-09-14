# Kế hoạch Triển khai Domain Entities Chuẩn Production cho Backend Hỏa Diệm Các (HoaDiemCat)

Tài liệu này đặc tả chi tiết kế hoạch xây dựng bộ JPA Entities & Enums chuẩn doanh nghiệp/production-ready cho dự án Backend Spring Boot (`HoaDiemCac-backend`), bám sát tài liệu đặc tả Use Case (Chương 3), 10 quy định nghiệp vụ (QĐ1 – QĐ10) của đề tài Nhà hàng Lẩu Hỏa Diệm Các, cùng quy chuẩn cấu trúc thư mục trong `backend_standard_structure.md`.

---

## 1. Phân tích Nghiệp vụ & Danh sách Entities cần thiết kế

Dựa trên tài liệu đặc tả UC01 – UC32 và sơ đồ hoạt động:
- **Khách hàng tại bàn:** Quét mã QR vào bàn (UC01), duyệt thực đơn & danh mục (UC02, UC03), quản lý giỏ hàng cộng tác thời gian thực (UC04, UC21, UC22, UC23), gửi đợt order (UC05), theo dõi trạng thái món (UC06), gọi phục vụ (UC07), yêu cầu thanh toán (UC08).
- **Nhân sự quản lý & Bếp:** Đăng nhập JWT (UC09), quản lý thực đơn (UC10, UC24, UC25, UC26), quản lý bàn & mã QR (UC11, UC27, UC28, UC29), sơ đồ bàn ăn (UC12), can thiệp order (UC13), tiếp nhận yêu cầu gọi phục vụ/thanh toán (UC14), chốt hóa đơn thanh toán tiền mặt / VietQR (UC15, UC30, UC31, UC32), thống kê báo cáo (UC16), điều phối bếp KDS FIFO (UC17, UC18), báo hết món khẩn cấp (UC19).

### Danh mục Enums trong hệ thống (`com.hoadiemcat.entity.enums`):
1. **`Role`**: Cập nhật thêm `KITCHEN` để phân quyền cho trạm bếp (gồm: `ADMIN`, `MANAGER`, `KITCHEN`, `STAFF`, `USER`).
2. **`Status`**: Trạng thái tài khoản người dùng (`ACTIVE`, `INACTIVE`, `PENDING`, `DELETED`).
3. **`TableStatus`**: Trạng thái bàn theo QĐ7 (`AVAILABLE` - Bàn trống, `OCCUPIED` - Đang có khách, `CLEANING` - Đang dọn dẹp).
4. **`TableArea`**: Khu vực bàn (`COMMON` - Khu vực chung, `VIP` - Phòng VIP hoàng gia).
5. **`OrderStatus`**: Trạng thái của một đợt order (`PENDING`, `COOKING`, `COMPLETED`, `CANCELLED`).
6. **`OrderItemStatus`**: Trạng thái từng món ăn theo QĐ8 (`COOKING` - Đang chuẩn bị, `SERVED` - Đã phục vụ, `CANCELLED` - Đã hủy).
7. **`PaymentMethod`**: Phương thức thanh toán (`CASH` - Tiền mặt, `VIETQR` - Quét mã VietQR chuyển khoản).
8. **`PaymentStatus`**: Trạng thái thanh toán (`PENDING`, `PAID`, `CANCELLED`, `FAILED`).
9. **`CallStaffType`**: Loại yêu cầu từ khách (`CALL_STAFF` - Gọi chung, `PAYMENT_REQUEST` - Yêu cầu tính tiền, `ICE_WATER` - Thêm đá/nước, `UTENSILS` - Chén đũa, `OTHER` - Hỗ trợ khác).
10. **`CallStaffStatus`**: Trạng thái yêu cầu hỗ trợ (`PENDING` - Đang chờ, `RESOLVED` - Đã xử lý, `CANCELLED` - Đã hủy).

---

### Danh mục Entities (`com.hoadiemcat.entity`):

Mọi Entity đều kế thừa từ `BaseEntity` (đã có sẵn `id`, `createdAt`, `updatedAt`).

#### 1. `User` (Bảng `users`)
- **Mục đích:** Quản lý tài khoản nhân viên, quản lý, đầu bếp đăng nhập vào hệ thống quản trị và màn hình KDS (UC09).
- **Các trường:**
  - `username` (VARCHAR(50), Unique, Not Null) - Tên đăng nhập
  - `password` (VARCHAR(255), Not Null) - Mật khẩu đã mã hóa BCrypt
  - `fullName` (VARCHAR(100), Not Null) - Họ và tên nhân sự
  - `email` (VARCHAR(100), Unique, Not Null) - Email liên hệ
  - `phoneNumber` (VARCHAR(20)) - Số điện thoại
  - `role` (EnumType.STRING, Not Null) - Vai trò trong hệ thống (ADMIN, MANAGER, KITCHEN, STAFF)
  - `status` (EnumType.STRING, Not Null) - Trạng thái hoạt động (ACTIVE, INACTIVE)
  - `lastLoginAt` (LocalDateTime) - Thời điểm đăng nhập gần nhất

#### 2. `RestaurantTable` (Bảng `restaurant_tables`)
- **Mục đích:** Quản lý bàn ăn, sơ đồ mặt bằng, cờ khóa order và token phiên QR động (UC01, UC11, UC12, UC27, UC28, UC29).
- **Các trường:**
  - `tableNumber` (VARCHAR(20), Unique, Not Null) - Mã định danh bàn (VD: "B01", "VIP 11")
  - `name` (VARCHAR(100), Not Null) - Tên hiển thị (VD: "Bàn số 01", "Phòng VIP Hoàng Triều")
  - `area` (EnumType.STRING, Not Null) - Phân khu (`COMMON`, `VIP`)
  - `capacity` (Integer, Not Null) - Sức chứa tối đa (số khách)
  - `status` (EnumType.STRING, Not Null) - Trạng thái bàn: `AVAILABLE`, `OCCUPIED`, `CLEANING` (QĐ7)
  - `isOrderLocked` (Boolean, Not Null, Default false) - Cờ khóa order khẩn cấp của bàn (QĐ3, UC29)
  - `currentSessionToken` (VARCHAR(128)) - Token phiên QR động hiện tại (UUID v4 + băm theo QĐ2, UC01, UC28)
  - `sessionStartedAt` (LocalDateTime) - Thời điểm mở bàn khi khách quét QR đầu tiên
  - `qrCodeUrl` (VARCHAR(500)) - Đường dẫn URL/ảnh mã QR hiện tại

#### 3. `Category` (Bảng `categories`)
- **Mục đích:** Phân loại danh mục món ăn (UC02, UC10, UC25).
- **Các trường:**
  - `name` (VARCHAR(100), Not Null) - Tên danh mục (VD: "Nước Lẩu Hoàng Gia", "Bò Thượng Hạng & Wagyu")
  - `slug` (VARCHAR(100), Unique, Not Null) - Đường dẫn thân thiện (VD: "nuoc-lau", "bo-wagyu")
  - `description` (TEXT) - Mô tả chi tiết về danh mục
  - `displayOrder` (Integer, Default 0) - Thứ tự hiển thị ưu tiên trên thanh cuộn menu (UC25)
  - `isActive` (Boolean, Not Null, Default true) - Cờ bật/tắt hiển thị danh mục
  - `isSystem` (Boolean, Not Null, Default false) - Đánh dấu danh mục hệ thống tự tổng hợp như "Bán chạy" (QĐ5, BR25.1)
  - Quan hệ: Many-to-Many với `MenuItem`.

#### 4. `MenuItem` (Bảng `menu_items`)
- **Mục đích:** Lưu trữ thông tin món ăn, đơn giá, tình trạng còn/hết hàng và số lượt gọi (UC02, UC03, UC10, UC19, UC24, UC26).
- **Các trường:**
  - `code` (VARCHAR(30), Unique, Not Null) - Mã món ăn (VD: "M01", "M02")
  - `name` (VARCHAR(200), Not Null) - Tên món ăn
  - `description` (TEXT) - Mô tả thành phần, hương vị, hướng dẫn nhúng
  - `price` (BigDecimal, Not Null) - Đơn giá hiện hành (VND)
  - `unit` (VARCHAR(50)) - Đơn vị tính (VD: "Khay 250g", "Nồi 9 ngăn", "Đĩa 200g")
  - `imageUrl` (VARCHAR(1000)) - Đường dẫn ảnh minh họa món ăn
  - `isAvailable` (Boolean, Not Null, Default true) - Tình trạng Còn hàng / Hết hàng (QĐ4, UC19, UC26)
  - `isFeatured` (Boolean, Not Null, Default false) - Món đặc sắc / khuyên dùng
  - `isDeleted` (Boolean, Not Null, Default false) - Cờ xóa mềm (Soft Delete) theo BR24.1
  - `totalOrderedCount` (Long, Default 0) - Tổng số lượt đã bán để tự động tổng hợp danh mục "Bán chạy" (QĐ5)
  - Quan hệ: Many-to-Many với `Category` thông qua bảng liên kết `menu_item_categories`.

#### 5. `Cart` (Bảng `carts`)
- **Mục đích:** Quản lý giỏ hàng cộng tác thời gian thực tại bàn (Collaborative Realtime Cart) (UC04, UC21, UC22, UC23).
- **Các trường:**
  - `restaurantTable` (ManyToOne, Not Null) - Bàn đang sở hữu giỏ hàng
  - `sessionToken` (VARCHAR(128), Not Null) - Token phiên gắn liền với giỏ
  - Quan hệ: One-to-Many với `CartItem` (cascade = ALL, orphanRemoval = true).

#### 6. `CartItem` (Bảng `cart_items`)
- **Mục đích:** Lưu từng món đang được chọn trong giỏ chung trước khi gửi bếp.
- **Các trường:**
  - `cart` (ManyToOne, Not Null) - Giỏ hàng cha
  - `menuItem` (ManyToOne, Not Null) - Món ăn được chọn
  - `quantity` (Integer, Not Null) - Số lượng chọn ($1 \le N \le 99$ theo QĐ9, UC22)
  - `note` (VARCHAR(255)) - Ghi chú riêng cho món (VD: "Ít cay", "Không lấy đá")

#### 7. `Order` (Bảng `orders`)
- **Mục đích:** Đại diện cho một đợt gọi món (Order Round) được gửi vào bếp (UC05, UC06, UC13).
- **Các trường:**
  - `restaurantTable` (ManyToOne, Not Null) - Bàn thực hiện order
  - `sessionToken` (VARCHAR(128), Not Null) - Mã phiên của bàn lúc order
  - `roundNumber` (Integer, Not Null) - Số thứ tự đợt gọi món (Đợt 1, Đợt 2, Đợt 3...)
  - `status` (EnumType.STRING, Not Null) - Trạng thái đợt order (`PENDING`, `COOKING`, `COMPLETED`, `CANCELLED`)
  - `totalAmount` (BigDecimal, Not Null) - Tổng số tiền của riêng đợt order này
  - `note` (VARCHAR(500)) - Ghi chú tổng quát của đợt gọi
  - Quan hệ: One-to-Many với `OrderItem` (cascade = ALL, orphanRemoval = true).

#### 8. `OrderItem` (Bảng `order_items`)
- **Mục đích:** Từng dòng món ăn cụ thể đã được gửi vào bếp để chế biến (UC05, UC06, UC13, UC17, UC18).
- **Các trường:**
  - `order` (ManyToOne, Not Null) - Đợt order chứa món
  - `menuItem` (ManyToOne, Not Null) - Món ăn tương ứng
  - `price` (BigDecimal, Not Null) - Đơn giá tại thời điểm khách bấm gọi món (chống biến động giá sau này)
  - `quantity` (Integer, Not Null) - Số lượng món gọi
  - `totalPrice` (BigDecimal, Not Null) - Thành tiền = price * quantity
  - `status` (EnumType.STRING, Not Null) - Trạng thái chế biến: `COOKING`, `SERVED`, `CANCELLED` (QĐ8)
  - `note` (VARCHAR(255)) - Ghi chú phục vụ của khách
  - `servedAt` (LocalDateTime) - Thời điểm bếp đánh dấu món đã chế biến xong (UC18)
  - `cancelledAt` (LocalDateTime) - Thời điểm món bị hủy (nếu có)
  - `cancelReason` (VARCHAR(255)) - Lý do hủy món (UC13)

#### 9. `Invoice` (Bảng `invoices`)
- **Mục đích:** Lưu trữ hóa đơn thanh toán và doanh thu khi đóng bàn (UC08, UC15, UC16, UC30, UC31, UC32).
- **Các trường:**
  - `invoiceCode` (VARCHAR(50), Unique, Not Null) - Mã hóa đơn duy nhất (VD: "HD-20260914-0001")
  - `restaurantTable` (ManyToOne, Not Null) - Bàn thanh toán
  - `sessionToken` (VARCHAR(128), Not Null) - Token phiên của bàn khi thanh toán
  - `subtotal` (BigDecimal, Not Null) - Tổng tiền món chưa trừ chiết khấu/thuế (QĐ1)
  - `discountPercent` (BigDecimal, Default 0) - Phần trăm chiết khấu (nếu có)
  - `discountAmount` (BigDecimal, Default 0) - Số tiền chiết khấu được giảm
  - `taxPercent` (BigDecimal, Default 8) - Tỷ lệ thuế VAT (%)
  - `taxAmount` (BigDecimal, Default 0) - Số tiền thuế VAT
  - `finalAmount` (BigDecimal, Not Null) - Tổng số tiền thực thanh toán cuối cùng (theo công thức QĐ1)
  - `paymentMethod` (EnumType.STRING, Not Null) - Phương thức: `CASH` hoặc `VIETQR` (UC30, UC31)
  - `paymentStatus` (EnumType.STRING, Not Null) - Trạng thái: `PENDING`, `PAID`, `CANCELLED`
  - `paidAt` (LocalDateTime) - Thời điểm xác nhận thanh toán thành công
  - `cashReceived` (BigDecimal) - Số tiền khách đưa (khi thanh toán tiền mặt)
  - `cashChange` (BigDecimal) - Tiền thối trả khách (khi thanh toán tiền mặt)
  - `transactionRef` (VARCHAR(100)) - Mã tham chiếu chuyển khoản hoặc nội dung VietQR Napas247
  - `cashier` (ManyToOne `User`) - Nhân viên / Quản lý xác nhận thu tiền và đóng bàn
  - `isPrinted` (Boolean, Default false) - Trạng thái đã in hóa đơn nhiệt khổ 80mm (UC32)

#### 10. `CallStaffLog` (Bảng `call_staff_logs`)
- **Mục đích:** Nhật ký chuông gọi nhân viên và yêu cầu thanh toán từ bàn (UC07, UC14).
- **Các trường:**
  - `restaurantTable` (ManyToOne, Not Null) - Bàn gửi tín hiệu gọi
  - `requestType` (EnumType.STRING, Not Null) - Loại hỗ trợ (`CALL_STAFF`, `PAYMENT_REQUEST`, `ICE_WATER`, `UTENSILS`, `OTHER`)
  - `message` (VARCHAR(255)) - Lời nhắn chi tiết từ khách
  - `status` (EnumType.STRING, Not Null) - Trạng thái: `PENDING` (chờ xử lý), `RESOLVED` (đã hỗ trợ xong)
  - `resolvedAt` (LocalDateTime) - Thời điểm nhân viên xác nhận hỗ trợ thành công
  - `resolvedBy` (ManyToOne `User`) - Nhân viên xử lý yêu cầu

#### 11. `AuditLog` (Bảng `audit_logs`)
- **Mục đích:** Nhật ký can thiệp và điều chỉnh order của Quản lý khi xử lý sự cố (UC13, BR13.1).
- **Các trường:**
  - `actor` (ManyToOne `User`, Not Null) - Quản lý thực hiện can thiệp
  - `action` (VARCHAR(50), Not Null) - Hành động (VD: "CANCEL_ITEM", "UPDATE_QUANTITY", "CHANGE_ITEM")
  - `targetEntity` (VARCHAR(50), Not Null) - Tên thực thể bị tác động (VD: "OrderItem", "Order")
  - `targetId` (Long, Not Null) - Khóa chính ID của bản ghi bị tác động
  - `reason` (VARCHAR(500), Not Null) - Lý do can thiệp bắt buộc phải nhập (BR13.1)
  - `oldValue` (TEXT) - Dữ liệu cũ trước can thiệp
  - `newValue` (TEXT) - Dữ liệu mới sau can thiệp

---

## 2. Tiêu chuẩn Kỹ thuật Entity Chuẩn Production

1. **Javadoc / Chú thích chi tiết:**
   - Mỗi file Entity có mô tả class, mục đích nghiệp vụ, bảng tương ứng trong DB.
   - Mỗi trường đều có chú thích JavaDoc tiếng Việt chi tiết: ý nghĩa, kiểu dữ liệu, ràng buộc nghiệp vụ, Use Case liên quan.
2. **JPA Annotations chuẩn mực:**
   - `@Entity`, `@Table(name = "...", indexes = { ... })` đánh index hợp lý cho các cột tìm kiếm thường xuyên (`table_number`, `code`, `session_token`, `status`, `invoice_code`).
   - `@Column(nullable = ..., length = ..., unique = ...)` đầy đủ.
   - Định dạng `BigDecimal` cho giá tiền (`precision = 15, scale = 2`) để đảm bảo không bị sai lệch số học trong giao dịch tài chính.
   - `@Enumerated(EnumType.STRING)` tránh phụ thuộc thứ tự ordinal của enum.
   - `@JsonIgnore` / `@ToString.Exclude` / `@EqualsAndHashCode.Exclude` cho các quan hệ hai chiều tránh vòng lặp đệ quy vô tận.
3. **Lombok & Java 21 / Spring Boot 3.3:**
   - Sử dụng `@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`.
   - Cấu hình tương thích `Lombok 1.18.36` trong `pom.xml` để tương thích hoàn hảo với Java 21 / Java 24 của môi trường máy chủ.

---

## 3. Quy trình Git & Kế hoạch Thực hiện

1. **Tạo và chuyển sang nhánh mới trong repo `HoaDiemCac-backend`:**
   ```bash
   git checkout -b feat/domain-entities
   ```
2. **Triển khai mã nguồn:**
   - Cập nhật phiên bản Lombok trong `pom.xml` để đảm bảo build compile thành công.
   - Tạo toàn bộ 10 file Enum trong `src/main/java/com/hoadiemcat/entity/enums/`.
   - Tạo toàn bộ 11 file Entity trong `src/main/java/com/hoadiemcat/entity/`.
   - Kiểm tra biên dịch dự án bằng `mvnw compile -DskipTests`.
3. **Commit & Push theo Conventional Commits:**
   - Thêm các file thay đổi: `git add src/ pom.xml doc/`
   - Commit:
     ```bash
     git commit -m "feat(entity): implement production-ready JPA domain entities and enums with comprehensive documentation"
     ```
   - Push lên remote nhánh mới:
     ```bash
     git push -u origin feat/domain-entities
     ```
4. **Xác nhận và báo cáo kết quả** chi tiết qua walkthrough artifact.
