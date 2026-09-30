# PHÂN TÍCH VÀ THIẾT KẾ KIẾN TRÚC CODEBASE CLIENT CHUẨN DOANH NGHIỆP (REACT + VITE)

> **Dự án:** Hệ thống quét mã QR gọi món tại bàn & Quản trị Nhà hàng theo thời gian thực (Hỏa Diệm Các)  
> **Môn học:** Kiểm thử phần mềm — Trường ĐH Sư phạm Kỹ thuật TP.HCM (HCMUTE)  
> **Công nghệ áp dụng:** React 18.3.1, Vite 6.1, Tailwind CSS 3.4, Zustand 5, Axios, STOMP/SockJS WebSocket, Lucide React, QRCode React, React Router v7.

---

## 1. Tổng quan và Triết lý Thiết kế

Trong các hệ thống đặt món và quản trị nhà hàng đa trạm (Khách hàng tại bàn — Trạm Bếp KDS — Trạm Phục vụ Waiter — Quầy Thu ngân POS — Ban Quản lý Admin), Frontend đóng vai trò sống còn trong việc đảm bảo **tốc độ phản hồi tức thì (Realtime Responsiveness)**, **trải nghiệm không gián đoạn (Seamless UX)**, **tính phân tách quyền hạn an toàn (Role-Based Access Control)** và **khả năng mở rộng, bảo trì dài hạn (High Scalability & Maintainability)**.

Codebase Client của Hỏa Diệm Các được xây dựng theo mô hình **Feature-based Architecture** (chuẩn **Bulletproof React**), chia nhỏ hệ thống thành các phân hệ nghiệp vụ độc lập (Vertical Slices), kết hợp cùng sự phân định ranh giới rõ ràng giữa **Khung điều hướng (Layouts)**, **Màn hình trang (Pages)**, **Trạng thái toàn cục (Zustand Stores)** và **Lớp kết nối mạng (Axios Interceptors & STOMP WebSocket)**.

### Ba Nguyên Tắc Cốt Lõi:
1. **Realtime-First Architecture:** Quản lý các kênh WebSocket độc lập (`/topic/tables`, `/topic/kitchen/orders`, `/topic/waiter/orders`, `/topic/table/{sessionToken}`, `/topic/menu-items`), cho phép dữ liệu đẩy về cập nhật tức thời các Store tương ứng mà không làm giật lag hay kích hoạt re-render toàn bộ ứng dụng.
2. **Bảo Mật & Kiểm Soát Chống Lạm Dụng Tại Bàn (Anti-Abuse & Role Separation):**
   - Phân biệt rõ rệt vai trò **Chủ Bàn (Host Device)** và **Thành Viên (Member Device)** ngay trên client thông qua `useTableSessionStore`.
   - Cơ chế bảo vệ Brute-Force Shield tại màn hình nhập mã PIN 4 số (`TableEntryPage`): đếm số lần sai, khóa tạm thời 60 giây và hiển thị bộ đếm ngược thời gian thực.
   - Cơ chế chuyển bàn mượt mà (Smooth Handshake Modal): đếm ngược 5 giây trước khi tự động chuyển hướng thiết bị sang bàn mới.
3. **Phục Vụ Tối Đa Cho Kiểm Thử Phần Mềm:** Tách biệt triệt để giữa logic tính toán thuần túy (`utils/`, pure functions), tầng dữ liệu mạng (`api/`, Axios/WebSocket) và thành phần hiển thị giao diện (`components/`, Presentational UI), tạo điều kiện tối ưu để viết kịch bản kiểm thử hộp đen (Black-box Test Cases) và kiểm thử hộp trắng (White-box Unit Tests).

---

## 2. Sơ đồ Cây Thư Mục Toàn Diện (Directory Tree)

```text
HoaDiemCac-client/
├── documents/                        # Tài liệu kiến trúc & đặc tả kỹ thuật
│   ├── backend_standard_structure.md # Kiến trúc chuẩn Backend Spring Boot 3.3.4
│   ├── codebase_client.txt           # Sơ đồ cây thư mục chi tiết toàn bộ client
│   ├── codebase_client_anlyst.md     # [Tài liệu này] Phân tích chuyên sâu kiến trúc Client
│   ├── DacTa_Chuong3_UseCase.md       # Đặc tả Use Case chi tiết hệ thống (UC01 - UC38)
│   └── ke_hoach_trien_khai_entities.md# Thiết kế chi tiết Domain Entities & cơ sở dữ liệu
├── public/                           # Tài nguyên tĩnh
│   ├── logo_tab.png                  # Favicon logo nhà hàng
│   └── README.md
├── src/
│   ├── assets/                       # Banner, logo thương hiệu và icon tĩnh
│   │   ├── icons/README.md
│   │   ├── images/
│   │   │   ├── hotpot-banner.jpg     # Ảnh banner lẩu Hỏa Diệm Các
│   │   │   ├── logo.png              # Logo thương hiệu
│   │   │   ├── logo_tab.png          # Logo tab icon
│   │   │   └── README.md
│   │   └── README.md
│   │
│   ├── components/                   # Thành phần giao diện dùng chung (Shared UI)
│   │   ├── feedback/
│   │   │   ├── DbConnectionCheckButton.jsx # Nút kiểm tra nhanh kết nối backend/DB
│   │   │   └── README.md
│   │   ├── navigation/
│   │   │   ├── AdminSidebar.jsx      # Thanh menu điều hướng quản trị với icon Lucide
│   │   │   ├── KitchenHeader.jsx     # Header trạm Bếp KDS (đồng hồ thời gian thực, audio alert)
│   │   │   └── README.md
│   │   ├── ui/README.md
│   │   └── README.md
│   │
│   ├── config/                       # Cấu hình môi trường & hằng số hệ thống
│   │   ├── constants.js              # Khai báo Enums: ROLES, TABLE_STATUS, ORDER_STATUS, TABLE_AREA
│   │   ├── env.js                    # Đọc và validate VITE_API_BASE_URL, VITE_WS_URL
│   │   └── README.md
│   │
│   ├── features/                     # Các module nghiệp vụ độc lập (Vertical Feature Slices)
│   │   ├── auth/                     # Phân hệ Xác thực tài khoản
│   │   │   ├── api/README.md
│   │   │   ├── components/README.md
│   │   │   └── README.md
│   │   ├── cart/                     # Phân hệ Giỏ hàng dùng chung thời gian thực
│   │   │   ├── api/README.md
│   │   │   ├── components/README.md
│   │   │   ├── hooks/README.md
│   │   │   └── README.md
│   │   ├── customer/                 # Phân hệ Thực khách tại bàn ăn
│   │   │   ├── api/orderApi.js       # API gửi order, lấy danh sách món bàn
│   │   │   ├── components/
│   │   │   │   ├── CollaborativeBanner.jsx           # Banner báo số thiết bị cùng bàn & trạng thái phiên
│   │   │   │   ├── CustomerBottomCartBar.jsx         # Thanh giỏ hàng nổi chân trang mobile
│   │   │   │   ├── CustomerCartDrawer.jsx            # Ngăn kéo xem & duyệt giỏ hàng mobile
│   │   │   │   ├── CustomerCartSidebar.jsx           # Thanh giỏ hàng bên phải desktop
│   │   │   │   ├── CustomerCategorySidebar.jsx       # Menu danh mục dọc bên trái
│   │   │   │   ├── CustomerDishCard.jsx              # Thẻ món ăn (ảnh, tên, giá, nút +/- số lượng)
│   │   │   │   ├── CustomerHeader.jsx                # Header: Bàn số, chuông gọi phục vụ, tính tiền, đổi bàn
│   │   │   │   ├── CustomerOutOfStockNoticeModal.jsx # Modal cảnh báo món bị bếp báo hết khẩn cấp
│   │   │   │   ├── TableDevicesModal.jsx             # Modal quản lý thiết bị, đá thiết bị lạ, nhường Host
│   │   │   │   ├── TableTransferModal.jsx            # Modal Chuyển/Ghép bàn kèm PIN và countdown 5s
│   │   │   │   └── README.md
│   │   │   ├── data/
│   │   │   │   ├── mockCustomerMenu.js               # Thực đơn mẫu cho khách
│   │   │   │   └── README.md
│   │   │   ├── index.js
│   │   │   └── README.md
│   │   ├── dashboard/                # Phân hệ Thống kê & Doanh thu Admin
│   │   │   └── api/dashboardApi.js   # API lấy số liệu KPI, doanh thu theo ngày/tháng
│   │   ├── employee/                 # Phân hệ Quản lý Nhân sự & Phân quyền
│   │   │   ├── components/
│   │   │   │   ├── AdminEmployeeAccountModal.jsx     # Modal tạo/sửa tài khoản nhân viên
│   │   │   │   ├── AdminEmployeeControlBar.jsx        # Thanh tìm kiếm & lọc trạng thái tài khoản
│   │   │   │   ├── AdminEmployeeHeader.jsx            # Header & nút thêm nhân sự
│   │   │   │   ├── AdminEmployeeRoleRibbon.jsx        # Bộ lọc vai trò: ADMIN, MANAGER, KITCHEN, STAFF
│   │   │   │   ├── AdminEmployeeStats.jsx             # Thống kê tổng nhân sự, active, inactive
│   │   │   │   └── AdminEmployeeTableRow.jsx          # Dòng bảng nhân viên, nút toggle trạng thái
│   │   │   ├── data/mockEmployees.js                  # Dữ liệu nhân viên mẫu
│   │   │   └── index.js
│   │   ├── invoices/                 # Phân hệ Lịch sử Hóa đơn & Đối soát
│   │   │   ├── api/invoiceApi.js     # API tìm kiếm, lọc hóa đơn theo ngày, phương thức
│   │   │   └── components/
│   │   │       └── InvoiceDetailModal.jsx            # Modal chi tiết hóa đơn: từng đợt order, thuế, giảm giá
│   │   ├── kitchen/                  # Phân hệ Trạm Bếp KDS Điều phối FIFO
│   │   │   ├── api/kitchenApi.js     # API chuyển trạng thái nấu, báo hết món, mở lại món
│   │   │   ├── components/
│   │   │   │   ├── ConfirmOutOfStockModal.jsx        # Modal xác nhận báo hết món khẩn cấp
│   │   │   │   ├── KdsAggregatedView.jsx             # Chế độ tổng hợp gom món theo loại cần nấu gấp
│   │   │   │   ├── KdsFilterBar.jsx                  # Lọc đơn (Tất cả, Đang chờ, Đang nấu, Đã xong)
│   │   │   │   ├── KdsOrderCard.jsx                  # Thẻ đơn món kèm bàn ăn, đợt gọi, đồng hồ đếm phút
│   │   │   │   └── OutOfStockModal.jsx               # Danh sách các món đang tạm khóa hết hàng
│   │   │   ├── data/mockKitchenOrders.js              # Dữ liệu order mẫu cho KDS
│   │   │   ├── hooks/useKitchenSocket.js             # Hook kết nối WebSocket STOMP kênh /topic/kitchen/orders
│   │   │   ├── index.js
│   │   │   └── README.md
│   │   ├── menu/                     # Phân hệ Quản trị Thực đơn
│   │   │   ├── api/menuApi.js        # API CRUD món ăn, danh mục, cập nhật ảnh
│   │   │   ├── components/
│   │   │   │   ├── AdminMenuHeader.jsx               # Header trang quản lý món ăn
│   │   │   │   ├── CategoryFilterRibbon.jsx          # Thanh lọc danh mục cuộn ngang
│   │   │   │   ├── CategoryManageSection.jsx         # Quản lý danh mục món ăn
│   │   │   │   ├── CategoryModal.jsx                 # Modal tạo/sửa danh mục
│   │   │   │   ├── KdsToastNotification.jsx          # Toast thông báo khi món đổi trạng thái
│   │   │   │   ├── MenuItemModal.jsx                 # Modal thêm/sửa món ăn, upload Cloudinary
│   │   │   │   ├── MenuItemRow.jsx                   # Dòng hiển thị món ăn trên bảng
│   │   │   │   ├── MenuPagination.jsx                # Phân trang danh sách món
│   │   │   │   └── MenuSummaryControlBar.jsx         # Thanh tìm kiếm & lọc nhanh món ăn
│   │   │   ├── data/mockMenuItems.js                 # Dữ liệu món ăn và danh mục mẫu
│   │   │   ├── index.js
│   │   │   └── README.md
│   │   ├── orders/README.md
│   │   ├── payment/README.md
│   │   ├── tables/                   # Phân hệ Sơ đồ bàn, Dynamic QR & Cụm bàn
│   │   │   ├── api/tableApi.js       # API sơ đồ bàn, đổi PIN, khóa order, cụm bàn
│   │   │   ├── components/
│   │   │   │   ├── AdminTableDetailModal.jsx         # Modal xem chi tiết bàn, order, tính tiền, đóng bàn
│   │   │   │   ├── AdminTableDevicesModal.jsx        # Modal quản lý thiết bị kết nối, đá máy, reset host
│   │   │   │   ├── FloorAreaSection.jsx              # Phân khu sảnh chung / phòng VIP
│   │   │   │   ├── FloorBottomBar.jsx                # Thanh điều khiển & tóm tắt đáy màn hình
│   │   │   │   ├── FloorStatusHeader.jsx             # Thống kê nhanh: Trống, Có khách, Dọn dẹp
│   │   │   │   ├── TableCard.jsx                     # Thẻ bàn ăn: số bàn, khách, giờ ngồi, tổng tiền, cờ khóa
│   │   │   │   ├── TableCardQr.jsx                   # Thẻ bàn chuyên dụng cho màn hình in mã QR
│   │   │   │   ├── TablePasscodeModal.jsx            # Modal xem & xoay vòng mã PIN 4 số của bàn
│   │   │   │   ├── TableQrPrintModal.jsx             # Modal in mã QR bàn hàng loạt
│   │   │   │   └── README.md
│   │   │   ├── data/mockTables.js                    # Dữ liệu bàn mẫu
│   │   │   ├── index.js
│   │   │   └── README.md
│   │   ├── waiter/                   # Phân hệ Trạm Phục vụ (Waiter Station)
│   │   │   ├── api/waiterApi.js      # API phục vụ: bưng món, thu tiền mặt, dọn bàn
│   │   │   ├── components/
│   │   │   │   ├── WaiterHeader.jsx                  # Header phục vụ kèm chuông báo gọi bàn realtime
│   │   │   │   └── WaiterTableCard.jsx               # Thẻ bàn phục vụ theo dõi món ăn đã nấu xong
│   │   │   └── index.js
│   │   └── README.md
│   │
│   ├── layouts/                      # Khung giao diện cố định (Layout Wrappers)
│   │   ├── AdminLayout.jsx           # Khung quản trị: AdminSidebar + Topbar + Content Outlet
│   │   ├── CustomerLayout.jsx        # Khung khách: CustomerHeader + Content Outlet + Cart Bar
│   │   ├── KitchenLayout.jsx         # Khung full màn hình cho Bếp KDS và Waiter
│   │   └── README.md
│   │
│   ├── lib/                          # Thư viện & Dịch vụ ngoại vi
│   │   ├── axios.js                  # Axios client với interceptors gắn JWT & session token
│   │   ├── uploadService.js          # Dịch vụ upload ảnh lên Cloudinary
│   │   ├── websocket.js              # Client STOMP over SockJS kết nối /ws
│   │   └── README.md
│   │
│   ├── pages/                        # Các trang màn hình hoàn chỉnh (Router Outlets)
│   │   ├── admin/
│   │   │   ├── AdminDashboardPage.jsx      # Trang thống kê KPI & doanh thu
│   │   │   ├── AdminEmployeeManagePage.jsx # Trang quản lý nhân sự & vai trò
│   │   │   ├── AdminInvoicesPage.jsx       # Trang tra cứu hóa đơn & xuất đối soát
│   │   │   ├── AdminProfilePage.jsx        # Trang thông tin cá nhân nhân viên
│   │   │   ├── AdminTablesQrPage.jsx       # Trang in mã QR bàn nhà hàng
│   │   │   ├── MenuManagePage.jsx          # Trang quản lý thực đơn món ăn & danh mục
│   │   │   ├── TableManagePage.jsx         # Trang sơ đồ bàn ăn thời gian thực (POS Floor View)
│   │   │   └── README.md
│   │   ├── auth/
│   │   │   ├── LoginPage.jsx         # Trang đăng nhập dành cho nhân viên / quản lý
│   │   │   └── README.md
│   │   ├── customer/
│   │   │   ├── MenuPage.jsx          # Trang thực đơn & giỏ hàng gọi món của khách
│   │   │   ├── TableEntryPage.jsx    # Trang nhập PIN 4 số khi quét mã QR vào bàn ăn
│   │   │   └── README.md
│   │   ├── kitchen/
│   │   │   ├── KitchenKdsPage.jsx    # Màn hình KDS Bếp điều phối hàng đợi FIFO
│   │   │   └── README.md
│   │   ├── waiter/
│   │   │   └── WaiterDisplayPage.jsx # Màn hình nhân viên phục vụ tại bàn
│   │   └── README.md
│   │
│   ├── routes/                       # Hệ thống định tuyến và bảo vệ Route Guards
│   │   ├── index.jsx                 # Cấu hình Router React Router v7 & phân quyền PermissionRoute
│   │   ├── ProtectedRoute.jsx        # Route Guard chặn truy cập chưa đăng nhập
│   │   └── README.md
│   │
│   ├── stores/                       # Quản lý State toàn cục bằng Zustand
│   │   ├── useAuthStore.js           # Lưu token JWT, thông tin user, vai trò và phân quyền
│   │   ├── useCartStore.js           # Lưu giỏ hàng khách, danh sách món, số lượng, tổng tiền
│   │   ├── useKdsStore.js            # Lưu danh sách đợt order tại bếp KDS, bộ lọc và tổng hợp
│   │   ├── useTableSessionStore.js   # Lưu sessionToken, deviceToken, isHost, bàn hiện tại, countdown
│   │   └── README.md
│   │
│   ├── utils/                        # Hàm tiện ích thuần túy (Pure Functions)
│   │   ├── formatters.js             # formatCurrencyVND(50000) -> "50.000 ₫", formatDateTime
│   │   ├── validators.js             # validatePasscode(pin), validateQuantity(1..99)
│   │   └── README.md
│   │
│   ├── App.jsx                       # Bọc Router Provider
│   ├── index.css                     # Nạp Tailwind CSS và custom styles
│   ├── main.jsx                      # Mount React DOM vào #root
│   └── README.md
│
├── .env                              # Biến môi trường cục bộ
├── .env.example                      # Mẫu khai báo biến môi trường
├── .gitignore
├── index.html                        # File HTML gốc
├── package.json                      # Quản lý dependencies & scripts
├── postcss.config.js
├── tailwind.config.js
└── vite.config.js                    # Alias '@' trỏ vào src/
```

---

## 3. Phân Tích Chuyên Sâu Các Phân Hệ Nghiệp Vụ Cốt Lõi

### 3.1. Phân Hệ Thực Khách Tại Bàn (`features/customer` & `pages/customer`)
- **`TableEntryPage.jsx` (Quét QR & Xác thực mã PIN 4 số):**
  - Khi khách quét QR dán trên bàn (URL dạng `/table/:tableId`), trang hiển thị giao diện nhập mã PIN 4 số an toàn.
  - Tích hợp **Lá chắn chống Brute-Force (Passcode Lockout Shield)**: Nếu nhập sai quá 5 lần liên tiếp, hệ thống khóa tạm thời 60 giây và hiển thị đồng hồ đếm ngược ngăn khách tiếp tục thử dò mã.
  - Khi nhập đúng PIN, server trả về `sessionToken`, `deviceToken` và quyền hạn `isHost`. Thiết bị đầu tiên mở bàn là **Chủ Bàn (Host)**, các thiết bị tiếp theo là **Thành Viên (Member)**. Toàn bộ thông tin này được lưu vào `useTableSessionStore`.
- **`MenuPage.jsx` (Thực đơn & Gọi món cộng tác):**
  - Màn hình chính của khách hàng hiển thị banner cộng tác `CollaborativeBanner` (hiển thị số thiết bị đang cùng ngồi ăn tại bàn).
  - Khách hàng xem danh mục món ăn (`CustomerCategorySidebar`), danh sách món dạng thẻ trực quan (`CustomerDishCard`).
  - Hỗ trợ giỏ hàng đa thiết bị: Drawer giỏ hàng trên mobile (`CustomerCartDrawer`) và Sidebar giỏ hàng trên desktop (`CustomerCartSidebar`).
  - Nút "Gửi đơn vào bếp" chỉ sáng và bấm được khi thiết bị có quyền **Chủ Bàn (Host)**. Khách thành viên được thông báo chỉ Chủ Bàn mới có quyền chốt order để tránh việc đặt món trùng lặp.
- **`TableTransferModal.jsx` (Chuyển Bàn & Ghép Bàn Thông Minh):**
  - Khách hàng (Chủ Bàn) có thể tự yêu cầu Chuyển bàn (chuyển sang bàn trống) hoặc Ghép bàn (nhập tiệc với bàn bạn bè).
  - Áp dụng quy trình **2-Phase Lock**: Khách nhập mã PIN của bàn đích để xác thực 2 chiều (chống gian lận chuyển nhầm hoặc đổ nợ đơn hàng).
  - Khi xác nhận thành công, modal kích hoạt đếm ngược 5 giây (`Smooth Handshake Modal`) trước khi điều hướng mượt mà toàn bộ thiết bị sang bàn mới.
- **`TableDevicesModal.jsx` (Quản lý Thiết Bị Tại Bàn):**
  - Cho phép Chủ Bàn xem danh sách tất cả các thiết bị đang kết nối vào bàn ăn của mình.
  - Cung cấp tính năng **Đá thiết bị (Kick Device)** để loại bỏ người lạ quét nhầm mã QR của bàn.
  - Cung cấp tính năng **Nhường quyền Chủ Bàn (Transfer Host)** cho thiết bị của bạn bè ngồi cùng bàn.

### 3.2. Phân Hệ Điều Phối Bếp Thông Minh KDS (`features/kitchen` & `pages/kitchen`)
- **`KitchenKdsPage.jsx` (Màn hình KDS Hàng Đợi FIFO):**
  - Được thiết kế tối ưu cho màn hình cảm ứng gắn tại khu vực chế biến bếp lẩu.
  - Lắng nghe realtime qua STOMP WebSocket topic `/topic/kitchen/orders` bằng hook `useKitchenSocket`.
  - Hiển thị các thẻ đợt gọi món `KdsOrderCard` sắp xếp nghiêm ngặt theo thứ tự thời gian vào bếp (First-In, First-Out).
  - Tích hợp đồng hồ bấm giờ trực quan theo thời gian thực (hiển thị số phút từ khi khách gửi đơn, đổi màu cảnh báo vàng/đỏ nếu món chờ quá lâu).
- **`KdsAggregatedView.jsx` (Chế Độ Tổng Hợp Món Nấu Gấp):**
  - Bếp trưởng có thể chuyển đổi linh hoạt giữa chế độ xem theo từng bàn và chế độ xem **Tổng hợp món**.
  - Gom toàn bộ số lượng các món cùng loại của tất cả các bàn (ví dụ: tổng cộng 8 đĩa Bò Wagyu, 5 nồi Nước Lẩu Cay Tứ Xuyên) để đầu bếp chế biến hàng loạt một lần, tối ưu hóa công suất bếp tối đa trong giờ cao điểm.
- **`OutOfStockModal.jsx` & `ConfirmOutOfStockModal.jsx` (Báo Hết Món Khẩn Cấp):**
  - Khi bếp cạn nguyên liệu đột xuất, nhân viên bếp chỉ cần 1 chạm để báo hết món (`reportOutOfStock`).
  - Hệ thống lập tức bắn sự kiện WebSocket `/topic/menu-items`, cập nhật đồng thời trên toàn bộ màn hình thực đơn của tất cả khách hàng tại bàn và màn hình quản trị menu. Món bị hết sẽ hiển thị nhãn "Tạm hết hàng" và khách không thể thêm vào giỏ.

### 3.3. Phân Hệ Trạm Phục Vụ Waiter (`features/waiter` & `pages/waiter`)
- **`WaiterDisplayPage.jsx` (Màn hình Phục vụ tại sảnh):**
  - Dành riêng cho nhân sự chạy bàn / phục vụ sảnh nhà hàng.
  - Lắng nghe sự kiện WebSocket `/topic/waiter/orders` và `/topic/tables`:
    - Khi bếp bấm hoàn tất nấu một món (`SERVED`), màn hình phục vụ ngay lập tức nhận thông báo kèm số bàn cụ thể để nhân viên nhanh chóng bưng món lên bàn cho khách.
    - Tiếp nhận tín hiệu chuông gọi phục vụ từ khách (`CallStaffLog`) kèm âm thanh cảnh báo.
  - Cho phép nhân viên phục vụ xác nhận thu tiền mặt trực tiếp tại bàn hoặc chuyển trạng thái bàn sang `CLEANING` khi khách rời đi.

### 3.4. Phân Hệ Quản Lý Sơ Đồ Bàn, Dynamic QR & Cụm Bàn (`features/tables` & `pages/admin`)
- **`TableManagePage.jsx` (Sơ Đồ Bàn POS Floor View):**
  - Hiển thị trực quan toàn bộ mặt bằng nhà hàng chia theo phân khu `COMMON` (Khu sảnh chung) và `VIP` (Phòng VIP Hoàng Triều) qua `FloorAreaSection`.
  - Mỗi bàn được thể hiện bằng `TableCard` với màu sắc trạng thái chuẩn:
    - Xanh lá: `AVAILABLE` (Bàn trống sẵn sàng đón khách).
    - Đỏ cam: `OCCUPIED` (Bàn đang có khách ngồi ăn, hiển thị thời gian ngồi, tổng tiền tạm tính, cờ khóa order).
    - Vàng nâu: `CLEANING` (Bàn đang dọn dẹp).
  - Hỗ trợ **Mô hình Cụm Bàn Liên Kết (Master - Slave Table Clustering)**: Bàn chính (Master) hiển thị nhãn cụm kèm danh sách các bàn phụ (Slaves) đang ghép vào. Mọi order của bàn phụ được dồn về bàn chính và thanh toán tập trung tại bàn chính.
- **`AdminTableDetailModal.jsx` (Chi Tiết Bàn & Can Thiệp Order):**
  - Thu ngân/Quản lý xem chi tiết toàn bộ các đợt order của bàn, tổng tiền món, VAT, phụ thu.
  - Nút bật/tắt quyền order của bàn (`isOrderLocked` theo QĐ3, UC29).
  - Nút đóng bàn & xác nhận thanh toán (Tiền mặt / VietQR).
- **`AdminTableDevicesModal.jsx` (Can Thiệp Quản Trị Thiết Bị):**
  - Quản lý có quyền tối cao xem tất cả thiết bị kết nối vào bàn, cưỡng chế đá thiết bị (`adminKickDevice`) hoặc chỉ định lại quyền Chủ Bàn (`adminResetHost`).
- **`AdminTablesQrPage.jsx` & `TableQrPrintModal.jsx` (In Ấn Dynamic QR):**
  - Trang quản lý tạo mã QR động chứa `sessionToken` bảo mật theo QĐ2.
  - Hỗ trợ in hàng loạt mã QR kèm logo thương hiệu và mã PIN 4 số để dán tại từng bàn ăn.

### 3.5. Phân Hệ Quản Lý Nhân Sự & Phân Quyền (`features/employee` & `pages/admin`)
- **`AdminEmployeeManagePage.jsx`:**
  - Quản lý toàn bộ danh sách tài khoản nhân sự nhà hàng.
  - Bộ lọc vai trò `AdminEmployeeRoleRibbon`: `ADMIN`, `MANAGER`, `KITCHEN`, `STAFF`.
  - Thẻ thống kê `AdminEmployeeStats`: Đếm tổng nhân sự, số tài khoản đang kích hoạt (`ACTIVE`), số tài khoản đang tạm khóa (`INACTIVE`).
  - Thao tác nhanh qua `AdminEmployeeTableRow`: Đổi trạng thái kích hoạt/khóa tài khoản, reset mật khẩu, mở modal sửa thông tin.
  - `AdminEmployeeAccountModal.jsx`: Tạo tài khoản mới, tích hợp gửi email thông tin đăng nhập tự động qua Mail Service backend.

### 3.6. Phân Hệ Hóa Đơn & Lịch Sử Doanh Thu (`features/invoices` & `pages/admin`)
- **`AdminInvoicesPage.jsx`:**
  - Tra cứu và lọc lịch sử hóa đơn theo: Từ ngày - Đến ngày, Trạng thái (`PAID`, `PENDING`, `CANCELLED`), Phương thức thanh toán (`CASH`, `VIETQR`).
  - Hiển thị bảng danh sách hóa đơn kèm mã tra cứu duy nhất (VD: `HD-20260914-0001`).
- **`InvoiceDetailModal.jsx`:**
  - Xem chi tiết từng món ăn trong từng đợt order của hóa đơn, đơn giá tại thời điểm gọi, tên nhân viên thu ngân, chiết khấu khuyến mãi và thuế VAT.
  - Hỗ trợ in hóa đơn thanh toán cho khách lưu giữ.

### 3.7. Phân Hệ Quản Trị Thực Đơn (`features/menu` & `pages/admin`)
- **`MenuManagePage.jsx`:**
  - CRUD món ăn và danh mục thực đơn.
  - Tích hợp tải lên ảnh món ăn trực tiếp lên Cloudinary qua `uploadService.js`.
  - Bật/tắt trạng thái Còn hàng / Hết hàng của từng món.
  - Điều chỉnh thứ tự hiển thị ưu tiên của các danh mục món trên thanh cuộn.

---

## 4. Quản Lý State Toàn Cục (Zustand Stores)

Hệ thống sử dụng **Zustand 5** để quản lý trạng thái toàn cục nhờ dung lượng siêu nhẹ, không cần boilerplate Context Provider phức tạp và cho phép cập nhật state ngoài phạm vi React Component (ví dụ trong WebSocket callbacks).

### 4.1. `useAuthStore.js` (Xác Thực & Phân Quyền)
- **State:** `user`, `token`, `isAuthenticated`.
- **Actions:**
  - `login(token, user)`: Lưu token JWT và thông tin user vào `localStorage`.
  - `logout()`: Xóa sạch token, reset state và chuyển hướng về `/login`.
  - `updateProfile(data)`: Cập nhật thông tin hiển thị của tài khoản.

### 4.2. `useTableSessionStore.js` (Phiên Bàn Ăn, Thiết Bị & Bảo Mật)
- **State:**
  - `tableNumber`, `tableName`, `sessionToken`, `deviceToken`, `isHost`.
  - `tableStatus`, `isOrderLocked`, `activeDeviceCount`.
  - `masterTableNumber`, `linkedTables`: Lưu trữ thông tin Cụm bàn liên kết.
  - `transferCountdown`: Bộ đếm ngược thời gian chuyển bàn (5 giây).
- **Actions:**
  - `initSession(sessionData)`: Thiết lập phiên bàn ăn sau khi nhập PIN thành công.
  - `setHostStatus(isHost)`: Cập nhật quyền Chủ Bàn.
  - `updateTableLock(locked)`: Cập nhật cờ khóa order khi nhận sự kiện WebSocket.
  - `clearSession()`: Xóa sạch phiên khi bàn thanh toán hoặc bị đá ra khỏi bàn.

### 4.3. `useCartStore.js` (Giỏ Hàng Cộng Tác)
- **State:** `cartItems`, `totalQuantity`, `totalPrice`, `note`.
- **Actions:**
  - `addToCart(item)`: Thêm món mới hoặc tăng số lượng nếu đã có trong giỏ.
  - `updateQuantity(itemId, quantity)`: Cập nhật số lượng món ($1 \le N \le 99$ theo QĐ9).
  - `removeFromCart(itemId)`: Xóa món khỏi giỏ hàng.
  - `clearCart()`: Làm rỗng giỏ hàng sau khi gửi bếp thành công.

### 4.4. `useKdsStore.js` (Hàng Đợi Trạm Bếp)
- **State:** `kdsOrders`, `activeFilter` ('ALL', 'PENDING', 'COOKING', 'COMPLETED'), `viewMode` ('BY_ORDER', 'AGGREGATED').
- **Actions:**
  - `setOrders(orders)`: Nạp danh sách đơn hàng bếp.
  - `updateOrderItemStatus(itemId, newStatus)`: Cập nhật trạng thái từng món.
  - `toggleViewMode()`: Chuyển đổi giữa chế độ xem theo đợt gọi món và chế độ tổng hợp món nấu gấp.

---

## 5. Hệ Thống Định Tuyến & Bảo Vệ Tuyến Đường (Routing & Guards)

Sử dụng **React Router v7** (`createBrowserRouter`) kết hợp cơ chế kiểm soát truy cập phân tầng:

```
[Mọi Request]
     │
     ├── Công khai: /login (Đăng nhập nhân sự), /table/:tableId (Nhập PIN vào bàn)
     │
     ├── Khách hàng tại bàn: /menu, /customer/menu (Bọc bởi CustomerLayout)
     │
     ├── Yêu cầu Đăng nhập: Bọc bởi <ProtectedRoute> (Kiểm tra JWT Token hợp lệ)
     │       │
     │       ├── Bếp KDS: /kitchen (Yêu cầu quyền KITCHEN, MANAGER, ADMIN)
     │       ├── Phục vụ: /waiter (Yêu cầu quyền WAITER, MANAGER, ADMIN)
     │       │
     │       └── Khu vực Admin: /admin/* (Bọc bởi AdminLayout + PermissionRoute)
     │               ├── /admin/dashboard   (Quyền: DASHBOARD)
     │               ├── /admin/tables      (Quyền: TABLES)
     │               ├── /admin/menu        (Quyền: MENU)
     │               ├── /admin/tables-qr   (Quyền: TABLES_QR)
     │               ├── /admin/invoices    (Quyền: INVOICES)
     │               ├── /admin/employees   (Chỉ dành cho ADMIN)
     │               └── /admin/profile     (Tất cả nhân sự đã đăng nhập)
```

- **`PermissionRoute`:** Tự động điều hướng nhân viên về đúng trang họ có quyền truy cập (Fallback Redirect) nếu họ cố tình gõ URL của trang vượt quá quyền hạn (ví dụ: Nhân viên phục vụ vào `/admin/employees` sẽ được redirect về `/waiter`).
- **`AdminIndexRedirect`:** Khi truy cập `/admin`, tự động chuyển hướng đến trang chức năng mặc định phù hợp nhất với quyền hạn của tài khoản.

---

## 6. Lớp Kết Nối Mạng & Đồng Bộ Realtime (`lib/`)

### 6.1. `axios.js` (HTTP Client Chuẩn Hóa)
- Tự động gắn header `Authorization: Bearer <token>` nếu đã đăng nhập.
- Tự động gắn header `X-Session-Token: <sessionToken>` và `X-Device-Token: <deviceToken>` khi gọi các API dành cho khách hàng tại bàn.
- Interceptor bắt lỗi toàn cục: Nếu nhận HTTP 401 Unauthorized từ API quản trị, tự động kích hoạt `logout()` và đưa người dùng về trang đăng nhập.

### 6.2. `websocket.js` (STOMP Client over SockJS)
- Kết nối tới backend tại endpoint `/ws`.
- Hỗ trợ cơ chế tự động tái kết nối (Auto-Reconnect với Heartbeat kiểm tra đường truyền mỗi 10 giây).
- Quản lý đăng ký Subscriptions tập trung:
  - Tự động huỷ đăng ký (Unsubscribe) khi Component unmount để chống rò rỉ bộ nhớ (Memory Leak).
  - Tự động đăng ký lại các kênh lắng nghe khi đường truyền WebSocket được khôi phục sau sự cố mất mạng.
