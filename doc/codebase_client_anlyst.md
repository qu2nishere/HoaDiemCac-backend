# PHÂN TÍCH VÀ THIẾT KẾ CẤU TRÚC CODEBASE CLIENT CHUẨN DOANH NGHIỆP (REACT + VITE)

> **Đề tài:** Xây dựng và kiểm thử hệ thống quét mã QR gọi món tại bàn theo thời gian thực  
> **Môn học:** Kiểm thử phần mềm — Trường ĐH Sư phạm Kỹ thuật TP.HCM (HCMUTE)  
> **Công nghệ áp dụng:** React 19, Vite, Tailwind CSS, Zustand, Axios, STOMP/WebSocket, Vitest & React Testing Library.

---

## 1. Tổng quan và Triết lý Thiết kế

Trong các dự án phần mềm quy mô doanh nghiệp, việc tổ chức mã nguồn Frontend quyết định trực tiếp đến **khả năng mở rộng (Scalability)**, **tính dễ bảo trì (Maintainability)**, **hiệu suất làm việc nhóm (Team Collaboration)** và đặc biệt là **tính dễ kiểm thử (Testability)**.

Thay vì tổ chức theo cách truyền thống "Group by Type" (gom tất cả components, hooks, services vào các thư mục khổng lồ), codebase của hệ thống được xây dựng theo mô hình **Feature-based Architecture** (chuẩn **Bulletproof React**). Kiến trúc này chia nhỏ ứng dụng thành các phân hệ nghiệp vụ độc lập (Vertical Slices), kết hợp cùng sự phân tách rành mạch giữa **Khung giao diện (Layouts)**, **Nội dung hiển thị (Pages)**, và **Dịch vụ mạng (Network & Realtime)**.

### Mục tiêu cốt lõi của kiến trúc:
1. **Cô lập nghiệp vụ (High Cohesion, Low Coupling):** Mỗi tính năng (Giỏ hàng, Thực đơn, Bếp, Bàn ăn) là một module tự đóng gói (Self-contained). Thay đổi ở một module không làm vỡ các module khác.
2. **Tối ưu hóa thời gian thực (Realtime Optimization):** Quản lý luồng dữ liệu WebSocket độc lập, không kích hoạt re-render toàn bộ cây DOM.
3. **Phục vụ tối đa cho Môn Kiểm thử phần mềm:** Tách bạch rõ rệt giữa **Pure Logic** (dễ viết Unit Test) và **UI Presentation** (dễ viết Component Test bằng Vitest / Testing Library).

---

## 2. Sơ đồ Cây Thư Mục Toàn Diện (Directory Tree)

Dưới đây là sơ đồ cấu trúc hoàn chỉnh của thư mục `client/`:

```text
client/
├── README.md                     # Tài liệu tổng quan root: package.json, vite.config.js, tailwind, env...
│
├── public/
│   └── README.md                 # Tài liệu public: favicon.svg, sounds/ting-ting.mp3, qr-placeholder.png
│
├── src/
│   ├── README.md                 # Tài liệu src root: main.jsx, App.jsx, index.css, App.css
│   │
│   ├── assets/
│   │   ├── README.md             # Tài liệu tổng quan assets
│   │   ├── images/
│   │   │   └── README.md         # Tài liệu images: hero-banner.jpg, vietqr-logo.png, empty-cart.svg...
│   │   └── icons/
│   │       └── README.md         # Tài liệu icons SVG thương hiệu
│   │
│   ├── config/
│   │   └── README.md             # Tài liệu config: env.js, constants.js (ORDER_STATUS, TABLE_STATUS...)
│   │
│   ├── lib/
│   │   └── README.md             # Tài liệu lib: axios.js (Interceptors), websocket.js (STOMP Client)
│   │
│   # -------------------------------------------------------------------------
│   # GIAO DIỆN DÙNG CHUNG (SHARED / GENERIC UI)
│   # -------------------------------------------------------------------------
│   ├── components/
│   │   ├── README.md             # Tài liệu tổng quan components
│   │   │
│   │   ├── ui/
│   │   │   └── README.md         # Tài liệu UI câm: Button.jsx, Input.jsx, Modal.jsx, Badge.jsx, Spinner.jsx, Card.jsx
│   │   │
│   │   ├── feedback/
│   │   │   └── README.md         # Tài liệu feedback: Toast.jsx, ErrorBoundary.jsx
│   │   │
│   │   └── navigation/           # 👉 THANH ĐIỀU HƯỚNG VÀ HEADER DÙNG CHUNG
│   │       └── README.md         # 1 FILE DUY NHẤT ghi cho 4 file JSX:
│   │                             # - CustomerHeader.jsx (Số bàn, chuông gọi phục vụ)
│   │                             # - KitchenHeader.jsx (Đồng hồ thực, nút tắt/bật chuông)
│   │                             # - AdminHeader.jsx (Topbar quản trị, avatar, logout)
│   │                             # - AdminSidebar.jsx (Menu trái điều hướng các trang)
│   │
│   # -------------------------------------------------------------------------
│   # KHUNG GIAO DIỆN CỐ ĐỊNH (LAYOUTS)
│   # -------------------------------------------------------------------------
│   ├── layouts/
│   │   └── README.md             # 1 FILE DUY NHẤT ghi cho 3 Layouts:
│   │                             # - CustomerLayout.jsx (Header bàn + <Outlet /> + Cart button)
│   │                             # - KitchenLayout.jsx (KDS Header + <Outlet /> fullscreen)
│   │                             # - AdminLayout.jsx (AdminSidebar + Topbar + <Outlet />)
│   │
│   # -------------------------------------------------------------------------
│   # MODULES NGHIỆP VỤ ĐỘC LẬP (FEATURE MODULES)
│   # -------------------------------------------------------------------------
│   ├── features/
│   │   ├── README.md             # Tài liệu tổng quan kiến trúc 7 module nghiệp vụ
│   │   │
│   │   ├── auth/                 # Phân hệ Xác thực
│   │   │   ├── README.md         # Tổng quan auth & index.js
│   │   │   ├── api/README.md     # authApi.js (login, refresh, logout)
│   │   │   └── components/README.md # LoginForm.jsx
│   │   │
│   │   ├── menu/                 # Phân hệ Thực đơn
│   │   │   ├── README.md         # Tổng quan menu & index.js
│   │   │   ├── api/README.md     # menuApi.js (getCategories, getMenuItems, getMenuItemDetail)
│   │   │   ├── data/README.md    # mockMenuItems.js (MENU_CATEGORIES, initialMenuItems)
│   │   │   └── components/README.md # AdminMenuHeader.jsx, MenuSummaryControlBar.jsx, CategoryFilterRibbon.jsx, MenuItemRow.jsx, MenuItemModal.jsx, KdsToastNotification.jsx, MenuPagination.jsx
│   │   │
│   │   ├── cart/                 # Phân hệ Giỏ hàng cộng tác Realtime
│   │   │   ├── README.md         # Tổng quan cart & index.js
│   │   │   ├── api/README.md     # cartApi.js (getCart, updateCartItem, submitCartToKitchen)
│   │   │   ├── hooks/README.md   # useCartSync.js (Đồng bộ giỏ hàng qua STOMP)
│   │   │   └── components/README.md # CartItem.jsx, CartDrawer.jsx, CartFloatingButton.jsx
│   │   │
│   │   ├── orders/               # Phân hệ Đặt món & Tiến độ
│   │   │   ├── README.md         # Tổng quan orders & index.js
│   │   │   ├── api/README.md     # orderApi.js (getOrderHistory)
│   │   │   └── components/README.md # OrderTimeline.jsx, OrderItemStatus.jsx
│   │   │
│   │   ├── kitchen/              # Phân hệ Điều phối Bếp KDS (FIFO)
│   │   │   ├── README.md         # Tổng quan kitchen & index.js
│   │   │   ├── api/README.md     # kitchenApi.js (getCookingQueue, updateStatus, outOfStock)
│   │   │   ├── hooks/README.md   # useKitchenSocket.js (Nhận đơn mới, phát chuông ting-ting)
│   │   │   └── components/README.md # KdsOrderCard.jsx, OutOfStockButton.jsx
│   │   │
│   │   ├── tables/               # Phân hệ Bàn ăn & Dynamic Session QR Token
│   │   │   ├── README.md         # Tổng quan tables & index.js
│   │   │   ├── api/README.md     # tableApi.js (getAllTables, generateQrToken, revokeQrToken)
│   │   │   └── components/README.md # TableGrid.jsx, QrCodeModal.jsx
│   │   │
│   │   └── payment/              # Phân hệ Thanh toán VietQR Napas247
│   │       ├── README.md         # Tổng quan payment & index.js
│   │       ├── api/README.md     # paymentApi.js (requestPayment, getVietQrPayload)
│   │       └── components/README.md # VietQrModal.jsx
│   │
│   # -------------------------------------------------------------------------
│   # MÀN HÌNH HIỂN THỊ CỤ THỂ (PAGES)
│   # -------------------------------------------------------------------------
│   ├── pages/
│   │   ├── README.md             # NotFoundPage.jsx
│   │   ├── customer/
│   │   │   └── README.md         # MenuPage.jsx, CartPage.jsx, OrderStatusPage.jsx
│   │   ├── kitchen/
│   │   │   └── README.md         # KitchenKdsPage.jsx
│   │   ├── admin/
│   │   │   └── README.md         # DashboardPage.jsx, TableManagePage.jsx, MenuManagePage.jsx
│   │   └── auth/
│   │       └── README.md         # LoginPage.jsx
│   │
│   # -------------------------------------------------------------------------
│   # ĐỊNH TUYẾN, STATE TOÀN CỤC & UTILS
│   # -------------------------------------------------------------------------
│   ├── routes/
│   │   └── README.md             # index.jsx (cây route), ProtectedRoute.jsx (kiểm tra JWT)
│   │
│   ├── stores/
│   │   └── README.md             # useCartStore.js, useAuthStore.js, useTableSessionStore.js
│   │
│   ├── utils/
│   │   └── README.md             # formatters.js (VND, time), validators.js (1 <= N <= 99)
│   │
│   # -------------------------------------------------------------------------
│   # KIỂM THỬ TỰ ĐỘNG (TESTING SUITE)
│   # -------------------------------------------------------------------------
│   └── test/
│       └── README.md             # setup.js (jest-dom), test-utils.jsx (custom render wrapper)
```CẤU TRÚC CODEBASE CLIENT CHUẨN DOANH NGHIỆP (REACT + VITE)

> **Đề tài:** Xây dựng và kiểm thử hệ thống quét mã QR gọi món tại bàn theo thời gian thực  
> **Môn học:** Kiểm thử phần mềm — Trường ĐH Sư phạm Kỹ thuật TP.HCM (HCMUTE)  
> **Công nghệ áp dụng:** React 19, Vite, Tailwind CSS, Zustand, Axios, STOMP/WebSocket, Vitest & React Testing Library.

---

## 1. Tổng quan và Triết lý Thiết kế

Trong các dự án phần mềm quy mô doanh nghiệp, việc tổ chức mã nguồn Frontend quyết định trực tiếp đến **khả năng mở rộng (Scalability)**, **tính dễ bảo trì (Maintainability)**, **hiệu suất làm việc nhóm (Team Collaboration)** và đặc biệt là **tính dễ kiểm thử (Testability)**.

Thay vì tổ chức theo cách truyền thống "Group by Type" (gom tất cả components, hooks, services vào các thư mục khổng lồ), codebase của hệ thống được xây dựng theo mô hình **Feature-based Architecture** (chuẩn **Bulletproof React**). Kiến trúc này chia nhỏ ứng dụng thành các phân hệ nghiệp vụ độc lập (Vertical Slices), kết hợp cùng sự phân tách rành mạch giữa **Khung giao diện (Layouts)**, **Nội dung hiển thị (Pages)**, và **Dịch vụ mạng (Network & Realtime)**.

### Mục tiêu cốt lõi của kiến trúc:
1. **Cô lập nghiệp vụ (High Cohesion, Low Coupling):** Mỗi tính năng (Giỏ hàng, Thực đơn, Bếp, Bàn ăn) là một module tự đóng gói (Self-contained). Thay đổi ở một module không làm vỡ các module khác.
2. **Tối ưu hóa thời gian thực (Realtime Optimization):** Quản lý luồng dữ liệu WebSocket độc lập, không kích hoạt re-render toàn bộ cây DOM.
3. **Phục vụ tối đa cho Môn Kiểm thử phần mềm:** Tách bạch rõ rệt giữa **Pure Logic** (dễ viết Unit Test) và **UI Presentation** (dễ viết Component Test bằng Vitest / Testing Library).

---

## 2. Sơ đồ Cây Thư Mục Toàn Diện (Directory Tree)

Dưới đây là sơ đồ cấu trúc hoàn chỉnh của thư mục `client/`:

```text
client/
├── public/                       # Tài nguyên công khai truy cập trực tiếp
│   ├── favicon.svg               # Biểu tượng nhà hàng trên tab trình duyệt
│   ├── sounds/
│   │   └── ting-ting.mp3         # Chuông báo order mới cho trạm bếp KDS
│   └── public_assets.md          # Tài liệu mô tả tài nguyên tĩnh
│
├── src/
│   ├── assets/                   # Tài nguyên đa phương tiện được nén và đóng gói
│   │   ├── images/images.md      # Banner ẩm thực, logo VietQR, hình minh họa
│   │   └── icons/icons.md        # Icons thương hiệu tùy biến
│   │
│   ├── config/                   # Cấu hình tĩnh và kiểm soát biến môi trường
│   │   ├── env.js.md             # Đọc và validate URL backend, WS endpoint
│   │   └── constants.js.md       # Hằng số Enum (Trạng thái món, Bàn ăn, Quyền hạn)
│   │
│   ├── lib/                      # Khởi tạo và cấu hình các thư viện bên thứ 3
│   │   ├── axios.js.md           # Cấu hình Axios instance kèm Token Interceptor
│   │   └── websocket.js.md       # Quản lý kết nối STOMP Client (Spring Boot WebSocket)
│   │
│   # -------------------------------------------------------------------------
│   # GIAO DIỆN DÙNG CHUNG (SHARED / GENERIC UI)
│   # -------------------------------------------------------------------------
│   ├── components/
│   │   ├── ui/                   # Reusable "Dumb" Components (Tailwind CSS)
│   │   │   ├── Button.jsx.md     # Nút bấm đa năng (Primary, Danger, Loading state)
│   │   │   ├── Input.jsx.md      # Ô nhập dữ liệu có nhãn và thông báo lỗi
│   │   │   ├── Modal.jsx.md      # Hộp thoại pop-up (Chi tiết món, Xác nhận)
│   │   │   ├── Badge.jsx.md      # Nhãn trạng thái (Đang chờ, Đang nấu, Đã xong)
│   │   │   ├── Spinner.jsx.md    # Vòng xoay trạng thái đang tải (Loading)
│   │   │   └── Card.jsx.md       # Thẻ khung viền bọc nội dung
│   │   │
│   │   ├── feedback/             # Thành phần phản hồi người dùng
│   │   │   ├── Toast.jsx.md      # Thông báo nổi góc màn hình
│   │   │   └── ErrorBoundary.jsx.md # Bắt lỗi crash giao diện runtime
│   │   │
│   │   └── navigation/           # 👉 THANH ĐIỀU HƯỚNG VÀ HEADER DÙNG CHUNG
│   │       ├── CustomerHeader.jsx.md # Header khách: Số bàn B01, chuông gọi phục vụ
│   │       ├── KitchenHeader.jsx.md  # Header bếp: Đồng hồ thực, nút tắt/bật âm thanh
│   │       ├── AdminHeader.jsx.md    # Topbar quản trị: Avatar, tên tài khoản, logout
│   │       └── AdminSidebar.jsx.md   # Menu trái: Bàn ăn, Thực đơn, Doanh thu, QR Token
│   │
│   # -------------------------------------------------------------------------
│   # KHUNG GIAO DIỆN CỐ ĐỊNH (LAYOUTS)
│   # -------------------------------------------------------------------------
│   ├── layouts/
│   │   ├── CustomerLayout.jsx.md # Khung di động cho khách: CustomerHeader + <Outlet />
│   │   ├── KitchenLayout.jsx.md  # Khung KDS toàn màn hình cho bếp: KitchenHeader + <Outlet />
│   │   └── AdminLayout.jsx.md    # Khung quản trị Desktop 2 cột: Sidebar + Header + <Outlet />
│   │
│   # -------------------------------------------------------------------------
│   # MODULES NGHIỆP VỤ ĐỘC LẬP (FEATURE MODULES)
│   # -------------------------------------------------------------------------
│   ├── features/
│   │   ├── auth/                 # Xác thực & Phân quyền quản trị
│   │   │   ├── api/authApi.js.md
│   │   │   ├── components/LoginForm.jsx.md
│   │   │   └── index.js.md
│   │   │
│   │   ├── menu/                 # Xem & Quản lý thực đơn
│   │   │   ├── api/menuApi.js.md
│   │   │   ├── components/CategoryTabs.jsx.md
│   │   │   ├── components/FoodCard.jsx.md
│   │   │   ├── components/FoodDetailModal.jsx.md
│   │   │   └── index.js.md
│   │   │
│   │   ├── cart/                 # Giỏ hàng cộng tác thời gian thực (Collaborative Cart)
│   │   │   ├── api/cartApi.js.md
│   │   │   ├── components/CartItem.jsx.md
│   │   │   ├── components/CartDrawer.jsx.md
│   │   │   ├── components/CartFloatingButton.jsx.md
│   │   │   ├── hooks/useCartSync.js.md # Lắng nghe STOMP cập nhật giỏ hàng từ người cùng bàn
│   │   │   └── index.js.md
│   │   │
│   │   ├── orders/               # Đặt món & Theo dõi tiến độ phục vụ
│   │   │   ├── api/orderApi.js.md
│   │   │   ├── components/OrderTimeline.jsx.md
│   │   │   ├── components/OrderItemStatus.jsx.md
│   │   │   └── index.js.md
│   │   │
│   │   ├── kitchen/              # Màn hình điều phối bếp KDS (FIFO)
│   │   │   ├── api/kitchenApi.js.md
│   │   │   ├── components/KdsOrderCard.jsx.md
│   │   │   ├── components/OutOfStockButton.jsx.md
│   │   │   ├── hooks/useKitchenSocket.js.md # Nhận order mới & phát chuông Ting-ting
│   │   │   └── index.js.md
│   │   │
│   │   ├── tables/               # Sơ đồ bàn & Quản lý Dynamic Session QR
│   │   │   ├── api/tableApi.js.md
│   │   │   ├── components/TableGrid.jsx.md
│   │   │   ├── components/QrCodeModal.jsx.md # Tạo và tải ảnh mã QR bàn
│   │   │   └── index.js.md
│   │   │
│   │   └── payment/              # Thanh toán VietQR Napas247 / Tiền mặt
│   │       ├── api/paymentApi.js.md
│   │       ├── components/VietQrModal.jsx.md
│   │       └── index.js.md
│   │
│   # -------------------------------------------------------------------------
│   # MÀN HÌNH HIỂN THỊ CỤ THỂ (PAGES)
│   # -------------------------------------------------------------------------
│   ├── pages/
│   │   ├── customer/
│   │   │   ├── MenuPage.jsx.md        # Màn hình xem menu khi quét QR
│   │   │   ├── CartPage.jsx.md        # Màn hình giỏ hàng và gửi bếp
│   │   │   └── OrderStatusPage.jsx.md # Màn hình theo dõi tiến độ món ăn
│   │   │
│   │   ├── kitchen/
│   │   │   └── KitchenKdsPage.jsx.md  # Màn hình trạm bếp KDS theo chuẩn FIFO
│   │   │
│   │   ├── admin/
│   │   │   ├── DashboardPage.jsx.md   # Biểu đồ doanh thu, top món bán chạy
│   │   │   ├── TableManagePage.jsx.md # Quản lý bàn & cấp lại mã QR
│   │   │   └── MenuManagePage.jsx.md  # Thêm/sửa món ăn và danh mục
│   │   │
│   │   ├── auth/
│   │   │   └── LoginPage.jsx.md       # Đăng nhập quản trị viên
│   │   └── NotFoundPage.jsx.md        # Trang 404
│   │
│   # -------------------------------------------------------------------------
│   # ĐỊNH TUYẾN & QUẢN LÝ TRẠNG THÁI TOÀN CỤC
│   # -------------------------------------------------------------------------
│   ├── routes/
│   │   ├── index.jsx.md               # Cấu hình danh sách URL và gán vào Layouts
│   │   └── ProtectedRoute.jsx.md      # Kiểm tra JWT Token và phân quyền truy cập
│   │
│   ├── stores/                        # Global State Management (Zustand)
│   │   ├── useCartStore.js.md         # Giỏ hàng (danh sách món, số lượng, tổng tiền)
│   │   ├── useAuthStore.js.md         # JWT Token và thông tin đăng nhập
│   │   └── useTableSessionStore.js.md # Mã phiên bàn (sessionToken) hiện tại
│   │
│   ├── utils/                         # Hàm tiện ích thuần túy (Pure Functions)
│   │   ├── formatters.js.md           # Định dạng tiền tệ VND, định dạng ngày giờ
│   │   └── validators.js.md           # Kiểm tra số lượng hợp lệ (1 <= N <= 99)
│   │
│   # -------------------------------------------------------------------------
│   # KIỂM THỬ TỰ ĐỘNG (TESTING SUITE)
│   # -------------------------------------------------------------------------
│   ├── test/
│   │   ├── setup.js.md                # Thiết lập Jest-DOM matchers cho Vitest
│   │   └── test-utils.jsx.md          # Custom Render Wrapper kèm Router & Store
│   │
│   ├── App.jsx.md                     # Component gốc bao bọc Providers
│   ├── App.css.md
│   ├── index.css.md                   # Nạp Tailwind CSS & style toàn cục
│   └── main.jsx.md                    # React 19 createRoot mount
│
├── .env.example.md                    # Mẫu khai báo biến môi trường
├── eslint.config.js.md                # Cấu hình chuẩn hóa cú pháp mã nguồn
├── index.html.md                      # File HTML đơn trang gốc
├── package.json.md                    # Quản lý thư viện và scripts (dev, build, test)
├── tailwind.config.js.md              # Cấu hình theme bảng màu ẩm thực
└── vite.config.js.md                  # Cấu hình Vite, Alias '@', và Vitest
```

---

## 3. Phân Tích Chuyên Sâu Từng Tầng Trong Kiến Trúc

### 3.1. Tầng Khung Giao Diện (Layouts) và Màn Hình (Pages)

Sự phân tách giữa `layouts/` và `pages/` là một trong những điểm khác biệt lớn nhất giữa một ứng dụng chắp vá và một sản phẩm chuyên nghiệp:

1. **`CustomerLayout` (Khung cho khách tại bàn):**
   - **Vấn đề giải quyết:** Khi khách đang xem món ăn rồi chuyển sang xem giỏ hàng hoặc xem tiến độ chế biến, thanh `CustomerHeader` (chứa số bàn, mã phiên, và nút chuông gọi phục vụ) **phải đứng yên tuyệt đối**.
   - **Lợi ích kỹ thuật:** Việc không unmount/remount Header giúp duy trì liên tục trạng thái bộ đếm đếm ngược chống spam chuông (Rate Limiting 30s) và giữ vững kết nối WebSocket của bàn mà không bị gián đoạn.

2. **`KitchenLayout` (Khung cho trạm bếp KDS):**
   - Tối ưu hóa cho màn hình tablet ngang hoặc màn hình TV treo trong khu vực bếp. Thiết kế nền tối (Dark mode) chống chói, cố định đồng hồ thời gian thực và nút tắt/bật chuông báo hiệu ứng "Ting-ting".

3. **`AdminLayout` (Khung quản trị):**
   - Bố cục 2 cột tiêu chuẩn: Cột trái là `AdminSidebar` điều hướng, cột phải là `AdminHeader` cùng vùng hiển thị nội dung các trang quản trị bên dưới `<Outlet />`.

---

### 3.2. Tầng Module Nghiệp Vụ (`features/`)

Mỗi module nghiệp vụ hoạt động như một "tiểu ứng dụng" khép kín với cấu trúc chuẩn:
- **`api/`:** Chứa các hàm gọi REST API riêng của module (ví dụ: `menuApi.js`, `cartApi.js`).
- **`components/`:** Chứa các giao diện đặc thù chỉ thuộc về tính năng đó (ví dụ: `FoodCard.jsx`, `KdsOrderCard.jsx`).
- **`hooks/`:** Chứa logic nghiệp vụ phức tạp, đặc biệt là các hook lắng nghe WebSocket thời gian thực (ví dụ: `useCartSync.js`, `useKitchenSocket.js`).
- **`index.js` (Barrel Export):** Đóng vai trò như "Cổng giao tiếp công khai (Public API)". Các phần khác của dự án chỉ được import những gì `index.js` xuất ra, ngăn chặn việc thọc sâu vào mã nguồn nội bộ của module.

---

### 3.3. Tầng Quản Lý Trạng Thái Toàn Cục (`stores/` - Zustand)

Thay vì sử dụng Redux phức tạp với nhiều mã nguồn thừa (boilerplate), hệ thống sử dụng **Zustand** — thư viện State Management hiện đại, nhẹ (~1KB) và có hiệu năng vượt trội:

1. **`useCartStore`:** 
   - Quản lý danh sách món ăn trong giỏ, số lượng từng món, tổng tiền tạm tính.
   - Chứa logic kiểm tra điều kiện ràng buộc số lượng món: $1 \le N \le 99$.
2. **`useTableSessionStore`:**
   - Trích xuất và lưu trữ `sessionToken` từ URL khi khách quét mã QR.
   - Đính kèm mã phiên này vào Header của mọi request gửi lên server để xác thực bàn.
3. **`useAuthStore`:**
   - Lưu trữ JWT Token, thông tin quyền hạn (`ROLE_ADMIN`, `ROLE_KITCHEN`) và trạng thái đăng nhập.

---

### 3.4. Tầng Giao Tiếp Mạng & Thời Gian Thực (`lib/`)

1. **`lib/axios.js`:**
   - Cấu hình sẵn `baseURL` từ biến môi trường.
   - **Request Interceptor:** Tự động gắn JWT Token (`Authorization: Bearer <token>`) và Session Token bàn ăn vào mọi yêu cầu.
   - **Response Interceptor:** Bắt tập trung các mã lỗi HTTP 401 (Hết hạn phiên -> Chuyển hướng về `/login`) và HTTP 403 (Không có quyền).

2. **`lib/websocket.js` (Tương thích 100% với Spring Boot STOMP):**
   - Sử dụng `@stomp/stompjs` kết hợp `sockjs-client`.
   - Lắng nghe các Topic tương ứng:
     - `/topic/table/{sessionToken}/cart`: Đồng bộ giỏ hàng chung đa thiết bị khi nhiều khách cùng ngồi một bàn thao tác.
     - `/topic/kitchen/orders`: Bắn thông báo đơn mới vào màn hình bếp theo chuẩn FIFO.
     - `/topic/table/{sessionToken}/status`: Đồng bộ trạng thái món (từ "Đang nấu" sang "Đã phục vụ").

---

### 3.5. Tầng Kiểm Thử (Testing Suite - Trọng tâm môn Kiểm thử phần mềm)

Cấu trúc này được tối ưu hóa cao nhất để phục vụ viết báo cáo và thực hiện kiểm thử tự động cho đồ án cuối kỳ:

| Cấp độ kiểm thử | Thư viện sử dụng | Đối tượng kiểm thử trong Codebase |
| :--- | :--- | :--- |
| **Unit Testing** | `Vitest` | Kiểm thử các hàm tính toán độc lập trong `src/utils/formatters.js` (đổi tiền VND, ngày giờ) và `src/utils/validators.js` (kiểm tra $1 \le N \le 99$). |
| **State Testing** | `Vitest` | Kiểm thử logic thêm món, sửa số lượng, tính tổng tiền trong `src/stores/useCartStore.js`. |
| **Component Testing** | `@testing-library/react` | Kiểm thử giao diện tương tác: `Button.jsx`, `FoodCard.jsx`, `CustomerHeader.jsx` (kiểm tra chuông gọi, cờ cooldown 30 giây). |
| **Integration Testing** | `Vitest` + `MSW` (Mock Service Worker) | Giả lập luồng khách chọn món -> Giỏ hàng cập nhật -> Bấm nút gửi bếp. |

---

## 4. Năm Nguyên Tắc Vàng Khi Lập Trình Dự Án

1. **Nguyên tắc "Component câm" (Presentational) và "Hook thông minh" (Container):**
   - Component JSX chỉ làm nhiệm vụ vẽ giao diện từ `props` và bắt sự kiện `onClick`, `onChange`.
   - Toàn bộ logic gọi API, tính toán, WebSocket phải được đưa vào **Custom Hook** hoặc **Store**.
2. **Nguyên tắc Path Alias (`@/`):**
   - Luôn sử dụng `@/components/...`, `@/features/...` thay vì đường dẫn tương đối dài dòng `../../..`.
3. **Nguyên tắc Phòng thủ dữ liệu (Validation First):**
   - Mọi dữ liệu số lượng món hay mã phiên bàn đều phải đi qua `validators.js` trước khi gửi lên API backend.
4. **Nguyên tắc Kiểm soát Re-render:**
   - Sử dụng selector trong Zustand (`const count = useCartStore(state => state.items.length)`) để component chỉ render lại khi đúng thuộc tính đó thay đổi.
5. **Đồng bộ song hành Code & Test:**
   - Mỗi khi triển khai một tính năng hoặc hàm mới, file test tương ứng phải được tạo song hành (Co-location).
