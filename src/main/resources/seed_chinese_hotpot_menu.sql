-- =============================================================================
-- HỎA DIỆM CÁC (IMPERIAL CHINESE HOTPOT) - DATA SEEDING SCRIPT
-- =============================================================================
-- Bộ dữ liệu chuẩn chỉ cho nhà hàng Lẩu Trung Hoa / Lẩu Tứ Xuyên - Trùng Khánh
-- Bao gồm: Nước lẩu, Bò thượng hạng, Đặc sản nhúng nội tạng, Viên thả, Rau nấm,
-- Món khai vị Dimsum, Nước uống giải ngấy và Tráng miệng ngự thiện.
-- Cột image_url để NULL để người dùng tự cập nhật URL ảnh CDN sau.
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- -----------------------------------------------------------------------------
-- 1. SEED DANH MỤC MÓN ĂN (CATEGORIES)
-- -----------------------------------------------------------------------------
INSERT INTO `categories` (`name`, `slug`, `description`, `display_order`, `is_active`, `is_system`, `created_at`, `updated_at`)
VALUES
('Nước Lẩu Hoàng Gia', 'nuoc-lau', 'Tinh hoa cốt lẩu hầm 48 giờ kết hợp hoa tiêu, ớt Tứ Xuyên và thảo quả ngự thiện', 1, 1, 0, NOW(), NOW()),
('Bò Thượng Hạng & Wagyu', 'bo-wagyu', 'Thịt bò Wagyu vân mỡ cẩm thạch và bò Black Angus nhập khẩu cao cấp', 2, 1, 0, NOW(), NOW()),
('Đặc Sản Nhúng & Nội Tạng', 'dac-san-nhung', 'Tam Kiếm Khách Trùng Khánh và các món nhúng trứ danh chuẩn phong vị lẩu Trung Hoa', 3, 1, 0, NOW(), NOW()),
('Hải Sản Tươi Sống', 'hai-san', 'Hải sản tự nhiên tươi sống hảo hạng nhúng giữ trọn vị ngọt đậm đà', 4, 1, 0, NOW(), NOW()),
('Viên Phết Tay & Đậu Váng', 'vien-tha-dau', 'Tôm cá phết tay thủ công 98% thịt tươi kèm các loại đậu đông đá hút nước lẩu', 5, 1, 0, NOW(), NOW()),
('Rau Củ, Nấm Quý & Miến', 'rau-nam', 'Nấm quý vùng cao Tây Tạng, rau xanh hữu cơ và miến khoai lang dẻo dai', 6, 1, 0, NOW(), NOW()),
('Khai Vị & Điểm Tâm Dimsum', 'khai-vi', 'Các món ăn đường phố Trùng Khánh giòn cay và điểm tâm xửng hấp ngự thiện', 7, 1, 0, NOW(), NOW()),
('Nước Uống Thanh Nhiệt', 'nuoc-uong', 'Thức uống truyền thống giải cay nồng và làm dịu vị giác đỉnh cao', 8, 1, 0, NOW(), NOW()),
('Tráng Miệng Ngự Thiện', 'trang-mieng', 'Chè tuyết yến dưỡng nhan và thạch băng phấn hoa quả ngọt mát', 9, 1, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE 
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `display_order` = VALUES(`display_order`),
    `is_active` = VALUES(`is_active`),
    `updated_at` = NOW();

-- -----------------------------------------------------------------------------
-- 2. SEED CÁC MÓN ĂN CHUẨN LẨU TRUNG HOA (MENU ITEMS)
-- -----------------------------------------------------------------------------
INSERT INTO `menu_items` (`code`, `name`, `description`, `price`, `unit`, `image_url`, `is_available`, `is_featured`, `is_deleted`, `total_ordered_count`, `created_at`, `updated_at`)
VALUES
-- =========================================================================
-- NHÓM 1: NƯỚC LẨU HOÀNG GIA (nuoc-lau)
-- =========================================================================
('M01', 'Lẩu Cay Tê Trùng Khánh Đại Hồng Bào (Mala Butter Hotpot)', 
 'Vị cay nồng bùng nổ chuẩn nguyên bản Trùng Khánh, nấu từ bơ bò nguyên chất ủ 24h, ớt chỉ thiên Tứ Xuyên, thảo quả và hoa tiêu đỏ tê tê đầu lưỡi.', 
 389000.00, 'Nồi 2 - 4 ngăn', NULL, 1, 1, 0, 340, NOW(), NOW()),

('M02', 'Lẩu Cà Chua Tân Cương Đậm Vị (Xinjiang Tomato Broth)', 
 'Cốt lẩu nấu từ cà chua chín mọng vùng Tân Cương kết hợp nước hầm gà thanh ngọt tự nhiên, vị chua thanh dịu nhẹ kích thích vị giác tối đa.', 
 299000.00, 'Nồi 2 - 4 ngăn', NULL, 1, 1, 0, 280, NOW(), NOW()),

('M03', 'Lẩu Nấm Tùng Nhung & Thảo Mộc Vân Nam (Wild Mushroom Soup)', 
 'Hầm từ 8 loại nấm quý núi cao Vân Nam cùng sâm ngọc linh, kỷ tử và đông trùng hạ thảo, vị ngọt thanh tao bồi bổ nguyên khí.', 
 320000.00, 'Nồi 2 - 4 ngăn', NULL, 1, 0, 0, 195, NOW(), NOW()),

('M04', 'Lẩu Xương Ống Hoàng Triều Collagen (Pork Bone Collagen Broth)', 
 'Nước hầm xương ống heo và tủy bò trong 18 giờ liên tục sánh đặc trắng sữa, giàu collagen tự nhiên, ngọt béo thơm lừng hương thảo mộc.', 
 350000.00, 'Nồi 2 - 4 ngăn', NULL, 1, 0, 0, 160, NOW(), NOW()),

('M05', 'Lẩu Bao Tử Heo Hầm Tiêu Xanh Triều Châu (Pork Tripe & Pepper Soup)', 
 'Bao tử heo tươi làm sạch giòn sần sật hầm cùng tiêu sọ Phú Quốc và gừng già, vị cay ấm nồng nàn giúp giữ ấm cơ thể và khai vị hoàn hảo.', 
 365000.00, 'Nồi 2 - 4 ngăn', NULL, 1, 0, 0, 125, NOW(), NOW()),

('M06', 'Lẩu Dưỡng Nhan Hoa Cúc Kỷ Tử (Imperial Beauty Herbal Broth)', 
 'Nước dùng trong vắt thanh mát từ hoa cúc kim tiền, hạt sen, táo đỏ Tân Cương và củ mài, tạo cảm giác êm dịu thanh lọc cơ thể.', 
 290000.00, 'Nồi 2 - 4 ngăn', NULL, 1, 0, 0, 95, NOW(), NOW()),

-- =========================================================================
-- NHÓM 2: BÒ THƯỢNG HẠNG & WAGYU (bo-wagyu)
-- =========================================================================
('M07', 'Bò Wagyu A5 Cánh Sen Hoàng Triều (Royal Wagyu A5 Rose)', 
 'Từng thớ thịt bò Wagyu A5 vân mỡ cẩm thạch hoàn hảo xếp hình đóa sen hoàng kim, nhúng lướt 8 giây mềm tan ngay trên đầu lưỡi.', 
 890000.00, 'Khay 250g', NULL, 1, 1, 0, 210, NOW(), NOW()),

('M08', 'Đĩa Ba Chỉ Bò Mỹ Hoa Sơn (Prime Angus Short Plate)', 
 'Ba chỉ bò Black Angus tỷ lệ nạc mỡ đan xen đều đặn 7:3, vị béo ngậy ngọt thịt tự nhiên, ăn kèm sốt chấm mè rang cay nồng.', 
 220000.00, 'Khay 300g', NULL, 1, 1, 0, 480, NOW(), NOW()),

('M09', 'Gầu Bò Úc Hoa Mai Thượng Hạng (Australian Beef Brisket)', 
 'Thịt gầu giòn sần sật xen viền mỡ hoa mai giòn rụm béo thơm, không ngấy, giữ độ giòn tuyệt đối khi nhúng trong nước lẩu sôi.', 
 260000.00, 'Khay 250g', NULL, 1, 0, 0, 310, NOW(), NOW()),

('M10', 'Bắp Bò Úc Tẩm Hoa Tiêu Tê Cay (Sichuan Spicy Marinated Beef)', 
 'Bắp bò hoa thái lát mỏng ướp đẫm dầu ớt Tứ Xuyên, bột hoa tiêu và tương đậu bản Tứ Xuyên (Pixian Doubanjiang), nhúng lẩu càng thêm dậy vị.', 
 285000.00, 'Đĩa 250g', NULL, 1, 1, 0, 265, NOW(), NOW()),

('M11', 'Lưỡi Bò Mỹ Cắt Lát Mỏng Tinh Tuyết (Thin Sliced Beef Tongue)', 
 'Lưỡi bò tuyển chọn phần cuống lưỡi mềm nhất cắt lát mỏng tang, nhúng 15 giây cho độ giòn ngọt sần sật độc đáo.', 
 295000.00, 'Khay 200g', NULL, 1, 0, 0, 145, NOW(), NOW()),

('M12', 'Thăn Cừu Mông Cổ Cuộn Tròn Hoàng Gia (Mongolian Rolled Lamb)', 
 'Thịt cừu thảo nguyên Mông Cổ thái cuộn tròn mỏng nhẹ, không tanh mùi hoi, nhúng lẩu cay Tứ Xuyên tạo nên hương vị kinh điển phương Bắc.', 
 275000.00, 'Khay 250g', NULL, 1, 0, 0, 180, NOW(), NOW()),

('M13', 'Bò Mềm Ướp Trứng Gà Cung Đình (Tender Beef with Egg Yolk)', 
 'Thịt thăn bò tươi ướp gia vị bí truyền, ở giữa đập một lòng đỏ trứng gà tươi; khuấy đều trước khi nhúng giúp thịt mềm mịn mọng nước.', 
 245000.00, 'Đĩa 250g', NULL, 1, 1, 0, 230, NOW(), NOW()),

-- =========================================================================
-- NHÓM 3: ĐẶC SẢN NHÚNG & NỘI TẠNG TỨ XUYÊN (dac-san-nhung)
-- =========================================================================
('M14', 'Khăn Lông Bò Đen Tươi (Mao Đỗ Trùng Khánh - Fresh Black Tripe)', 
 'Đặc sản số 1 của lẩu Trùng Khánh! Nhúng theo công thức bí truyền "7 lên 8 xuống" đúng 15 giây để đạt độ giòn sần sật mọng nước đỉnh cao.', 
 215000.00, 'Đĩa 200g', NULL, 1, 1, 0, 520, NOW(), NOW()),

('M15', 'Ruột Vịt Tươi Giòn Thượng Hạng (Crispy Fresh Duck Intestines)', 
 'Ruột vịt tươi tuyển chọn giữ nguyên độ tươi mới bóng mượt, nhúng lẩu 10 giây cuộn lại giòn giòn sần sật quyện nước lẩu mala cay xé.', 
 185000.00, 'Đĩa 200g', NULL, 1, 1, 0, 410, NOW(), NOW()),

('M16', 'Cuống Họng Heo Giòn Hoa Mai (Hoàng Hầu - Pork Aorta)', 
 'Hoàng Hầu giòn sần sật ngâm nước đá tuyết, cắt khảm hình hoa mai, thấm đượm gia vị lẩu cay nồng.', 
 175000.00, 'Đĩa 200g', NULL, 1, 0, 0, 280, NOW(), NOW()),

('M17', 'Tiết Vịt Tươi Thảo Mộc (Silky Duck Blood Curd)', 
 'Tiết vịt tươi luộc chậm với thảo mộc, kết cấu mềm mượt núng nính như thạch pudding, hút trọn vị cay của lẩu mà không hề có mùi tanh.', 
 85000.00, 'Đĩa 250g', NULL, 1, 0, 0, 320, NOW(), NOW()),

('M18', 'Tủy Bò Thượng Hạng Nhúng Cay (Premium Beef Bone Marrow)', 
 'Tủy bò tươi ngậy béo bùi, nhúng chín tới trong nồi lẩu cay tạo cảm giác mềm tan ngất ngây.', 
 195000.00, 'Đĩa 200g', NULL, 1, 0, 0, 110, NOW(), NOW()),

('M19', 'Cật Heo Cắt Hoa Khảm (Carved Pork Kidney Slices)', 
 'Cật heo tươi khử mùi tỉ mỉ bằng rượu nếp và gừng cay, cắt tỉa hoa tinh xảo bung nở bung cánh khi nhúng chín.', 
 165000.00, 'Đĩa 200g', NULL, 1, 0, 0, 130, NOW(), NOW()),

('M20', 'Gân Bò Hầm Hoa Tiêu Nhuận Sắc (Tender Stewed Beef Tendon)', 
 'Gân bò đã được hầm sơ trong thảo mộc cho mềm dẻo, thả vào nồi lẩu sôi tiếp tục ngấm gia vị dẻo quánh thơm ngon.', 
 190000.00, 'Đĩa 220g', NULL, 1, 0, 0, 215, NOW(), NOW()),

-- =========================================================================
-- NHÓM 4: HẢI SẢN TƯƠI SỐNG (hai-san)
-- =========================================================================
('M21', 'Thuyền Tôm Càng Hoàng Đế & Đuôi Tôm Hùm (Imperial Seafood Boat)', 
 'Tôm hùm Canada cùng tôm càng xanh tươi rói phục vụ trên thuyền băng nghệ thuật tráng lệ, thịt chắc giòn ngọt đậm đà.', 
 1450000.00, 'Thuyền lớn', NULL, 1, 1, 0, 65, NOW(), NOW()),

('M22', 'Bạch Tuộc Đại Dương Sống Baby (Fresh Baby Octopus)', 
 'Bạch tuộc tươi sống nguyên con nhúng lẩu giòn ngọt sần sật, chấm kèm sốt tương ớt mè rang cay thơm.', 
 195000.00, 'Đĩa 200g', NULL, 1, 1, 0, 290, NOW(), NOW()),

('M23', 'Mực Trứng Phú Quốc Tươi Căng (Squid Stuffed with Roe)', 
 'Mực trứng loại 1 ôm đầy nang trứng béo bùi, vị ngọt giòn tự nhiên vùng biển đảo phương Nam.', 
 285000.00, 'Đĩa 300g', NULL, 1, 0, 0, 175, NOW(), NOW()),

('M24', 'Phi Lê Cá Tra Sốt Hoa Tiêu (Sichuan Pepper Fish Fillet)', 
 'Thịt cá phi lê trắng ngần tẩm ướp tiêu hoa và lòng trắng trứng, nhúng chín tới mềm mịn tan trong miệng không lo xương.', 
 180000.00, 'Đĩa 250g', NULL, 1, 0, 0, 240, NOW(), NOW()),

('M25', 'Sò Điệp Nhật Cắt Lát Khổng Lồ (Hokkaido Scallop Slices)', 
 'Cồi sò điệp Hokkaido to tròn dày thịt, ngọt lịm mọng nước, nhúng lẩu 30 giây để cảm nhận trọn vẹn vị ngon thượng lưu.', 
 390000.00, 'Đĩa 150g', NULL, 1, 0, 0, 85, NOW(), NOW()),

('M26', 'Râu Mực Khổng Lồ Sốt Sa Tế Cay (Giant Squid Tentacles)', 
 'Râu mực đại dương giòn dai sần sật ướp sốt sa tế thơm lừng cay nồng, ăn cực đã miệng.', 
 210000.00, 'Đĩa 250g', NULL, 1, 0, 0, 195, NOW(), NOW()),

-- =========================================================================
-- NHÓM 5: VIÊN PHẾT TAY & ĐẬU VÁNG CUNG ĐÌNH (vien-tha-dau)
-- =========================================================================
('M27', 'Tôm Tươi Phết Ống Trúc Cung Đình (Handmade Shrimp Paste with Roe)', 
 'Làm từ 98% tôm sú biển đập nhuyễn giữ thớ giòn phủ ngập trứng tôm tobiko, nhân viên dùng muỗng tre gạt từng viên tròn vào nồi lẩu.', 
 210000.00, 'Ống tre 180g', NULL, 1, 1, 0, 540, NOW(), NOW()),

('M28', 'Bò Viên Tê Cay Nhân Phô Mai Chảy (Spicy Cheese Beef Meatball)', 
 'Thịt bò quết tay đập dẻo dai bên trong ẩn chứa lõi phô mai mozzarella béo ngậy tan chảy khi cắn vỡ trong miệng.', 
 165000.00, 'Đĩa 6 viên', NULL, 1, 1, 0, 390, NOW(), NOW()),

('M29', 'Mực Viên Giòn Phết Trứng Cá Chuồn (Handmade Squid Ball with Caviar)', 
 'Mực tươi giã tay dẻo quánh trộn lẫn trứng cá giòn lộp bộp, viên tròn nhúng lẩu chín phồng thơm nức.', 
 175000.00, 'Đĩa 6 viên', NULL, 1, 0, 0, 220, NOW(), NOW()),

('M30', 'Phù Trúc Cuộn Chiên Giòn (Ring Roll - Fried Bean Curd Rolls)', 
 'Váng đậu tiến vua chiên giòn cuộn tròn, chỉ cần nhúng lướt 3 giây trong nước lẩu sôi là ngậm no nước súp thơm béo ngất ngây.', 
 85000.00, 'Khay 6 cuộn', NULL, 1, 1, 0, 680, NOW(), NOW()),

('M31', 'Đậu Hũ Đông Đá Trùng Khánh (Frozen Sponge Tofu)', 
 'Đậu hũ non được cấp đông theo bí quyết Tứ Xuyên tạo thành cấu trúc xốp tổ ong đặc biệt, hút trọn nước lẩu cay nồng vào từng ngóc ngách.', 
 65000.00, 'Khay 250g', NULL, 1, 1, 0, 360, NOW(), NOW()),

('M32', 'Đậu Hũ Phô Mai Hoàng Gia (Cheese Fish Tofu)', 
 'Chả cá thượng hạng mềm mịn kết hợp nhân phô mai cheddar thơm béo đậm đà, món khoái khẩu của cả người lớn và trẻ em.', 
 95000.00, 'Đĩa 6 viên', NULL, 1, 0, 0, 420, NOW(), NOW()),

('M33', 'Da Đậu Hũ Tươi Ngàn Lớp (Fresh Tofu Skin Sheets)', 
 'Từng tấm phù trúc tươi mềm mại óng ả, thơm ngát mùi đậu tương non nguyên chất khi thả vào nồi nước dùng.', 
 75000.00, 'Khay 200g', NULL, 1, 0, 0, 190, NOW(), NOW()),

-- =========================================================================
-- NHÓM 6: RAU CỦ, NẤM QUÝ & MIẾN (rau-nam)
-- =========================================================================
('M34', 'Combo Nấm Cung Đình & Rau Hữu Cơ (Royal Veg & Mushroom Platter)', 
 'Tuyển tập nấm đông cô tươi, nấm kim châm, nấm đùi gà, ngô ngọt Đà Lạt, cải thìa hoàng cung và rau bina hữu cơ mát lành.', 
 125000.00, 'Khay lớn', NULL, 1, 1, 0, 490, NOW(), NOW()),

('M35', 'Nấm Tùng Nhung Núi Cao Vân Nam (Matsutake Mushroom)', 
 'Nấm tùng nhung quý hiếm hương thơm tinh tế đặc trưng, nhúng lẩu thanh ngọt bồi bổ thể lực và dưỡng da.', 
 185000.00, 'Khay 150g', NULL, 1, 0, 0, 115, NOW(), NOW()),

('M36', 'Nấm Kim Châm Hoàng Kim (Golden Needle Mushroom)', 
 'Nấm kim châm tươi trắng ngần sợi giòn sần sật, hòa quyện tuyệt hảo cùng vị cay nồng của nước lẩu bơ bò.', 
 45000.00, 'Khay 200g', NULL, 1, 0, 0, 430, NOW(), NOW()),

('M37', 'Củ Sen Cắt Lát Giòn Ngọt Hồ Nam (Hunan Lotus Root Slices)', 
 'Củ sen tươi hồ Nam thái lát mỏng vừa phải, giữ độ giòn sần sật ngọt bùi tự nhiên giúp thanh nhiệt giải ngấy.', 
 55000.00, 'Đĩa 200g', NULL, 1, 0, 0, 270, NOW(), NOW()),

('M38', 'Măng Trúc Tứ Xuyên Muối Tươi (Sichuan Tender Bamboo Shoots)', 
 'Măng trúc non tươi mọc đầu mùa trên núi cao, giòn giòn sần sật thấm đẫm ớt hoa tiêu.', 
 65000.00, 'Đĩa 200g', NULL, 1, 0, 0, 190, NOW(), NOW()),

('M39', 'Miến Khoai Lang Trùng Khánh Bản To (Chongqing Sweet Potato Noodles)', 
 'Sợi miến to dẹt dẻo dai làm từ tinh bột khoai lang nguyên chất, nhúng trong nước lẩu cay chuyển màu bóng bẩy, dai giòn không hề nát.', 
 60000.00, 'Đĩa 200g', NULL, 1, 1, 0, 580, NOW(), NOW()),

('M40', 'Mì Tươi Trứng Gà Kéo Tay Thượng Hạng (Hand-pulled Egg Noodles)', 
 'Mì tươi nhào bột kéo sợi thủ công trong ngày, sợi mì vàng ươm dai mịn, kết thúc bữa tiệc lẩu no nê trọn vẹn.', 
 45000.00, 'Đĩa 2 vắt', NULL, 1, 0, 0, 350, NOW(), NOW()),

-- =========================================================================
-- NHÓM 7: KHAI VỊ & ĐIỂM TÂM DIMSUM (khai-vi)
-- =========================================================================
('M41', 'Thịt Heo Chiên Giòn Tê Cay Tiểu Tô Nhục (Sichuan Crispy Pork Bites)', 
 'Món khai vị quốc hồn quốc túy của các quán lẩu Tứ Xuyên! Thịt ba chỉ ướp hoa tiêu chiên vàng giòn rụm bên ngoài, mọng nước bên trong, chấm bột ớt cay.', 
 145000.00, 'Đĩa 250g', NULL, 1, 1, 0, 610, NOW(), NOW()),

('M42', 'Sủi Cảo Tôm Tươi Bọc Vàng Cung Đình (Imperial Golden Shrimp Dumplings)', 
 'Nhân tôm sú tự nhiên giòn ngọt bọc lá hoành thánh mỏng dát ánh vàng ngọc hoàng gia, phục vụ cùng dầu ớt giấm đen Triều Châu.', 
 155000.00, 'Xửng 6 viên', NULL, 1, 1, 0, 290, NOW(), NOW()),

('M43', 'Màn Thầu Chiên Hoàng Kim Kèm Sữa Đặc (Golden Fried Mantou with Milk)', 
 'Bánh màn thầu chiên ngoài giòn rụm vàng ruộm ruột mềm xốp thơm bơ, chấm đẫm sữa đặc ngọt béo làm dịu ngay vị cay sau khi ăn lẩu.', 
 75000.00, 'Đĩa 6 bánh', NULL, 1, 1, 0, 670, NOW(), NOW()),

('M44', 'Há Cảo Tôm Thủy Tinh Ngự Thiện (Crystal Har Gow Dumplings)', 
 'Vỏ bánh trong suốt thấy rõ màu tôm hồng hào bên trong, hấp nóng hổi nghi ngút khói chuẩn phong vị trà chiều Quảng Đông.', 
 95000.00, 'Xửng 4 viên', NULL, 1, 0, 0, 310, NOW(), NOW()),

('M45', 'Chân Gà Rút Xương Sốt Cay Tê Trùng Khánh (Boneless Spicy Chicken Feet)', 
 'Chân gà rút xương giòn sần sật ngâm sốt ớt hoa tiêu, chanh vàng và rau mùi thơm nồng khai vị kích thích tuyến nước bọt.', 
 135000.00, 'Đĩa 200g', NULL, 1, 0, 0, 240, NOW(), NOW()),

('M46', 'Đậu Phộng Chiên Giòn Sốt Muối Tiêu Tứ Xuyên (Sichuan Spiced Peanuts)', 
 'Món nhắm giòn tan cay thơm nhè nhẹ mùi hoa tiêu và ớt khô, nhâm nhi trong lúc đợi nồi lẩu sôi.', 
 39000.00, 'Đĩa nhỏ', NULL, 1, 0, 0, 180, NOW(), NOW()),

-- =========================================================================
-- NHÓM 8: NƯỚC UỐNG THANH NHIỆT (nuoc-uong)
-- =========================================================================
('M47', 'Nước Mận Khói Cung Đình Ướp Lạnh (Suanmeitang Smoked Plum Juice)', 
 'Bảo vật giải cay số 1 Trung Hoa! Nấu thủ công từ mận hun khói, sơn tra, vỏ quýt trần bì và hoa quế ngọt mát, dập tan cảm giác cay rát tức thì.', 
 45000.00, 'Bình 500ml', NULL, 1, 1, 0, 890, NOW(), NOW()),

('M48', 'Trà Sữa Thảo Mộc Hồng Kông Trân Châu Đen (Hong Kong Herbal Milk Tea)', 
 'Vị trà Ceylon đậm đà quyện sữa béo bùi và thảo mộc thanh mát, kèm trân châu đường đen dẻo dai chuẩn vị xứ Cảng Thơm.', 
 55000.00, 'Ly 500ml', NULL, 1, 1, 0, 520, NOW(), NOW()),

('M49', 'Trà Hoa Cúc Kỷ Tử Hoàng Đế Ướp Lạnh (Imperial Chrysanthemum Tea)', 
 'Nấu từ hoa cúc trắng Triều Châu, kỷ tử đỏ Ninh Hạ và đường phèn thanh khiết, làm mát gan và giải nhiệt cơ thể cực tốt.', 
 40000.00, 'Bình 500ml', NULL, 1, 0, 0, 340, NOW(), NOW()),

('M50', 'Trà Bí Đao Hạt Chia Sương Sáo Cung Đình (Winter Melon Chia Tea)', 
 'Trà bí đao nấu lá dứa thơm mát kết hợp hạt chia hữu cơ và thạch sương sáo mềm mượt, vị ngọt dịu thanh tao.', 
 45000.00, 'Ly 500ml', NULL, 1, 0, 0, 290, NOW(), NOW()),

('M51', 'Nước Chanh Vàng Hoa Tiêu Lắc Lạnh (Sichuan Pepper Lemonade)', 
 'Sự kết hợp bùng nổ giữa nước cốt chanh vàng tươi mát và một chút hoa tiêu thơm tê đầu lưỡi, trải nghiệm hương vị vô cùng mới lạ.', 
 49000.00, 'Ly 500ml', NULL, 1, 0, 0, 160, NOW(), NOW()),

-- =========================================================================
-- NHÓM 9: TRÁNG MIỆNG NGỰ THIỆN (trang-mieng)
-- =========================================================================
('M52', 'Thạch Băng Phấn Tứ Xuyên Đường Nâu (Sichuan Bingfen Jelly)', 
 'Món tráng miệng lẩu kinh điển bậc nhất! Thạch trong suốt mát rượi rưới mật mía đường nâu, rắc nho khô, mè rang, sơn tra lát và dưa hấu mát lạnh.', 
 55000.00, 'Bát sứ', NULL, 1, 1, 0, 720, NOW(), NOW()),

('M53', 'Chè Trầm Hương Tuyết Yến Ngự Thiện (Imperial Bird Nest Herbal Dessert)', 
 'Chè dưỡng nhan tinh túy nấu từ tuyết yến tự nhiên, bồ đào, long nhãn Hưng Yên, hạt sen và nấm tuyết, ngọt thanh mát mượt bổ dưỡng.', 
 65000.00, 'Thố sứ', NULL, 1, 1, 0, 380, NOW(), NOW()),

('M54', 'Bánh Nếp Dẻo Nhân Đậu Đỏ Đường Nâu (Brown Sugar Red Bean Mochi)', 
 'Bánh nếp chiên phồng phủ bột đậu nành rang thơm nức, bên trong nhân đậu đỏ nhuyễn mịn ngọt dịu rưới sốt đường nâu ấm nóng.', 
 59000.00, 'Đĩa 4 bánh', NULL, 1, 0, 0, 260, NOW(), NOW()),

('M55', 'Kem Tuyết Khói Trái Cây Nhiệt Đới (Imperial Smoky Ice Cream)', 
 'Viên kem mát lạnh đựng trong thố gốm bốc khói mây huyền ảo nhờ đá khô sương mù, ngọt bùi giải nhiệt mỹ mãn cho bàn tiệc lẩu.', 
 49000.00, 'Ly khói sương', NULL, 1, 0, 0, 410, NOW(), NOW())

ON DUPLICATE KEY UPDATE 
    `name` = VALUES(`name`),
    `description` = VALUES(`description`),
    `price` = VALUES(`price`),
    `unit` = VALUES(`unit`),
    `is_available` = VALUES(`is_available`),
    `is_featured` = VALUES(`is_featured`),
    `is_deleted` = VALUES(`is_deleted`),
    `total_ordered_count` = VALUES(`total_ordered_count`),
    `updated_at` = NOW();

-- -----------------------------------------------------------------------------
-- 3. LIÊN KẾT MÓN ĂN VÀ DANH MỤC (MENU_ITEM_CATEGORIES)
-- Sử dụng Subquery để tự động ánh xạ chính xác theo mã món và slug danh mục
-- -----------------------------------------------------------------------------

-- Xóa liên kết cũ của các mã món này để đồng bộ sạch sẽ
DELETE mic FROM `menu_item_categories` mic
JOIN `menu_items` mi ON mic.menu_item_id = mi.id
WHERE mi.code IN (
    'M01', 'M02', 'M03', 'M04', 'M05', 'M06',
    'M07', 'M08', 'M09', 'M10', 'M11', 'M12', 'M13',
    'M14', 'M15', 'M16', 'M17', 'M18', 'M19', 'M20',
    'M21', 'M22', 'M23', 'M24', 'M25', 'M26',
    'M27', 'M28', 'M29', 'M30', 'M31', 'M32', 'M33',
    'M34', 'M35', 'M36', 'M37', 'M38', 'M39', 'M40',
    'M41', 'M42', 'M43', 'M44', 'M45', 'M46',
    'M47', 'M48', 'M49', 'M50', 'M51',
    'M52', 'M53', 'M54', 'M55'
);

-- Nước lẩu (M01 -> M06) -> nuoc-lau
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M01', 'M02', 'M03', 'M04', 'M05', 'M06') AND cat.slug = 'nuoc-lau';

-- Thịt bò & thịt nhúng (M07 -> M13) -> bo-wagyu
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M07', 'M08', 'M09', 'M10', 'M11', 'M12', 'M13') AND cat.slug = 'bo-wagyu';

-- Đặc sản nhúng nội tạng (M14 -> M20) -> dac-san-nhung
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M14', 'M15', 'M16', 'M17', 'M18', 'M19', 'M20') AND cat.slug = 'dac-san-nhung';

-- Hải sản tươi sống (M21 -> M26) -> hai-san
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M21', 'M22', 'M23', 'M24', 'M25', 'M26') AND cat.slug = 'hai-san';

-- Viên thả & đậu váng (M27 -> M33) -> vien-tha-dau
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M27', 'M28', 'M29', 'M30', 'M31', 'M32', 'M33') AND cat.slug = 'vien-tha-dau';

-- Rau nấm & miến (M34 -> M40) -> rau-nam
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M34', 'M35', 'M36', 'M37', 'M38', 'M39', 'M40') AND cat.slug = 'rau-nam';

-- Khai vị & Dimsum (M41 -> M46) -> khai-vi
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M41', 'M42', 'M43', 'M44', 'M45', 'M46') AND cat.slug = 'khai-vi';

-- Nước uống (M47 -> M51) -> nuoc-uong
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M47', 'M48', 'M49', 'M50', 'M51') AND cat.slug = 'nuoc-uong';

-- Tráng miệng (M52 -> M55) -> trang-mieng
INSERT INTO `menu_item_categories` (`menu_item_id`, `category_id`)
SELECT mi.id, cat.id FROM `menu_items` mi, `categories` cat
WHERE mi.code IN ('M52', 'M53', 'M54', 'M55') AND cat.slug = 'trang-mieng';

SET FOREIGN_KEY_CHECKS = 1;

-- =============================================================================
-- HOÀN TẤT SEED DỮ LIỆU THỰC ĐƠN LẨU TRUNG HOA CHO HỎA DIỆM CÁC!
-- =============================================================================
