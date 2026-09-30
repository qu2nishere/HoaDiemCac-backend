# CHƯƠNG 3: MÔ HÌNH HÓA YÊU CẦU
## Hệ thống Order & Quản trị Nhà hàng qua Mã QR (DacTa)

> Tài liệu này dựa trên các yêu cầu chức năng đã xác định ở Chương 2 của **DacTa.docx** (Bảng 1–11, Quy định QĐ1–QĐ10), được mô hình hóa theo đúng cấu trúc trình bày của **Nhom02_FinalProject.docx** (Chương 3 – Mô hình hóa): Use Case tổng quát → Lược đồ Use Case chi tiết (kèm bảng chú thích) → Đặc tả Use Case chi tiết. Các lược đồ được cung cấp dưới dạng mã nguồn **PlantUML** thay cho hình ảnh.
>
> **4 actor chính:** Khách hàng (tại bàn, không cần tài khoản), Nhân viên Bếp (Kitchen/Bar), Nhân viên Phục vụ (Waiter), Quản trị viên & Quản lý (Admin / Manager).

---

## 3.1 Use Case Diagram

### 3.1.1 Use Case tổng quát

**Hình 1: Lược đồ use case tổng quát**

```plantuml
@startuml uc-tong-quat
left to right direction
skinparam packageStyle rectangle
skinparam usecase {
  BackgroundColor #CFE2F3
  BorderColor #3C78D8
}

actor "Khách hàng\n(tại bàn)" as KH
actor "Nhân viên Bếp" as NVB
actor "Nhân viên Phục vụ" as NVPV
actor "Quản lý / Admin" as QL

rectangle "Hệ thống Order & Quản trị Nhà hàng qua QR" {

  package "Chức năng Khách hàng" {
    usecase "UC01\nQuét mã QR vào bàn" as UC01
    usecase "UC02\nXem thực đơn" as UC02
    usecase "UC03\nXem chi tiết món ăn" as UC03
    usecase "UC04\nQuản lý giỏ hàng chung\n(Realtime Cart)" as UC04
    usecase "UC05\nGửi đơn gọi món" as UC05
    usecase "UC06\nTheo dõi trạng thái món ăn" as UC06
    usecase "UC07\nGọi nhân viên phục vụ" as UC07
    usecase "UC08\nYêu cầu thanh toán" as UC08
    usecase "UC33\nChuyển bàn (Transfer)" as UC33
    usecase "UC34\nGhép bàn & Cụm bàn (Merge)" as UC34
    usecase "UC35\nQuản lý thiết bị & Chủ bàn" as UC35
  }

  package "Chức năng dùng chung (Nhân sự)" {
    usecase "UC09\nĐăng nhập hệ thống quản trị" as UC09
    usecase "UC20\nĐăng xuất" as UC20
  }

  package "Chức năng Quản lý" {
    usecase "UC10\nQuản lý thực đơn" as UC10
    usecase "UC11\nQuản lý bàn ăn" as UC11
    usecase "UC12\nXem sơ đồ tổng quát bàn ăn" as UC12
    usecase "UC13\nCan thiệp & điều chỉnh order" as UC13
    usecase "UC14\nTiếp nhận gọi NV / thanh toán" as UC14
    usecase "UC15\nXác nhận thanh toán & đóng bàn" as UC15
    usecase "UC16\nXem thống kê doanh thu" as UC16
    usecase "UC36\nQuản lý nhân sự & phân quyền" as UC36
    usecase "UC37\nQuản lý hóa đơn & đối soát" as UC37
  }

  package "Chức năng Bếp" {
    usecase "UC17\nXem danh sách món cần chế biến" as UC17
    usecase "UC18\nCập nhật trạng thái chế biến món" as UC18
    usecase "UC19\nBáo hết món khẩn cấp" as UC19
  }

  package "Chức năng Phục vụ" {
    usecase "UC38\nTiếp nhận phục vụ & giao món" as UC38
  }
}

KH --> UC01
KH --> UC02
KH --> UC03
KH --> UC04
KH --> UC05
KH --> UC06
KH --> UC07
KH --> UC08
KH --> UC33
KH --> UC34
KH --> UC35

QL --> UC09
QL --> UC10
QL --> UC11
QL --> UC12
QL --> UC13
QL --> UC14
QL --> UC15
QL --> UC16
QL --> UC20
QL --> UC33
QL --> UC34
QL --> UC35
QL --> UC36
QL --> UC37

NVB --> UC09
NVB --> UC17
NVB --> UC18
NVB --> UC19
NVB --> UC20

NVPV --> UC09
NVPV --> UC14
NVPV --> UC15
NVPV --> UC20
NVPV --> UC38

UC05 ..> UC04 : <<include>>
UC08 ..> UC04 : <<include>>
UC14 ..> UC07 : <<extend>>
UC14 ..> UC08 : <<extend>>
UC38 ..> UC18 : <<extend>>
@enduml
```

**Bảng 1: Bảng Use Case tổng quát**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC01 | Quét mã QR vào bàn | Khách hàng | Khách quét mã QR dán tại bàn để xác thực phiên order (session_token) đúng bàn của mình. |
| UC02 | Xem thực đơn | Khách hàng | Duyệt danh sách món ăn theo danh mục (Nước lẩu, Thịt bò, Hải sản, Rau nấm, Thức uống, Bán chạy...). |
| UC03 | Xem chi tiết món ăn | Khách hàng | Xem ảnh phóng to, mô tả nguyên liệu/hương vị, giá và tình trạng còn/hết của một món cụ thể. |
| UC04 | Quản lý giỏ hàng chung (Realtime Cart) | Khách hàng | Phân hệ giỏ hàng dùng chung cho cả bàn, đồng bộ realtime; gồm thêm món (UC21), điều chỉnh số lượng (UC22), xem giỏ hàng & tổng tiền (UC23). |
| UC05 | Gửi đơn gọi món | Khách hàng | Chốt các món trong giỏ hàng, gửi một đợt order (Order Round) vào bếp. |
| UC06 | Theo dõi trạng thái món ăn | Khách hàng | Theo dõi realtime trạng thái từng món đã gọi (Đang chuẩn bị/Đã phục vụ/Đã hủy). |
| UC07 | Gọi nhân viên phục vụ | Khách hàng | Bấm chuông gọi nhân viên (xin nước đá, chén đũa, hỗ trợ...). |
| UC08 | Yêu cầu thanh toán | Khách hàng | Gửi yêu cầu thanh toán tới Quản lý kèm hóa đơn tạm tính của bàn. |
| UC09 | Đăng nhập hệ thống quản trị | Quản lý, Nhân viên Bếp, Phục vụ | Xác thực tài khoản nhân sự bằng Email/Username và mật khẩu để cấp JWT Token, phân quyền theo vai trò. |
| UC10 | Quản lý thực đơn | Quản lý | Phân hệ quản lý món ăn (UC24), danh mục (UC25) và trạng thái còn/hết hàng (UC26). |
| UC11 | Quản lý bàn ăn | Quản lý | Phân hệ quản lý danh sách bàn (UC27), cấp lại mã QR (UC28) và khóa/mở quyền order của bàn (UC29). |
| UC12 | Xem sơ đồ tổng quát bàn ăn | Quản lý | Xem bản đồ trực quan toàn bộ bàn (trạng thái màu sắc, thời gian ngồi, tạm tính). |
| UC13 | Can thiệp & điều chỉnh order khách hàng | Quản lý | Sửa số lượng, đổi món, hủy món trong order của khách khi có sự cố. |
| UC14 | Tiếp nhận yêu cầu gọi nhân viên/thanh toán | Quản lý, Phục vụ | Nhận và xử lý các cảnh báo chuông gọi phục vụ (UC07) hoặc yêu cầu thanh toán (UC08) từ khách. |
| UC15 | Xác nhận thanh toán & đóng bàn | Quản lý, Phục vụ | Phân hệ chốt hóa đơn cho bàn, gồm thanh toán tiền mặt (UC30), VietQR (UC31) và in hóa đơn & đóng bàn (UC32). |
| UC16 | Xem thống kê doanh thu | Quản lý | Xem báo cáo, biểu đồ doanh thu theo Giờ/Ngày/Tuần/Tháng và Top món bán chạy. |
| UC17 | Xem danh sách món cần chế biến | Nhân viên Bếp | Xem danh sách món cần nấu, sắp xếp theo nguyên tắc FIFO (thời gian gọi). |
| UC18 | Cập nhật trạng thái chế biến món | Nhân viên Bếp | Chuyển trạng thái món từ "Đang chuẩn bị" sang "Đã phục vụ". |
| UC19 | Báo hết món khẩn cấp | Nhân viên Bếp | Tắt nhanh một món khi cạn nguyên liệu giữa ca, khóa món trên thực đơn khách ngay lập tức. |
| UC20 | Đăng xuất | Quản lý, Nhân viên Bếp, Phục vụ | Đăng xuất khỏi hệ thống quản trị/KDS, xóa Token phiên hiện tại. |
| UC33 | Chuyển bàn (Table Transfer 1:1) | Khách hàng (Host), Quản lý | Chuyển toàn bộ đợt order và giỏ hàng từ bàn cũ sang bàn mới còn trống, áp dụng 2-Phase Lock. |
| UC34 | Ghép bàn & Cụm bàn (Table Merge N:1) | Khách hàng (Host), Quản lý | Ghép bàn phụ vào bàn chính cho đoàn tiệc lớn, hợp nhất hóa đơn thanh toán tập trung. |
| UC35 | Quản lý thiết bị & Phân quyền Chủ Bàn | Khách hàng (Host), Quản lý | Quét QR, nhập PIN 4 số, chống Brute-force, gán quyền Host, nhường Host và đá thiết bị lạ. |
| UC36 | Quản lý tài khoản nhân sự | Quản trị viên (Admin) | Thêm mới nhân viên, phân vai trò ADMIN/MANAGER/KITCHEN/STAFF, khóa tài khoản, gửi email. |
| UC37 | Quản lý hóa đơn & Đối soát | Quản lý | Tra cứu lịch sử hóa đơn theo ngày, phương thức thanh toán, xem chi tiết món ăn, in lại hóa đơn. |
| UC38 | Tiếp nhận phục vụ & giao món tại sảnh | Nhân viên Phục vụ | Theo dõi danh sách món nấu xong cần bưng ra bàn, xác nhận đã giao món, thu tiền mặt tại chỗ. |

---

### 3.1.2 Lược đồ Use Case chi tiết

#### 3.1.2.1 Lược đồ Giỏ hàng & Gọi món

**Hình 2: Lược đồ use case Giỏ hàng & Gọi món**

```plantuml
@startuml uc-gio-hang-goi-mon
title uc Gio hang & Goi mon

actor "Khách hàng\n(tại bàn)" as KH

usecase "Quản lý giỏ hàng chung\n(Realtime Cart)" as UC04
usecase "Thêm món vào giỏ hàng" as UC21
usecase "Điều chỉnh số lượng món\ntrong giỏ" as UC22
usecase "Xem giỏ hàng &\ntổng hóa đơn tạm tính" as UC23

KH -- UC04
UC21 ..> UC04 : <<extend>>
UC22 ..> UC04 : <<extend>>
UC23 ..> UC04 : <<extend>>
@enduml
```

**Bảng 2: Bảng Use Case Giỏ hàng & Gọi món**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC21 | Thêm món vào giỏ hàng | Khách hàng | Chọn món và số lượng từ thực đơn, thêm vào giỏ hàng chung của bàn. |
| UC22 | Điều chỉnh số lượng món trong giỏ | Khách hàng | Tăng/giảm hoặc xóa một dòng món chưa gửi bếp trong giỏ hàng. |
| UC23 | Xem giỏ hàng & tổng hóa đơn tạm tính | Khách hàng | Xem toàn bộ món (đã gửi + chưa gửi) và tổng tiền tạm tính của cả bàn. |

---

#### 3.1.2.2 Lược đồ Quản lý thực đơn

**Hình 3: Lược đồ use case Quản lý thực đơn**

```plantuml
@startuml uc-quan-ly-thuc-don
title uc Quan ly thuc don

actor "Quản lý" as QL

usecase "Quản lý thực đơn" as UC10
usecase "Quản lý danh sách món ăn" as UC24
usecase "Quản lý danh mục món ăn" as UC25
usecase "Cập nhật trạng thái\nCòn hàng / Hết hàng" as UC26

QL -- UC10
UC24 ..> UC10 : <<extend>>
UC25 ..> UC10 : <<extend>>
UC26 ..> UC10 : <<extend>>
@enduml
```

**Bảng 3: Bảng Use Case Quản lý thực đơn**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC24 | Quản lý danh sách món ăn | Quản lý | Thêm/sửa/xóa món ăn: tên, mô tả, giá, ảnh minh họa. |
| UC25 | Quản lý danh mục món ăn | Quản lý | Thêm/sửa/xóa danh mục và gán món ăn vào một hoặc nhiều danh mục (N-N). |
| UC26 | Cập nhật trạng thái Còn hàng/Hết hàng | Quản lý | Bật/tắt nhanh trạng thái còn hàng của một món, đồng bộ realtime tới khách hàng. |

---

#### 3.1.2.3 Lược đồ Quản lý bàn ăn & Mã QR

**Hình 4: Lược đồ use case Quản lý bàn ăn & Mã QR**

```plantuml
@startuml uc-quan-ly-ban-an
title uc Quan ly ban an & Ma QR

actor "Quản lý" as QL

usecase "Quản lý bàn ăn" as UC11
usecase "Quản lý danh sách bàn ăn" as UC27
usecase "Cấp lại mã QR cho bàn" as UC28
usecase "Bật/tắt quyền order của bàn" as UC29

QL -- UC11
UC27 ..> UC11 : <<extend>>
UC28 ..> UC11 : <<extend>>
UC29 ..> UC11 : <<extend>>
@enduml
```

**Bảng 4: Bảng Use Case Quản lý bàn ăn & Mã QR**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC27 | Quản lý danh sách bàn ăn | Quản lý | Thêm/sửa/xóa bàn ăn: mã bàn, tên hiển thị, khu vực, sức chứa tối đa. |
| UC28 | Cấp lại mã QR cho bàn | Quản lý | Thu hồi token cũ và sinh token/QR mới cho một hoặc nhiều bàn (khi nghi lộ QR). |
| UC29 | Bật/tắt quyền order của bàn | Quản lý | Khóa/mở cờ is_order_locked để ngăn bàn gọi thêm món khi có sự cố. |

---

#### 3.1.2.4 Lược đồ Thanh toán & Đóng bàn

**Hình 5: Lược đồ use case Thanh toán & Đóng bàn**

```plantuml
@startuml uc-thanh-toan-dong-ban
title uc Thanh toan & Dong ban

actor "Quản lý" as QL

usecase "Xác nhận thanh toán\n& đóng bàn" as UC15
usecase "Thanh toán bằng tiền mặt" as UC30
usecase "Thanh toán bằng VietQR" as UC31
usecase "In hóa đơn & đóng bàn" as UC32

QL -- UC15
UC30 ..> UC15 : <<extend>>
UC31 ..> UC15 : <<extend>>
UC32 ..> UC15 : <<extend>>
UC32 ..> UC30 : <<include>>
UC32 ..> UC31 : <<include>>
@enduml
```

**Bảng 5: Bảng Use Case Thanh toán & Đóng bàn**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC30 | Thanh toán bằng tiền mặt | Quản lý | Ghi nhận giao dịch thanh toán bằng tiền mặt cho hóa đơn của bàn. |
| UC31 | Thanh toán bằng VietQR | Quản lý | Sinh mã VietQR động (chuẩn Napas247) đúng số tiền hóa đơn để khách quét chuyển khoản. |
| UC32 | In hóa đơn & đóng bàn | Quản lý | In hóa đơn nhiệt khổ 80mm, lưu doanh thu, chuyển bàn sang trạng thái dọn dẹp và thu hồi mã QR hiện tại. |

---

#### 3.1.2.5 Lược đồ Điều chuyển bàn & Cụm bàn

**Hình 6: Lược đồ use case Điều chuyển bàn & Cụm bàn**

```plantuml
@startuml uc-chuyen-ghep-ban
title uc Dieu chuyen ban & Cum ban

actor "Khách hàng (Host)" as KH
actor "Quản lý / Thu ngân" as QL

usecase "Chuyển bàn (Transfer 1:1)" as UC33
usecase "Ghép bàn & Cụm bàn (Merge N:1)" as UC34

KH -- UC33
KH -- UC34
QL -- UC33
QL -- UC34

UC34 ..> UC33 : <<extend>>
@enduml
```

**Bảng 6: Bảng Use Case Điều chuyển bàn & Cụm bàn**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC33 | Chuyển bàn (Table Move / Transfer 1:1) | Khách hàng (Host), Quản lý | Di chuyển toàn bộ đợt order và giỏ hàng từ bàn cũ sang bàn mới trống, áp dụng khóa tạm thời 2 giai đoạn (2-Phase Lock). |
| UC34 | Ghép bàn & Cụm bàn (Table Merge & Cluster N:1) | Khách hàng (Host), Quản lý | Ghép bàn phụ (Slave) vào bàn chính (Master) cho đoàn tiệc đông người, gom chung order và thanh toán tập trung. |

---

#### 3.1.2.6 Lược đồ Quản lý thiết bị & Phiên bàn ăn

**Hình 7: Lược đồ use case Quản lý thiết bị & Phiên bàn ăn**

```plantuml
@startuml uc-quan-ly-thiet-bi
title uc Quan ly thiet bi & Phien ban an

actor "Khách hàng (Host)" as KH
actor "Quản lý / Admin" as QL

usecase "Quản lý thiết bị & Chủ bàn" as UC35

KH -- UC35
QL -- UC35
@enduml
```

**Bảng 7: Bảng Use Case Quản lý thiết bị & Phiên bàn ăn**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC35 | Quản lý thiết bị & Phân quyền Chủ bàn | Khách hàng (Host), Quản lý | Xác thực PIN 4 số chống Brute-force (khóa 60s), phân quyền Chủ Bàn (Host) vs Thành viên (Member), đá thiết bị lạ và nhường Host. |

---

#### 3.1.2.7 Lược đồ Quản trị nhân sự, Hóa đơn & Phục vụ sảnh

**Hình 8: Lược đồ use case Quản trị nhân sự, Hóa đơn & Phục vụ sảnh**

```plantuml
@startuml uc-nhan-su-hoa-don-phuc-vu
title uc Quan tri nhan su, Hoa don & Phuc vu

actor "Quản trị viên (Admin)" as ADMIN
actor "Quản lý" as QL
actor "Nhân viên Phục vụ" as NVPV

usecase "Quản lý tài khoản nhân sự" as UC36
usecase "Quản lý hóa đơn & đối soát" as UC37
usecase "Tiếp nhận phục vụ & giao món" as UC38

ADMIN -- UC36
QL -- UC37
NVPV -- UC38
@enduml
```

**Bảng 8: Bảng Use Case Quản trị nhân sự, Hóa đơn & Phục vụ sảnh**

| Mã UC | Tên Use Case | Actor chính | Mô tả ngắn |
|---|---|---|---|
| UC36 | Quản lý tài khoản nhân sự | Quản trị viên (Admin) | Quản lý hồ sơ nhân viên, phân quyền ADMIN/MANAGER/KITCHEN/STAFF, bật/tắt trạng thái, gửi email chào mừng. |
| UC37 | Quản lý hóa đơn & Đối soát doanh thu | Quản lý | Tra cứu lịch sử hóa đơn theo ngày, lọc theo trạng thái/phương thức, xem chi tiết đợt order của bàn. |
| UC38 | Tiếp nhận phục vụ & giao món tại sảnh | Nhân viên Phục vụ | Tiếp nhận chuông gọi phục vụ, theo dõi món nấu xong từ bếp và xác nhận bưng ra bàn, thu tiền mặt. |

---

## 3.2 Đặc tả Use Case

### 3.2.1 UC01: Quét mã QR vào bàn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC01 |
| Use Case Name | Quét mã QR vào bàn |
| Description | Là khách hàng, tôi muốn quét mã QR dán tại bàn để vào đúng phiên order của bàn mình mà không cần đăng ký tài khoản. |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng dùng camera điện thoại quét mã QR dán tại bàn. |
| Pre-Condition(s) | Mã QR của bàn còn hiệu lực (chưa bị thu hồi). |
| Post-Condition(s) | Trình duyệt được xác thực đúng bàn qua session_token; trang thực đơn/order của đúng bàn hiển thị; nếu bàn đang trống thì chuyển sang trạng thái "Đang có khách". |
| Basic Flow | 1. Khách hàng quét mã QR, mã chứa URL dạng `https://domain.com/order?table={id}&token={session_token}`.<br>2. Trình duyệt điều hướng tới URL, gửi yêu cầu xác thực token với server.<br>3. Hệ thống đối chiếu token với current_session_token đang hiệu lực của bàn.<br>4. Nếu bàn đang ở trạng thái "Bàn trống" (AVAILABLE), hệ thống tự chuyển sang "Đang có khách" (OCCUPIED) và ghi nhận thời điểm mở bàn.<br>5. Hệ thống trả về thông tin phiên (giỏ hàng chung hiện tại nếu có) và mở trang thực đơn (chuyển UC02). |
| Alternative Flow | 4a. Nếu bàn đã ở trạng thái "Đang có khách" (một người trong nhóm đã mở trước), hệ thống bỏ qua bước chuyển trạng thái và cho khách tham gia thẳng vào phiên hiện có, đồng bộ giỏ hàng chung đang có. |
| Exception Flow | 3a. Token không khớp hoặc đã bị thu hồi (do Quản lý cấp lại QR hoặc bàn đã đóng trước đó).<br>- 3a1. Hệ thống hiển thị thông báo "Mã QR không còn hiệu lực, vui lòng liên hệ nhân viên".<br>- Use Case dừng lại. |
| Business Rules | - BR01.1: Vòng đời mã QR và session token tuân theo QĐ2.<br>- BR01.2: Trạng thái bàn tuân theo QĐ7 (Available/Occupied/Cleaning). |
| Non-Functional Requirement | - NFR01.1: Thời gian xác thực và tải trang order dưới 2 giây. |

### 3.2.2 UC02: Xem thực đơn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC02 |
| Use Case Name | Xem thực đơn |
| Description | Là khách hàng, tôi muốn duyệt thực đơn theo danh mục để chọn món phù hợp. |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng vào tab "Thực đơn" sau khi vào phiên order (UC01). |
| Pre-Condition(s) | Phiên bàn hợp lệ (đã qua UC01). |
| Post-Condition(s) | Danh sách món ăn theo danh mục được hiển thị; món hết hàng hiển thị mờ và không thể thêm. |
| Basic Flow | 1. Khách hàng vào tab "Thực đơn".<br>2. Hệ thống lấy danh sách danh mục và món ăn kèm ảnh, giá, trạng thái còn/hết.<br>3. Giao diện hiển thị món theo từng danh mục dạng tab cuộn (Nước lẩu, Thịt bò, Hải sản, Rau nấm, Thức uống, Bán chạy...).<br>4. Khách hàng chuyển tab danh mục để xem các nhóm món khác nhau. |
| Alternative Flow | 3a. Khách hàng chọn danh mục "Bán chạy" (Best Seller) → hệ thống hiển thị Top món có lượt gọi cao nhất trong 7 ngày gần nhất. |
| Exception Flow | 2a. Lỗi kết nối tới server.<br>- 2a1. Hệ thống hiển thị "Không tải được thực đơn, vui lòng thử lại".<br>- Use Case dừng lại. |
| Business Rules | - BR02.1: Danh mục "Bán chạy" là danh mục hệ thống tự tổng hợp theo QĐ5, không do Quản lý gán thủ công. |
| Non-Functional Requirement | - NFR02.1: Giao diện mobile-first, tối ưu cuộn theo tab danh mục. |

### 3.2.3 UC03: Xem chi tiết món ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC03 |
| Use Case Name | Xem chi tiết món ăn |
| Description | Là khách hàng, tôi muốn xem chi tiết một món ăn trước khi quyết định gọi món. |
| Actor(s) | Khách hàng |
| Priority | Should have |
| Trigger | Khách hàng chạm vào một món trong danh sách thực đơn (UC02). |
| Pre-Condition(s) | Đã ở màn hình thực đơn. |
| Post-Condition(s) | Modal/trang chi tiết hiển thị ảnh phóng to, mô tả nguyên liệu/hương vị, giá và trạng thái còn/hết hàng. |
| Basic Flow | 1. Khách hàng chạm vào món ăn muốn xem.<br>2. Hệ thống lấy dữ liệu chi tiết của món.<br>3. Hiển thị chi tiết (ảnh lớn, mô tả, giá, nhãn Best Seller nếu có).<br>4. Khách hàng có thể thêm món vào giỏ ngay từ đây nếu món còn hàng (chuyển UC21). |
| Alternative Flow | Không có |
| Exception Flow | 4a. Món vừa chuyển sang hết hàng trong lúc xem chi tiết (do bếp/Quản lý cập nhật realtime).<br>- 4a1. Nút "Thêm" tự động bị vô hiệu hóa, hiển thị "Món vừa hết hàng".<br>- Use Case dừng lại. |
| Business Rules | - BR03.1: Tuân theo QĐ4 (quy định Còn hàng/Hết hàng). |
| Non-Functional Requirement | - NFR03.1: Ảnh tải theo cơ chế lazy-load để không làm chậm trang khi nhiều ảnh chất lượng cao. |

### 3.2.4 UC04: Quản lý giỏ hàng chung (Realtime Cart)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC04 |
| Use Case Name | Quản lý giỏ hàng chung (Realtime Cart) |
| Description | Là khách hàng, tôi muốn cả bàn cùng thao tác trên một giỏ hàng chung, đồng bộ tức thì trên mọi thiết bị; gồm thêm món (UC21), điều chỉnh số lượng (UC22), xem giỏ hàng & tổng tiền (UC23). |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng chọn biểu tượng giỏ hàng hoặc thêm món từ thực đơn. |
| Pre-Condition(s) | Phiên bàn hợp lệ. |
| Post-Condition(s) | Giỏ hàng được đồng bộ tức thì trên mọi thiết bị đang mở cùng phiên bàn. |
| Basic Flow | 1. Khách hàng thao tác trên giỏ hàng (thêm/sửa số lượng/xem).<br>2. Hệ thống phát sự kiện WebSocket cập nhật giỏ hàng tới toàn bộ thiết bị đang kết nối phiên bàn.<br>3. Mọi thiết bị nhận cập nhật realtime, hiển thị đồng nhất. |
| Alternative Flow | Không có |
| Exception Flow | 2a. Mất kết nối realtime tạm thời.<br>- 2a1. Thiết bị tự động kết nối lại và đồng bộ lại giỏ hàng khi có mạng trở lại. |
| Business Rules | - BR04.1: Giỏ hàng chỉ tồn tại tạm thời (các món chưa gửi bếp) và bị xóa khi phiên bàn đóng (UC32). |
| Non-Functional Requirement | - NFR04.1: Độ trễ đồng bộ giữa các thiết bị trong cùng bàn dưới 1 giây. |

### 3.2.5 UC05: Gửi đơn gọi món

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC05 |
| Use Case Name | Gửi đơn gọi món |
| Description | Là khách hàng, tôi muốn gửi các món đã chọn trong giỏ hàng vào bếp thành một đợt order. |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng nhấn "Gửi đơn vào bếp" sau khi đã chọn món trong giỏ hàng (UC04). |
| Pre-Condition(s) | Giỏ hàng có ít nhất một món; bàn không bị khóa order (is_order_locked = false). |
| Post-Condition(s) | Một Order Round mới được tạo với từng món ở trạng thái "Đang chuẩn bị"; bếp nhận được đơn theo thời gian thực. |
| Basic Flow | 1. Khách hàng xem lại giỏ hàng, nhấn "Gửi đơn vào bếp".<br>2. Hệ thống kiểm tra cờ is_order_locked của bàn.<br>3. Hệ thống tạo Order Round mới (số thứ tự đợt tăng dần), gán từng món trạng thái "Đang chuẩn bị", ghi nhận đơn giá tại thời điểm gọi.<br>4. Hệ thống trừ tồn kho tức thời tương ứng.<br>5. Hệ thống phát sự kiện realtime kèm âm thanh "Ting ting" tới màn hình Bếp và Quản lý.<br>6. Giỏ hàng tạm được làm trống, khách chuyển sang theo dõi trạng thái (UC06). |
| Alternative Flow | Không có |
| Exception Flow | 2a. Bàn đang bị khóa order (is_order_locked = true).<br>- 2a1. Hệ thống từ chối với lỗi "Bàn hiện không thể gọi thêm món" (HTTP 403).<br>- Use Case dừng lại.<br>4a. Một món trong giỏ vừa chuyển hết hàng trước khi gửi.<br>- 4a1. Hệ thống loại món đó khỏi đơn và yêu cầu khách xác nhận lại trước khi gửi. |
| Business Rules | - BR05.1: Tuân theo QĐ3 (khóa order), QĐ4 (hết hàng) và QĐ9 (giới hạn số lượng 1–99 mỗi món). |
| Non-Functional Requirement | - NFR05.1: Thời gian xử lý gửi đơn và phát tín hiệu tới bếp dưới 1 giây. |

### 3.2.6 UC06: Theo dõi trạng thái món ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC06 |
| Use Case Name | Theo dõi trạng thái món ăn |
| Description | Là khách hàng, tôi muốn theo dõi realtime món nào đang được nấu và món nào đã mang ra bàn. |
| Actor(s) | Khách hàng |
| Priority | Should have |
| Trigger | Khách hàng mở tab "Đơn của tôi" sau khi đã gửi ít nhất một đơn (UC05). |
| Pre-Condition(s) | Đã có ít nhất một Order Round trong phiên bàn. |
| Post-Condition(s) | Trạng thái realtime từng món (Đang chuẩn bị/Đã phục vụ/Đã hủy) được hiển thị. |
| Basic Flow | 1. Khách hàng mở tab theo dõi đơn.<br>2. Hệ thống lấy toàn bộ Order Round của phiên bàn hiện tại, nhóm theo đợt gọi.<br>3. Khi bếp cập nhật trạng thái món (UC18) hoặc Quản lý hủy món (UC13), hệ thống đẩy realtime cập nhật giao diện khách ngay lập tức. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR06.1: Tuân theo QĐ8 (trạng thái món: Đang chuẩn bị/Đã phục vụ/Đã hủy). |
| Non-Functional Requirement | - NFR06.1: Cập nhật trạng thái hiển thị tức thời (dưới 1 giây), không cần tải lại trang. |

### 3.2.7 UC07: Gọi nhân viên phục vụ

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC07 |
| Use Case Name | Gọi nhân viên phục vụ |
| Description | Là khách hàng, tôi muốn bấm chuông gọi nhân viên khi cần hỗ trợ (xin nước đá, chén đũa...). |
| Actor(s) | Khách hàng |
| Priority | Should have |
| Trigger | Khách hàng nhấn nút chuông "Gọi nhân viên". |
| Pre-Condition(s) | Phiên bàn hợp lệ. |
| Post-Condition(s) | Thông báo kèm âm thanh được gửi tới màn hình Quản lý (UC14). |
| Basic Flow | 1. Khách hàng nhấn nút gọi nhân viên (có thể chọn lý do nhanh: xin nước đá/chén đũa/khác).<br>2. Hệ thống kiểm tra thời gian lần gọi gần nhất của bàn.<br>3. Hệ thống ghi log yêu cầu và phát sự kiện realtime kèm âm thanh "Ting ting" tới màn hình Quản lý.<br>4. Giao diện khách hiển thị "Đã gửi yêu cầu, nhân viên đang đến!". |
| Alternative Flow | Không có |
| Exception Flow | 2a. Khách bấm liên tục trong vòng 30 giây kể từ lần gọi trước.<br>- 2a1. Hệ thống không tạo yêu cầu mới, chỉ hiển thị lại thông báo "Yêu cầu của bạn đã được gửi, nhân viên đang đến!".<br>- Use Case dừng lại. |
| Business Rules | - BR07.1: Tuân theo QĐ10 (Rate Limiting 30 giây/lần gọi). |
| Non-Functional Requirement | - NFR07.1: Âm thanh và hiệu ứng cảnh báo phải đủ nổi bật để nhân viên không bỏ sót giữa ca đông khách. |

### 3.2.8 UC08: Yêu cầu thanh toán

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC08 |
| Use Case Name | Yêu cầu thanh toán |
| Description | Là khách hàng, tôi muốn báo cho Quản lý biết bàn tôi đã sẵn sàng thanh toán. |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng nhấn nút "Yêu cầu thanh toán". |
| Pre-Condition(s) | Bàn đang có ít nhất một Order Round đã gửi bếp. |
| Post-Condition(s) | Yêu cầu thanh toán được gửi tới Quản lý kèm tổng tiền tạm tính (UC14). |
| Basic Flow | 1. Khách hàng xem tổng hóa đơn tạm tính (UC23) và chọn phương thức mong muốn (chỉ là nguyện vọng tham khảo, việc xác nhận cuối do Quản lý thực hiện ở UC15).<br>2. Khách hàng nhấn "Yêu cầu thanh toán".<br>3. Hệ thống ghi log yêu cầu, phát sự kiện realtime kèm âm thanh tới màn hình Quản lý.<br>4. Giao diện khách hiển thị "Yêu cầu thanh toán đã được gửi, vui lòng chờ nhân viên". |
| Alternative Flow | Không có |
| Exception Flow | 3a. Bàn chưa có món nào được gửi bếp (chỉ có giỏ hàng tạm chưa order).<br>- 3a1. Hệ thống từ chối, báo "Bàn chưa có đơn gọi món nào để thanh toán".<br>- Use Case dừng lại. |
| Business Rules | - BR08.1: Tổng tiền tạm tính tuân theo công thức QĐ1. |
| Non-Functional Requirement | - NFR08.1: Tổng tiền tạm tính hiển thị phải khớp chính xác với công thức tính hóa đơn cuối cùng (không sai lệch làm tròn). |

### 3.2.9 UC09: Đăng nhập hệ thống quản trị

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC09 |
| Use Case Name | Đăng nhập hệ thống quản trị |
| Description | Là Quản lý/Nhân viên Bếp, tôi muốn đăng nhập để được cấp quyền truy cập đúng phân hệ của mình (Dashboard quản trị hoặc màn hình điều phối bếp KDS). |
| Actor(s) | Quản lý, Nhân viên Bếp |
| Priority | Must have |
| Trigger | Người dùng mở trang đăng nhập, nhập tài khoản. |
| Pre-Condition(s) | Tài khoản đã tồn tại trong hệ thống. |
| Post-Condition(s) | Người dùng được cấp JWT Token kèm vai trò và truy cập đúng phân hệ tương ứng. |
| Basic Flow | 1. Người dùng nhập username/email và mật khẩu, nhấn "Đăng nhập".<br>2. Hệ thống kiểm tra thông tin, đối chiếu với mật khẩu đã băm trong Database.<br>3. Hệ thống cấp JWT Token kèm vai trò (Manager/Kitchen).<br>4. Hệ thống chuyển hướng vào Dashboard quản trị (Quản lý) hoặc màn hình KDS (Nhân viên Bếp) tương ứng. |
| Alternative Flow | Không có |
| Exception Flow | 2a. Sai tài khoản hoặc mật khẩu.<br>- 2a1. Hệ thống báo lỗi "Thông tin đăng nhập không chính xác".<br>- Use Case dừng lại. |
| Business Rules | - BR09.1: Phân quyền RBAC (Manager/Kitchen) được áp dụng nghiêm ngặt tại tầng API. |
| Non-Functional Requirement | - NFR09.1: Thời gian xác thực không quá 2 giây. |

### 3.2.10 UC10: Quản lý thực đơn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC10 |
| Use Case Name | Quản lý thực đơn |
| Description | Là quản lý, tôi muốn truy cập phân hệ quản lý thực đơn. Phân hệ gồm: quản lý danh sách món ăn (UC24), quản lý danh mục (UC25) và cập nhật trạng thái còn/hết hàng (UC26). |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý chọn mục "Quản lý thực đơn" trên Dashboard. |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Giao diện tổng hợp thực đơn (món ăn + danh mục) được hiển thị. |
| Basic Flow | 1. Quản lý chọn "Quản lý thực đơn".<br>2. Hệ thống tải danh sách món ăn và danh mục hiện có.<br>3. Quản lý chọn tiếp thao tác con: UC24, UC25 hoặc UC26. |
| Alternative Flow | Không có |
| Exception Flow | 2a. Tài khoản không có role Manager.<br>- 2a1. Hệ thống báo lỗi "Bạn không có quyền truy cập".<br>- Use Case dừng lại. |
| Business Rules | Không có (xem quy định riêng của từng UC con). |
| Non-Functional Requirement | - NFR10.1: Danh sách hỗ trợ tìm kiếm/lọc theo tên món hoặc danh mục. |

### 3.2.11 UC11: Quản lý bàn ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC11 |
| Use Case Name | Quản lý bàn ăn |
| Description | Là quản lý, tôi muốn truy cập phân hệ quản lý bàn ăn. Phân hệ gồm: quản lý danh sách bàn (UC27), cấp lại mã QR (UC28) và bật/tắt quyền order (UC29). |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý chọn mục "Quản lý bàn ăn" trên Dashboard. |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Danh sách bàn kèm trạng thái, mã QR, cờ khóa order được hiển thị. |
| Basic Flow | 1. Quản lý chọn "Quản lý bàn ăn".<br>2. Hệ thống tải danh sách bàn hiện có kèm trạng thái vận hành.<br>3. Quản lý chọn tiếp thao tác con: UC27, UC28 hoặc UC29. |
| Alternative Flow | Không có |
| Exception Flow | 2a. Tài khoản không có role Manager.<br>- 2a1. Hệ thống báo lỗi "Bạn không có quyền truy cập".<br>- Use Case dừng lại. |
| Business Rules | Không có (xem quy định riêng của từng UC con). |
| Non-Functional Requirement | - NFR11.1: Hiển thị trạng thái bàn realtime, đồng bộ với sơ đồ tổng quát (UC12). |

### 3.2.12 UC12: Xem sơ đồ tổng quát bàn ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC12 |
| Use Case Name | Xem sơ đồ tổng quát bàn ăn |
| Description | Là quản lý, tôi muốn xem toàn cảnh mặt bằng các bàn ăn để nắm nhanh tình hình vận hành. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý mở trang Dashboard chính/"Sơ đồ bàn ăn". |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Bản đồ trực quan toàn bộ bàn (màu sắc theo trạng thái, thời gian ngồi, tạm tính) được hiển thị. |
| Basic Flow | 1. Quản lý mở trang sơ đồ bàn.<br>2. Hệ thống lấy trạng thái tất cả bàn (Trống/Đang có khách/Đang dọn dẹp), thời điểm mở bàn và tổng tạm tính hiện tại của từng bàn đang có khách.<br>3. Giao diện hiển thị mặt bằng bàn bằng màu sắc phân biệt theo trạng thái.<br>4. Quản lý bấm vào một bàn để xem chi tiết order (UC13) hoặc xác nhận thanh toán (UC15). |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR12.1: Tuân theo QĐ7 (3 trạng thái bàn cố định). |
| Non-Functional Requirement | - NFR12.1: Sơ đồ tự động làm mới (realtime) mỗi khi trạng thái bàn thay đổi, không cần tải lại trang. |

### 3.2.13 UC13: Can thiệp & điều chỉnh order khách hàng

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC13 |
| Use Case Name | Can thiệp & điều chỉnh order khách hàng |
| Description | Là quản lý, tôi muốn sửa số lượng, đổi món hoặc hủy món trong order của khách khi xảy ra sự cố (hết nguyên liệu đột xuất, khách đổi ý). |
| Actor(s) | Quản lý |
| Priority | Should have |
| Trigger | Từ sơ đồ bàn (UC12) hoặc danh sách bếp (UC17), Quản lý chọn một bàn/đơn để chỉnh sửa. |
| Pre-Condition(s) | Tài khoản có role Manager; order đang tồn tại và chưa thanh toán xong. |
| Post-Condition(s) | Order được điều chỉnh (sửa số lượng, đổi món, hủy món kèm lý do), có ghi log can thiệp. |
| Basic Flow | 1. Quản lý mở chi tiết order của bàn.<br>2. Quản lý chọn món cần sửa/hủy, nhập lý do nếu hủy.<br>3. Hệ thống cập nhật Database, ghi log Audit (ai can thiệp, lúc nào, lý do).<br>4. Hệ thống phát realtime cập nhật cho khách hàng (UC06) và bếp (nếu món đang chờ nấu). |
| Alternative Flow | Không có |
| Exception Flow | 3a. Món đã ở trạng thái "Đã phục vụ".<br>- 3a1. Hệ thống không cho phép hủy món, chỉ cho phép ghi chú điều chỉnh hóa đơn thủ công riêng.<br>- Use Case dừng lại. |
| Business Rules | - BR13.1: Mọi can thiệp phải được ghi vào Nhật ký hỗ trợ & Can thiệp (Audit & Call Staff Logs). |
| Non-Functional Requirement | - NFR13.1: Thao tác chỉnh sửa phải phản ánh ngay trên hóa đơn tạm tính của bàn. |

### 3.2.14 UC14: Tiếp nhận yêu cầu gọi nhân viên/thanh toán

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC14 |
| Use Case Name | Tiếp nhận yêu cầu gọi nhân viên/thanh toán |
| Description | Là quản lý, tôi muốn nhận và xử lý các cảnh báo chuông gọi phục vụ hoặc yêu cầu thanh toán từ khách hàng theo thời gian thực. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Hệ thống nhận sự kiện realtime từ UC07 (Gọi nhân viên) hoặc UC08 (Yêu cầu thanh toán). |
| Pre-Condition(s) | Tài khoản có role Manager đang mở Dashboard. |
| Post-Condition(s) | Yêu cầu được đánh dấu đã tiếp nhận/xử lý. |
| Basic Flow | 1. Hệ thống phát âm thanh và hiển thị thẻ cảnh báo (số bàn, loại yêu cầu) trên Dashboard.<br>2. Quản lý xác nhận đã tiếp nhận (bấm "Đã xử lý").<br>3. Hệ thống ẩn cảnh báo và ghi log thời gian phản hồi. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR14.1: Yêu cầu được lưu vào Nhật ký hỗ trợ & Can thiệp để thống kê thời gian phản hồi. |
| Non-Functional Requirement | - NFR14.1: Âm thanh cảnh báo phải phát liên tục cho đến khi được xác nhận. |

### 3.2.15 UC15: Xác nhận thanh toán & đóng bàn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC15 |
| Use Case Name | Xác nhận thanh toán & đóng bàn |
| Description | Là quản lý, tôi muốn truy cập phân hệ chốt hóa đơn cho một bàn. Phân hệ gồm: thanh toán tiền mặt (UC30), thanh toán VietQR (UC31) và in hóa đơn & đóng bàn (UC32). |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Nhận yêu cầu thanh toán (UC14) hoặc Quản lý chủ động chọn từ sơ đồ bàn (UC12). |
| Pre-Condition(s) | Tài khoản có role Manager; bàn đang "Đang có khách" và có ít nhất một Order Round. |
| Post-Condition(s) | Hóa đơn được lập, bàn chuyển sang "Đang dọn dẹp", mã QR cũ bị thu hồi. |
| Basic Flow | 1. Quản lý mở màn hình thanh toán của bàn, hệ thống tính tổng hóa đơn theo QĐ1.<br>2. Quản lý chọn phương thức (chuyển UC30 hoặc UC31).<br>3. Sau khi xác nhận đã thu tiền, hệ thống chuyển sang UC32 để in hóa đơn & đóng bàn. |
| Alternative Flow | Không có |
| Exception Flow | 1a. Có món đang ở trạng thái "Đang chuẩn bị" chưa phục vụ xong.<br>- 1a1. Hệ thống cảnh báo, yêu cầu Quản lý xác nhận trước khi cho phép thanh toán. |
| Business Rules | Không có (xem quy định riêng của từng UC con). |
| Non-Functional Requirement | - NFR15.1: Toàn bộ quy trình thanh toán – đóng bàn phải hoàn tất trong dưới 1 phút thao tác. |

### 3.2.16 UC16: Xem thống kê doanh thu

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC16 |
| Use Case Name | Xem thống kê doanh thu |
| Description | Là quản lý, tôi muốn xem báo cáo, biểu đồ doanh thu để đánh giá hiệu quả kinh doanh. |
| Actor(s) | Quản lý |
| Priority | Should have |
| Trigger | Quản lý chọn mục "Dashboard doanh thu". |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Biểu đồ và bảng số liệu doanh thu (BM1) được hiển thị. |
| Basic Flow | 1. Quản lý chọn kỳ thống kê (Hôm nay/Tuần này/Tháng).<br>2. Hệ thống tính tổng doanh thu, tổng lượt bàn, doanh thu trung bình/bàn (QĐ6) và Top món bán chạy.<br>3. Hệ thống hiển thị biểu đồ cột/đường/tròn tương ứng. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR16.1: Doanh thu trung bình tính theo công thức QĐ6. |
| Non-Functional Requirement | - NFR16.1: Biểu đồ sử dụng thư viện trực quan (VD: Chart.js) tải qua CDN. |

### 3.2.17 UC17: Xem danh sách món cần chế biến

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC17 |
| Use Case Name | Xem danh sách món cần chế biến |
| Description | Là nhân viên bếp, tôi muốn xem danh sách món cần nấu theo đúng thứ tự thời gian gọi để không bỏ sót đơn. |
| Actor(s) | Nhân viên Bếp |
| Priority | Must have |
| Trigger | Nhân viên bếp mở màn hình điều phối bếp (KDS). |
| Pre-Condition(s) | Tài khoản có role Kitchen. |
| Post-Condition(s) | Danh sách món cần nấu hiển thị theo nguyên tắc FIFO (thời gian gọi). |
| Basic Flow | 1. Nhân viên bếp mở màn hình KDS.<br>2. Hệ thống lấy toàn bộ món đang ở trạng thái "Đang chuẩn bị", sắp xếp theo thời điểm gọi tăng dần, kèm số bàn, đợt gọi và ghi chú món.<br>3. Hệ thống hiển thị bộ đếm giờ (elapsed time) cho từng món để cảnh báo món chờ lâu. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR17.1: Tuân theo QĐ8 (trạng thái món). |
| Non-Functional Requirement | - NFR17.1: Màn hình phải tự làm mới realtime khi có đơn mới, không cần thao tác thủ công. |

### 3.2.18 UC18: Cập nhật trạng thái chế biến món

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC18 |
| Use Case Name | Cập nhật trạng thái chế biến món |
| Description | Là nhân viên bếp, tôi muốn đánh dấu một món đã nấu xong để khách và Quản lý biết. |
| Actor(s) | Nhân viên Bếp |
| Priority | Must have |
| Trigger | Nhân viên bếp nấu xong một món, chạm vào thẻ món trên màn hình KDS. |
| Pre-Condition(s) | Món đang ở trạng thái "Đang chuẩn bị". |
| Post-Condition(s) | Món chuyển sang trạng thái "Đã phục vụ"; khách hàng và Quản lý thấy cập nhật realtime. |
| Basic Flow | 1. Nhân viên bếp chạm vào món đã hoàn thành.<br>2. Hệ thống cập nhật trạng thái món = "Đã phục vụ", ghi nhận thời điểm hoàn thành.<br>3. Hệ thống phát realtime cập nhật tới khách hàng (UC06) và Quản lý. |
| Alternative Flow | Không có |
| Exception Flow | 1a. Món đã bị Quản lý hủy trước đó (UC13).<br>- 1a1. Thẻ món tự động biến mất khỏi màn hình KDS, không cho thao tác.<br>- Use Case dừng lại. |
| Business Rules | - BR18.1: Tuân theo QĐ8 (trạng thái món). |
| Non-Functional Requirement | - NFR18.1: Thao tác cập nhật trạng thái chỉ cần một chạm (one-tap) để tối ưu tốc độ trong giờ cao điểm. |

### 3.2.19 UC19: Báo hết món khẩn cấp

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC19 |
| Use Case Name | Báo hết món khẩn cấp |
| Description | Là nhân viên bếp, tôi muốn tắt nhanh một món khi cạn nguyên liệu giữa ca để tránh khách tiếp tục đặt món đó. |
| Actor(s) | Nhân viên Bếp |
| Priority | Must have |
| Trigger | Nhân viên bếp phát hiện nguyên liệu của một món đã cạn. |
| Pre-Condition(s) | Tài khoản có role Kitchen. |
| Post-Condition(s) | Món chuyển trạng thái "Hết hàng"; khách hàng không thể thêm món đó nữa. |
| Basic Flow | 1. Nhân viên bếp chọn món cần báo hết trên màn hình KDS.<br>2. Nhân viên xác nhận "Báo hết hàng".<br>3. Hệ thống cập nhật trạng thái món = Hết hàng, phát sự kiện WebSocket broadcast tới toàn bộ khách hàng đang xem thực đơn.<br>4. Trên giao diện khách, nút "Thêm" của món đó bị vô hiệu hóa; món trong giỏ tạm (chưa gửi bếp) bị cảnh báo gỡ bỏ. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR19.1: Tuân theo QĐ4 (quy định Còn hàng/Hết hàng). |
| Non-Functional Requirement | - NFR19.1: Việc đồng bộ trạng thái hết hàng tới toàn bộ thiết bị khách phải tức thời (dưới 1 giây) để tránh đặt nhầm. |

### 3.2.20 UC20: Đăng xuất

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC20 |
| Use Case Name | Đăng xuất |
| Description | Là quản lý/nhân viên bếp, tôi muốn đăng xuất khỏi hệ thống để bảo mật phiên làm việc. |
| Actor(s) | Quản lý, Nhân viên Bếp |
| Priority | Must have |
| Trigger | Người dùng chọn nút "Đăng xuất". |
| Pre-Condition(s) | Người dùng đang trong trạng thái đã đăng nhập. |
| Post-Condition(s) | Phiên làm việc kết thúc. |
| Basic Flow | 1. Người dùng chọn "Đăng xuất".<br>2. Client xóa JWT Token khỏi bộ nhớ cục bộ.<br>3. Hệ thống chuyển hướng về trang đăng nhập. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR20.1: Token JWT đã cấp vẫn còn hiệu lực tới khi hết hạn tự nhiên (hệ thống hiện không triển khai blacklist/revocation phía server) — hạn chế đã biết, được đánh đổi bằng thời gian sống (TTL) của token đủ ngắn. |
| Non-Functional Requirement | - NFR20.1: Thời gian thực hiện đăng xuất phản hồi tức thời (dưới 1 giây). |

### 3.2.21 UC21: Thêm món vào giỏ hàng

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC21 |
| Use Case Name | Thêm món vào giỏ hàng |
| Description | Là khách hàng, tôi muốn thêm một món ăn với số lượng mong muốn vào giỏ hàng chung của bàn. |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng nhấn nút "Thêm" trên một món tại thực đơn (UC02) hoặc trang chi tiết món (UC03). |
| Pre-Condition(s) | Món đang ở trạng thái Còn hàng; bàn chưa bị khóa order. |
| Post-Condition(s) | Món được thêm vào giỏ hàng chung, đồng bộ tới mọi thiết bị trong bàn. |
| Basic Flow | 1. Khách hàng chọn món và số lượng mong muốn (mặc định 1), nhấn "Thêm vào giỏ".<br>2. Hệ thống kiểm tra món còn hàng và bàn chưa bị khóa order.<br>3. Hệ thống thêm/cộng dồn dòng món vào giỏ hàng chung của phiên bàn kèm ghi chú (nếu có).<br>4. Hệ thống phát sự kiện realtime cập nhật giỏ hàng tới toàn bộ thiết bị của bàn. |
| Alternative Flow | Không có |
| Exception Flow | 2a. Món vừa chuyển hết hàng ngay lúc thêm.<br>- 2a1. Hệ thống từ chối, báo "Món vừa hết hàng, vui lòng chọn món khác".<br>- Use Case dừng lại.<br>2b. Bàn đang bị khóa order.<br>- 2b1. Hệ thống báo "Bàn hiện không thể thêm món".<br>- Use Case dừng lại. |
| Business Rules | - BR21.1: Tuân theo QĐ9 (giới hạn số lượng 1–99 mỗi lần thêm). |
| Non-Functional Requirement | - NFR21.1: Đồng bộ giỏ hàng giữa các thiết bị trong bàn dưới 1 giây. |

### 3.2.22 UC22: Điều chỉnh số lượng món trong giỏ

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC22 |
| Use Case Name | Điều chỉnh số lượng món trong giỏ |
| Description | Là khách hàng, tôi muốn tăng/giảm số lượng hoặc xóa một món chưa gửi bếp trong giỏ hàng. |
| Actor(s) | Khách hàng |
| Priority | Should have |
| Trigger | Khách hàng mở giỏ hàng (UC23), chạm nút +/- hoặc nhập số lượng cho một dòng món chưa gửi bếp. |
| Pre-Condition(s) | Dòng món đang ở trạng thái "chưa gửi" (còn trong giỏ tạm, chưa qua UC05). |
| Post-Condition(s) | Số lượng dòng món được cập nhật (hoặc dòng bị xóa nếu về 0); tổng tiền tạm tính được tính lại. |
| Basic Flow | 1. Khách hàng mở giỏ hàng, chọn dòng món cần sửa.<br>2. Khách hàng tăng/giảm số lượng hoặc nhập trực tiếp.<br>3. Hệ thống kiểm tra giá trị hợp lệ (1 ≤ N ≤ 99).<br>4. Hệ thống cập nhật thành tiền dòng đó (đơn giá × số lượng) và tính lại tổng giỏ hàng.<br>5. Hệ thống phát realtime đồng bộ tới các thiết bị khác trong bàn. |
| Alternative Flow | 2a. Khách hàng giảm số lượng về 0 → dòng món bị xóa khỏi giỏ hàng. |
| Exception Flow | 3a. Người dùng nhập số âm, số 0 (ngoài thao tác xóa), số thập phân hoặc ký tự chữ.<br>- 3a1. Hệ thống từ chối và giữ nguyên giá trị cũ. |
| Business Rules | - BR22.1: Tuân theo QĐ9 (giới hạn số lượng). |
| Non-Functional Requirement | - NFR22.1: Thao tác +/- phải phản hồi tức thời trên giao diện (optimistic UI) trước khi xác nhận từ server. |

### 3.2.23 UC23: Xem giỏ hàng & tổng hóa đơn tạm tính

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC23 |
| Use Case Name | Xem giỏ hàng & tổng hóa đơn tạm tính |
| Description | Là khách hàng, tôi muốn xem toàn bộ món (đã gửi và chưa gửi) cùng tổng hóa đơn tạm tính của cả bàn. |
| Actor(s) | Khách hàng |
| Priority | Must have |
| Trigger | Khách hàng chạm biểu tượng giỏ hàng. |
| Pre-Condition(s) | Phiên bàn hợp lệ. |
| Post-Condition(s) | Danh sách món (chưa gửi + đã gửi các đợt trước) và tổng hóa đơn tạm tính toàn bàn được hiển thị. |
| Basic Flow | 1. Khách hàng mở giỏ hàng.<br>2. Hệ thống lấy toàn bộ dòng món trong giỏ tạm (chưa gửi) và toàn bộ Order Round đã gửi của phiên bàn.<br>3. Hệ thống tính tổng tiền tạm tính theo QĐ1 (chưa gồm chiết khấu/VAT cuối cùng).<br>4. Hệ thống hiển thị danh sách kèm trạng thái từng món (đã gửi: Đang chuẩn bị/Đã phục vụ; chưa gửi: có thể sửa/xóa) và nút "Gửi đơn" (UC05). |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR23.1: Tuân theo QĐ1 (công thức tính tổng tiền hóa đơn). |
| Non-Functional Requirement | - NFR23.1: Tổng tiền hiển thị phải cập nhật tức thời mỗi khi giỏ hàng hoặc trạng thái món thay đổi. |

### 3.2.24 UC24: Quản lý danh sách món ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC24 |
| Use Case Name | Quản lý danh sách món ăn |
| Description | Là quản lý, tôi muốn thêm/sửa/xóa món ăn trong thực đơn. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý chọn "Danh sách món ăn" trong Quản lý thực đơn (UC10). |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Món ăn được thêm mới/cập nhật/xóa (ẩn) trong hệ thống, đồng bộ ngay ra thực đơn khách. |
| Basic Flow | 1. Quản lý xem danh sách món (BM2), chọn "Thêm món" hoặc chọn một món để "Sửa"/"Xóa".<br>2. Với thêm/sửa: nhập tên món, mô tả, đơn giá, tải một ảnh minh họa, gán danh mục (UC25).<br>3. Hệ thống lưu vào Database (sinh mã món mới nếu là thêm mới).<br>4. Hệ thống phát realtime cập nhật thực đơn tới toàn bộ khách hàng đang xem. |
| Alternative Flow | Không có |
| Exception Flow | 3a. Thiếu trường bắt buộc (tên món, giá) hoặc ảnh sai định dạng/quá dung lượng.<br>- 3a1. Hệ thống từ chối lưu và báo lỗi cụ thể.<br>- Use Case dừng lại. |
| Business Rules | - BR24.1: Xóa món chỉ nên là "ẩn" (soft delete) để không phá vỡ dữ liệu lịch sử order đã tham chiếu tới món đó. |
| Non-Functional Requirement | - NFR24.1: Ảnh món ăn nên được tối ưu/nén trước khi lưu để đảm bảo tốc độ tải thực đơn. |

### 3.2.25 UC25: Quản lý danh mục món ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC25 |
| Use Case Name | Quản lý danh mục món ăn |
| Description | Là quản lý, tôi muốn thêm/sửa/xóa danh mục và gán món ăn vào một hoặc nhiều danh mục. |
| Actor(s) | Quản lý |
| Priority | Should have |
| Trigger | Quản lý chọn "Danh mục" trong Quản lý thực đơn (UC10). |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Danh mục được thêm/sửa/xóa; món ăn có thể được gán vào nhiều danh mục. |
| Basic Flow | 1. Quản lý xem danh sách danh mục hiện có, chọn "Thêm danh mục" hoặc sửa/xóa một danh mục.<br>2. Quản lý nhập tên danh mục, mô tả, thứ tự hiển thị.<br>3. Quản lý gán/bỏ gán món ăn vào danh mục (quan hệ N-N) qua giao diện đa chọn.<br>4. Hệ thống lưu và cập nhật thứ tự tab danh mục trên thực đơn khách. |
| Alternative Flow | Không có |
| Exception Flow | 3a. Xóa danh mục đang có món gán.<br>- 3a1. Hệ thống cảnh báo và yêu cầu xác nhận (món chỉ mất gán tới danh mục đó, không bị xóa khỏi hệ thống). |
| Business Rules | - BR25.1: Tuân theo QĐ5 — danh mục "Bán chạy" là danh mục hệ thống tự tổng hợp, Quản lý không gán món thủ công vào danh mục này. |
| Non-Functional Requirement | - NFR25.1: Thứ tự danh mục có thể kéo-thả (drag & drop) để sắp xếp ưu tiên hiển thị. |

### 3.2.26 UC26: Cập nhật trạng thái Còn hàng/Hết hàng

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC26 |
| Use Case Name | Cập nhật trạng thái Còn hàng/Hết hàng |
| Description | Là quản lý, tôi muốn bật/tắt nhanh trạng thái còn hàng của một món ngay trên danh sách. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý chọn nhanh công tắc trạng thái của một món trong danh sách (UC24). |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Trạng thái món thay đổi, đồng bộ realtime tới khách hàng. |
| Basic Flow | 1. Quản lý bật/tắt công tắc "Còn hàng" của một món.<br>2. Hệ thống cập nhật trạng thái món, phát sự kiện WebSocket broadcast.<br>3. Trên giao diện khách, món chuyển hiển thị tương ứng (mờ và không thể thêm nếu hết hàng). |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR26.1: Tuân theo QĐ4 (quy định Còn hàng/Hết hàng). |
| Non-Functional Requirement | - NFR26.1: Việc bật/tắt trạng thái phải thực hiện được ngay trên danh sách, không cần mở form chỉnh sửa đầy đủ. |

### 3.2.27 UC27: Quản lý danh sách bàn ăn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC27 |
| Use Case Name | Quản lý danh sách bàn ăn |
| Description | Là quản lý, tôi muốn thêm/sửa/xóa bàn ăn trong hệ thống. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý chọn "Danh sách bàn ăn" trong Quản lý bàn ăn (UC11). |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Bàn ăn được thêm mới/sửa/xóa (BM3). |
| Basic Flow | 1. Quản lý xem danh sách bàn hiện có, chọn "Thêm bàn" hoặc sửa/xóa một bàn.<br>2. Quản lý nhập mã bàn, tên hiển thị, khu vực (Khu chung/Phòng VIP), sức chứa tối đa.<br>3. Với bàn mới, hệ thống tự sinh mã QR và session token ban đầu theo QĐ2.<br>4. Hệ thống lưu và hiển thị lại danh sách. |
| Alternative Flow | Không có |
| Exception Flow | 4a. Xóa một bàn đang ở trạng thái "Đang có khách".<br>- 4a1. Hệ thống từ chối xóa, yêu cầu đóng bàn trước.<br>- Use Case dừng lại. |
| Business Rules | - BR27.1: Tuân theo QĐ7 (trạng thái bàn ăn). |
| Non-Functional Requirement | - NFR27.1: Danh sách bàn hỗ trợ lọc theo khu vực và trạng thái. |

### 3.2.28 UC28: Cấp lại mã QR cho bàn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC28 |
| Use Case Name | Cấp lại mã QR cho bàn |
| Description | Là quản lý, tôi muốn cấp lại mã QR cho một hoặc nhiều bàn khi nghi ngờ mã QR bị lộ ra ngoài nhà hàng. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Quản lý chọn một hoặc nhiều bàn, nhấn "Cấp lại mã QR". |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Token cũ của (các) bàn bị thu hồi (revoke); token mới được sinh; mã QR mới sẵn sàng để in/hiển thị. |
| Basic Flow | 1. Quản lý chọn một hoặc nhiều bàn cần cấp lại QR.<br>2. Quản lý xác nhận "Cấp lại mã QR".<br>3. Hệ thống sinh session_token mới theo chuẩn UUID v4 kết hợp mã băm cho từng bàn đã chọn, đồng thời vô hiệu hóa token cũ.<br>4. Mọi thiết bị khách đang mở bằng mã QR cũ của (các) bàn đó bị ngắt kết nối ngay lập tức.<br>5. Hệ thống xuất mã QR mới (dạng SVG/PNG) để Quản lý tải về in ấn. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR28.1: Tuân theo QĐ2 (vòng đời QR & bảo mật chống lộ QR). |
| Non-Functional Requirement | - NFR28.1: Hỗ trợ cấp lại hàng loạt (nhiều bàn cùng lúc) để xử lý nhanh sự cố. |

### 3.2.29 UC29: Bật/tắt quyền order của bàn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC29 |
| Use Case Name | Bật/tắt quyền order của bàn |
| Description | Là quản lý, tôi muốn khóa/mở quyền gọi món của một bàn khi xảy ra tranh chấp hoặc lộ link. |
| Actor(s) | Quản lý |
| Priority | Should have |
| Trigger | Quản lý chọn công tắc "Khóa order" trên một bàn. |
| Pre-Condition(s) | Tài khoản có role Manager. |
| Post-Condition(s) | Cờ is_order_locked của bàn được bật/tắt. |
| Basic Flow | 1. Quản lý chọn bàn cần khóa/mở order.<br>2. Quản lý bật công tắc "Khóa order".<br>3. Hệ thống cập nhật is_order_locked = true, đồng bộ realtime tới thiết bị khách của bàn đó (nút "Gửi đơn vào bếp" bị vô hiệu hóa).<br>4. Các món đã gửi trước đó vẫn được giữ nguyên trạng thái, không bị ảnh hưởng. |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR29.1: Tuân theo QĐ3 (kiểm soát quyền gọi món khẩn cấp của bàn). |
| Non-Functional Requirement | - NFR29.1: Việc khóa/mở order có hiệu lực tức thời trên thiết bị khách (dưới 1 giây). |

### 3.2.30 UC30: Thanh toán bằng tiền mặt

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC30 |
| Use Case Name | Thanh toán bằng tiền mặt |
| Description | Là quản lý, tôi muốn ghi nhận thanh toán bằng tiền mặt cho hóa đơn của bàn. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Trong UC15, Quản lý chọn phương thức "Tiền mặt". |
| Pre-Condition(s) | Đã tính tổng hóa đơn theo QĐ1. |
| Post-Condition(s) | Giao dịch thanh toán được ghi nhận với phương thức Tiền mặt, trạng thái Đã thanh toán. |
| Basic Flow | 1. Quản lý chọn "Tiền mặt".<br>2. Quản lý nhận tiền mặt từ khách, nhập số tiền khách đưa (tùy chọn, để tính tiền thối).<br>3. Quản lý nhấn "Xác nhận đã thu tiền".<br>4. Hệ thống ghi nhận giao dịch thanh toán (phương thức Tiền mặt, thời gian giao dịch, tổng tiền).<br>5. Hệ thống chuyển sang UC32 (In hóa đơn & đóng bàn). |
| Alternative Flow | Không có |
| Exception Flow | Không có |
| Business Rules | - BR30.1: Tuân theo QĐ1 (công thức tính tổng tiền hóa đơn). |
| Non-Functional Requirement | - NFR30.1: Giao diện phải hiện rõ số tiền cần thu và tiền thối (nếu có nhập số tiền khách đưa). |

### 3.2.31 UC31: Thanh toán bằng VietQR

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC31 |
| Use Case Name | Thanh toán bằng VietQR |
| Description | Là quản lý, tôi muốn tạo mã VietQR động để khách quét chuyển khoản đúng số tiền hóa đơn. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Trong UC15, Quản lý chọn phương thức "VietQR". |
| Pre-Condition(s) | Đã tính tổng hóa đơn theo QĐ1. |
| Post-Condition(s) | Giao dịch thanh toán được ghi nhận với phương thức VietQR, chuyển trạng thái Đã thanh toán khi xác nhận. |
| Basic Flow | 1. Quản lý chọn "VietQR".<br>2. Hệ thống sinh mã VietQR động (chuẩn Napas247) chứa chính xác số tiền cần thanh toán và nội dung chuyển khoản (mã bàn + số hóa đơn).<br>3. Màn hình hiển thị mã QR để khách quét bằng ứng dụng ngân hàng.<br>4. Quản lý xác nhận thủ công đã nhận được tiền vào tài khoản (hoặc hệ thống nhận webhook nếu có tích hợp cổng thanh toán).<br>5. Hệ thống ghi nhận giao dịch, chuyển sang UC32. |
| Alternative Flow | Không có |
| Exception Flow | 4a. Khách quét nhưng chưa chuyển khoản hoặc hủy giữa chừng.<br>- 4a1. Quản lý có thể quay lại chọn phương thức khác (UC30) hoặc tạo lại mã VietQR mới.<br>- Use Case dừng lại. |
| Business Rules | - BR31.1: Mã VietQR phải luôn khớp chính xác số tiền hóa đơn hiện hành (QĐ1) để tránh sai lệch thu tiền. |
| Non-Functional Requirement | - NFR31.1: Mã QR phải được sinh và hiển thị trong vòng 2 giây. |

### 3.2.32 UC32: In hóa đơn & đóng bàn

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC32 |
| Use Case Name | In hóa đơn & đóng bàn |
| Description | Là quản lý, tôi muốn in hóa đơn hoàn tất và đóng bàn sau khi đã xác nhận thu tiền. |
| Actor(s) | Quản lý |
| Priority | Must have |
| Trigger | Sau khi xác nhận đã thu tiền ở UC30 hoặc UC31. |
| Pre-Condition(s) | Giao dịch thanh toán đã ở trạng thái Đã thanh toán. |
| Post-Condition(s) | Hóa đơn được in (khổ 80mm); bàn chuyển sang trạng thái "Đang dọn dẹp"; mã QR/token của bàn bị thu hồi. |
| Basic Flow | 1. Hệ thống tổng hợp hóa đơn hoàn chỉnh (chi tiết món, tổng tiền, phương thức thanh toán).<br>2. Quản lý nhấn "In hóa đơn", hệ thống gửi lệnh in tới máy in nhiệt khổ 80mm.<br>3. Hệ thống lưu doanh thu của phiên bàn vào báo cáo (phục vụ UC16).<br>4. Hệ thống chuyển trạng thái bàn sang "Đang dọn dẹp", thu hồi session_token hiện tại (theo QĐ2).<br>5. Sau khi bàn được dọn xong, hệ thống/Quản lý chuyển bàn về "Bàn trống" và sinh sẵn token QR mới cho lượt khách kế tiếp. |
| Alternative Flow | Không có |
| Exception Flow | 2a. Máy in không phản hồi hoặc hết giấy.<br>- 2a1. Hệ thống báo lỗi in nhưng vẫn cho phép đóng bàn, đồng thời cho phép "In lại" hóa đơn sau đó từ lịch sử. |
| Business Rules | - BR32.1: Tuân theo QĐ2 (thu hồi token khi đóng bàn) và QĐ7 (chuyển trạng thái bàn). |
| Non-Functional Requirement | - NFR32.1: Hệ thống phải tương thích các dòng máy in nhiệt cổng LAN/USB phổ biến (Xprinter, Epson). |

### 3.2.33 UC33: Chuyển bàn (Table Move / Transfer 1:1)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC33 |
| Use Case Name | Chuyển bàn (Table Move / Transfer 1:1) |
| Description | Khách hàng (Chủ Bàn) hoặc Quản lý thực hiện chuyển toàn bộ đợt order đã gọi và giỏ hàng đang chọn từ bàn cũ sang bàn mới còn trống, áp dụng cơ chế khóa 2 giai đoạn (2-Phase Lock). |
| Actor(s) | Khách hàng (Chủ Bàn), Quản lý / Thu ngân |
| Priority | High |
| Trigger | Khách hàng có nhu cầu đổi sang bàn khác (view đẹp hơn, mát hơn) hoặc Quản lý điều chuyển bàn trên sơ đồ POS. |
| Pre-Condition(s) | Bàn cũ ở trạng thái `OCCUPIED`. Bàn đích ở trạng thái `AVAILABLE`. Thiết bị yêu cầu phải là Chủ Bàn (`isHost = true`) hoặc tài khoản nhân sự có thẩm quyền. |
| Post-Condition(s) | Toàn bộ order và giỏ hàng được gán sang bàn đích; bàn cũ chuyển trạng thái `CLEANING`; mã PIN và session token bàn cũ bị thu hồi; toàn bộ thiết bị chuyển hướng sang bàn mới sau 5 giây. |
| Basic Flow | 1. Khách hàng (Chủ Bàn) bấm nút "Đổi bàn" trên giao diện gọi món.<br>2. Hệ thống gọi API xuất mã chuyển bàn (`TableTransferRequest`), sinh mã chuyển (VD: `TRF-8492`) có thời hạn sống TTL 5 phút, đồng thời đặt cờ `isOrderLocked = true` tại bàn cũ (giữ nguyên 100% giỏ hàng).<br>3. Khách hàng di chuyển sang bàn mới, mở modal chuyển bàn và nhập mã điều chuyển kèm mã PIN 4 số của bàn mới.<br>4. Hệ thống kiểm tra hợp lệ 2 chiều (Two-Way Handshake): xác thực mã chuyển hợp lệ, chưa hết hạn và đúng mã PIN bàn đích.<br>5. Hệ thống di dời toàn bộ `Order` và `OrderItem` sang bàn đích, hợp nhất giỏ hàng, cập nhật bàn cũ thành `CLEANING` và bàn mới thành `OCCUPIED`.<br>6. Server bắn WebSocket `/topic/table/{oldSessionToken}` kích hoạt modal đếm ngược 5 giây trên mọi thiết bị đang kết nối tại bàn cũ để tự động chuyển sang bàn mới.<br>7. Server bắn WebSocket `/topic/kitchen/orders` và `/topic/waiter/orders` cập nhật số bàn mới trên màn hình Bếp KDS và Phục vụ. |
| Alternative Flow | 1a. Quản lý thực hiện chuyển trực tiếp 1-chạm từ màn hình sơ đồ bàn POS (`directTransfer`). Hệ thống thực hiện ngay bước 5–7 mà không cần sinh mã tạm. |
| Exception Flow | 2a. Khách đổi ý không muốn chuyển: Nhấn "Hủy yêu cầu đổi bàn", hệ thống mở lại `isOrderLocked = false` tại bàn cũ.<br>3a. Mã chuyển bàn quá hạn 5 phút: Hệ thống tự động hủy mã và khôi phục trạng thái order bàn cũ.<br>4a. Nhập sai mã PIN bàn đích quá 5 lần: Khóa tạm thời 60 giây chống Brute-force. |
| Business Rules | - BR33.1: Chỉ Chủ Bàn (Host) mới được tạo mã chuyển bàn.<br>- BR33.2: Cơ chế 2-Phase Lock bảo toàn 100% giỏ hàng đang chọn dở của khách.<br>- BR33.3: Bàn đích bắt buộc phải ở trạng thái Trống (`AVAILABLE`). |
| Non-Functional Requirement | - NFR33.1: Toàn bộ quá trình di dời dữ liệu và đồng bộ WebSocket diễn ra dưới 1 giây. |

### 3.2.34 UC34: Ghép bàn & Cụm bàn liên kết (Table Merge & Cluster N:1)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC34 |
| Use Case Name | Ghép bàn & Cụm bàn liên kết (Table Merge & Cluster N:1) |
| Description | Ghép một hoặc nhiều bàn phụ (Slave Tables) vào một bàn chính (Master Table) phục vụ đoàn tiệc đông người, gom chung order và hợp nhất hóa đơn thanh toán tập trung. |
| Actor(s) | Khách hàng (Chủ Bàn), Quản lý / Thu ngân |
| Priority | High |
| Trigger | Đoàn khách đi đông người ngồi rải rác nhiều bàn muốn gộp chung thành một nhóm thanh toán. |
| Pre-Condition(s) | Cả bàn nguồn và bàn đích đều đang ở trạng thái `OCCUPIED`. Bàn nguồn không phải là bàn Master đang quản lý các bàn phụ khác. |
| Post-Condition(s) | Bàn phụ được liên kết vào Bàn chính (`masterTable`); toàn bộ order của bàn phụ được chuyển về Bàn chính; giỏ hàng được hợp nhất; thanh toán tập trung tại Bàn chính. |
| Basic Flow | 1. Khách tại bàn phụ (Chủ Bàn) hoặc Quản lý khởi tạo yêu cầu Ghép bàn (`transferType = MERGE`).<br>2. Nhập mã chuyển và mã PIN xác thực của Bàn chính (Master Table).<br>3. Server xác thực thành công, thiết lập quan hệ `masterTable` trỏ về Bàn chính.<br>4. Toàn bộ `Order` của bàn phụ được reparent về Bàn chính; các món trong giỏ hàng bàn phụ được dồn vào giỏ Bàn chính.<br>5. Server tự động tăng sức chứa thiết bị tối đa (`maxActiveDevices`) của cụm bàn tương ứng với tổng số khách.<br>6. Server bắn WebSocket `/topic/tables` cập nhật giao diện Cụm bàn (Master Table hiển thị nhãn Cụm kèm danh sách bàn phụ liên kết).<br>7. Khi kết thúc bữa tiệc, Thu ngân thanh toán hóa đơn tổng tại Bàn chính (UC15), hệ thống tự động chuyển Bàn chính và toàn bộ Bàn phụ sang trạng thái `CLEANING`. |
| Alternative Flow | 1a. Quản lý liên kết nhanh nhiều bàn thành Cụm bàn trực tiếp từ giao diện POS (`TableClusterLinkRequest`). |
| Exception Flow | 3a. Bàn phụ đang cố gắng ghép vòng lặp (ghép vào bàn đang là slave của chính mình): Hệ thống từ chối và báo lỗi nghiệp vụ.<br>4a. Bàn phụ tự ý gọi thanh toán riêng: Hệ thống tự động chuyển hướng yêu cầu thanh toán về Bàn chính. |
| Business Rules | - BR34.1: Chỉ Bàn chính (Master Table) mới có quyền yêu cầu xuất hóa đơn và thanh toán.<br>- BR34.2: Thành viên cụm bàn không được phép chuyển bàn đơn lẻ khi chưa hủy liên kết cụm. |
| Non-Functional Requirement | - NFR34.1: Sơ đồ bàn POS hiển thị trực quan các đường liên kết giữa Bàn chính và các Bàn phụ. |

### 3.2.35 UC35: Quản lý thiết bị & Phân quyền Chủ Bàn (Host / Member Session Management)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC35 |
| Use Case Name | Quản lý thiết bị & Phân quyền Chủ Bàn |
| Description | Xác thực mã PIN 4 số chống Brute-force khi quét QR vào bàn, tự động phân quyền Chủ Bàn (Host) cho thiết bị đầu tiên, cho phép đá thiết bị lạ hoặc nhường quyền Chủ Bàn. |
| Actor(s) | Khách hàng (tại bàn), Quản lý / Admin |
| Priority | High |
| Trigger | Khách quét mã QR tại bàn để bắt đầu gọi món, hoặc Chủ Bàn cần quản lý các máy cùng bàn. |
| Pre-Condition(s) | Khách đã quét mã QR mở URL `/table/:tableId`. Bàn đang hoạt động hợp lệ. |
| Post-Condition(s) | Thiết bị được định danh bằng `deviceToken`; lưu phiên vào `TableSessionDevice`; gán quyền `isHost`; cấp quyền thao tác giỏ hàng và đặt món. |
| Basic Flow | 1. Trình duyệt hiển thị trang nhập mã PIN 4 số an toàn (`TableEntryPage`).<br>2. Khách nhập mã PIN 4 số hiển thị trên bàn ăn.<br>3. Server gọi `verifyPasscode`: kiểm tra tính đúng đắn của mã PIN.<br>4. Nếu đúng: reset bộ đếm lỗi; kiểm tra số lượng thiết bị hiện tại của bàn. Nếu đây là thiết bị đầu tiên, gán `isHost = true`, các thiết bị sau gán `isHost = false`.<br>5. Client lưu `sessionToken`, `deviceToken`, `isHost` vào `useTableSessionStore` và chuyển hướng đến trang Thực đơn (`/menu`).<br>6. Chủ Bàn có thể mở modal `TableDevicesModal` để xem danh sách máy đang kết nối, bấm "Đá thiết bị" (`kickDevice`) hoặc "Nhường quyền Chủ Bàn" (`transferHost`).<br>7. Thiết bị bị đá lập tức nhận thông báo qua WebSocket, xóa sạch session và bị đẩy về màn hình thông báo rời bàn. |
| Alternative Flow | 6a. Quản lý can thiệp trên giao diện POS: xem danh sách thiết bị của bàn (`AdminTableDevicesModal`), bấm cưỡng chế đá máy hoặc reset quyền Host. |
| Exception Flow | 3a. Nhập sai mã PIN: Hệ thống tăng `failedAttempts`. Khi sai liên tiếp 5 lần, bàn bị khóa 60 giây (`lockedUntil = now + 60s`). API trả lỗi `TABLE_PASSCODE_LOCKED` kèm đồng hồ đếm ngược.<br>4a. Số thiết bị vượt quá `maxActiveDevices`: Server từ chối kết nối mới để chống spam/quá tải bàn. |
| Business Rules | - BR35.1: Mỗi phiên bàn chỉ có duy nhất 1 Chủ Bàn (Host) tại một thời điểm.<br>- BR35.2: Chỉ Chủ Bàn mới có quyền bấm "Gửi đơn vào bếp", "Đổi bàn" và "Đá thiết bị". |
| Non-Functional Requirement | - NFR35.1: Mã PIN 4 số được sinh ngẫu nhiên bằng bộ sinh số an toàn `SecureRandom`. |

### 3.2.36 UC36: Quản lý tài khoản nhân sự (Employee Management)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC36 |
| Use Case Name | Quản lý tài khoản nhân sự |
| Description | Quản trị viên (Admin) quản lý hồ sơ nhân viên trong hệ thống nhà hàng, phân quyền vai trò (ADMIN, MANAGER, KITCHEN, STAFF), kích hoạt/khóa tài khoản và gửi email thông tin đăng nhập. |
| Actor(s) | Quản trị viên (Admin) |
| Priority | High |
| Trigger | Admin có nhu cầu thêm nhân viên mới, điều chỉnh chức vụ hoặc khóa tài khoản nhân sự nghỉ việc. |
| Pre-Condition(s) | Admin đã đăng nhập với tài khoản có vai trò `ROLE_ADMIN`. |
| Post-Condition(s) | Hồ sơ nhân viên được lưu vào cơ sở dữ liệu (`User`); email chào mừng chứa thông tin đăng nhập được gửi tự động tới nhân viên. |
| Basic Flow | 1. Admin truy cập trang Quản lý nhân sự (`/admin/employees`).<br>2. Hệ thống hiển thị bảng thống kê nhân sự (`AdminEmployeeStats`) và danh sách nhân viên.<br>3. Admin có thể tìm kiếm theo tên/email/SĐT, hoặc lọc danh sách theo vai trò (`AdminEmployeeRoleRibbon`).<br>4. Nhấn "Thêm nhân viên", nhập thông tin: Họ tên, Tên đăng nhập, Email, Số điện thoại, Vai trò.<br>5. Hệ thống xác thực tính duy nhất của username và email, mã hóa mật khẩu bằng BCrypt, lưu tài khoản với trạng thái `ACTIVE`.<br>6. Hệ thống kích hoạt `MailService` gửi email bất đồng bộ thông báo tài khoản và mật khẩu khởi tạo cho nhân viên.<br>7. Admin có thể nhấn nút chuyển trạng thái để kích hoạt hoặc tạm khóa (`INACTIVE`) tài khoản nhân viên bất kỳ lúc nào. |
| Alternative Flow | 4a. Admin nhấn nút "Sửa" trên dòng nhân viên để cập nhật thông tin họ tên, email, vai trò hoặc reset mật khẩu mới. |
| Exception Flow | 5a. Trùng lặp username hoặc email: Hệ thống báo lỗi và yêu cầu chỉnh sửa.<br>7a. Admin cố tình khóa tài khoản của chính mình: Hệ thống ngăn chặn để tránh mất quyền quản trị hệ thống. |
| Business Rules | - BR36.1: Chỉ tài khoản có vai trò ADMIN mới có quyền truy cập module này (`PermissionRoute adminOnly`).<br>- BR36.2: Mật khẩu luôn được băm bằng thuật toán BCrypt với độ muối (Salt) an toàn. |
| Non-Functional Requirement | - NFR36.1: Email chào mừng gửi qua SMTP Google Mail hoàn tất trong vòng dưới 3 giây. |

### 3.2.37 UC37: Quản lý hóa đơn & Đối soát doanh thu (Invoice Management)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC37 |
| Use Case Name | Quản lý hóa đơn & Đối soát doanh thu |
| Description | Quản lý tra cứu, tìm kiếm và đối soát toàn bộ lịch sử hóa đơn thanh toán của nhà hàng theo mốc thời gian, trạng thái và phương thức thanh toán. |
| Actor(s) | Quản lý / Admin |
| Priority | High |
| Trigger | Quản lý cần kiểm tra doanh thu cuối ngày, đối soát tiền mặt hoặc tra cứu lại đơn hàng cũ của khách. |
| Pre-Condition(s) | Người dùng đã đăng nhập với vai trò ADMIN hoặc MANAGER có quyền `INVOICES`. |
| Post-Condition(s) | Danh sách hóa đơn được kết xuất chính xác; xem được chi tiết từng món ăn trong từng đợt order của hóa đơn. |
| Basic Flow | 1. Quản lý truy cập trang Lịch sử hóa đơn (`/admin/invoices`).<br>2. Chọn bộ lọc: Khoảng thời gian (Từ ngày - Đến ngày), Trạng thái thanh toán (`PAID`, `PENDING`, `CANCELLED`), Phương thức (`CASH`, `VIETQR`), hoặc tìm theo mã hóa đơn/số bàn.<br>3. Hệ thống truy vấn cơ sở dữ liệu có phân trang (`Pageable`) và hiển thị danh sách hóa đơn tương ứng.<br>4. Quản lý nhấn vào một dòng hóa đơn để mở modal chi tiết (`InvoiceDetailModal`).<br>5. Màn hình hiển thị đầy đủ: Mã hóa đơn, Bàn ăn (hoặc Cụm bàn), Thời gian mở bàn và thanh toán, Danh sách từng đợt order kèm các món ăn, Tiền tạm tính, Chiết khấu khuyến mãi, Tiền thuế VAT và Tổng tiền thanh toán cuối cùng.<br>6. Quản lý có thể nhấn "In lại hóa đơn" ra máy in nhiệt khổ 80mm khi khách yêu cầu. |
| Alternative Flow | Không có |
| Exception Flow | 3a. Không tìm thấy hóa đơn nào khớp với bộ lọc: Hệ thống hiển thị trạng thái danh sách trống thân thiện. |
| Business Rules | - BR37.1: Hóa đơn đã ở trạng thái `PAID` là dữ liệu bất biến (Immutable), không được phép chỉnh sửa số tiền.<br>- BR37.2: Doanh thu của Cụm bàn được tổng hợp đầy đủ từ tất cả các bàn phụ liên kết. |
| Non-Functional Requirement | - NFR37.1: Thời gian truy vấn tìm kiếm hóa đơn có phân trang phải dưới 500ms đối với cơ sở dữ liệu hàng chục nghìn bản ghi. |

### 3.2.38 UC38: Tiếp nhận phục vụ & giao món tại sảnh (Waiter Service)

| Thành phần | Nội dung |
|---|---|
| Use Case ID | UC38 |
| Use Case Name | Tiếp nhận phục vụ & giao món tại sảnh |
| Description | Nhân viên phục vụ theo dõi danh sách món ăn bếp đã chế biến xong cần bưng ra bàn, tiếp nhận chuông gọi phục vụ từ thực khách và xác nhận thu tiền mặt tại chỗ. |
| Actor(s) | Nhân viên Phục vụ (Waiter) |
| Priority | High |
| Trigger | Bếp hoàn tất nấu một món ăn, hoặc khách hàng bấm chuông gọi phục vụ tại bàn. |
| Pre-Condition(s) | Nhân viên phục vụ đã đăng nhập vào hệ thống với quyền `WAITER`. |
| Post-Condition(s) | Món ăn được giao tận bàn và cập nhật trạng thái `SERVED`; chuông gọi phục vụ được xử lý; bàn được dọn dẹp hoặc thu tiền mặt. |
| Basic Flow | 1. Nhân viên phục vụ mở màn hình trạm phục vụ (`/waiter`).<br>2. Khi Bếp bấm hoàn tất chế biến món ăn, hệ thống bắn tin nhắn WebSocket tới `/topic/waiter/orders`.<br>3. Thẻ bàn `WaiterTableCard` trên màn hình phục vụ phát âm thanh thông báo và làm nổi bật món ăn cần bưng ra.<br>4. Nhân viên lấy món từ quầy bếp, mang đến bàn khách và nhấn "Xác nhận đã bưng món" (`deliverOrderItem`).<br>5. Khi khách hàng bấm chuông gọi phục vụ (UC07), thanh Header `WaiterHeader` hiển thị cảnh báo đỏ kèm nội dung yêu cầu (xin đá, chén dĩa). Nhân viên đến hỗ trợ và nhấn "Đã hỗ trợ xong" để đóng chuông.<br>6. Khi khách yêu cầu tính tiền mặt, nhân viên phục vụ có thể kiểm tra hóa đơn tạm tính và xác nhận đã thu tiền mặt trực tiếp tại bàn. |
| Alternative Flow | 4a. Bàn có nhiều món xong cùng lúc: Nhân viên có thể nhấn "Bưng tất cả món" (`deliverAllOrderItems`) để cập nhật nhanh. |
| Exception Flow | 2a. Khách đổi bàn trong lúc món đang nấu: Hệ thống tự động cập nhật số bàn mới trên thẻ `WaiterTableCard` để nhân viên không bưng nhầm sang bàn cũ. |
| Business Rules | - BR38.1: Màn hình phục vụ được thiết kế giao diện nút bấm lớn, tương thích màn hình cảm ứng điện thoại/tablet di động.<br>- BR38.2: Âm thanh cảnh báo chuông gọi phục vụ có tần số và âm lượng rõ ràng trong không gian nhà hàng. |
| Non-Functional Requirement | - NFR38.1: Thời gian từ khi bếp bấm xong món đến khi màn hình phục vụ nhận được tín hiệu là dưới 200ms. |
