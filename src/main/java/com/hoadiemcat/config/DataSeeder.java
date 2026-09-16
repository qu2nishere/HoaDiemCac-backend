package com.hoadiemcat.config;

import com.hoadiemcat.entity.Category;
import com.hoadiemcat.entity.MenuItem;
import com.hoadiemcat.entity.User;
import com.hoadiemcat.entity.enums.Role;
import com.hoadiemcat.entity.enums.Status;
import com.hoadiemcat.repository.CategoryRepository;
import com.hoadiemcat.repository.MenuItemRepository;
import com.hoadiemcat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final com.hoadiemcat.repository.RestaurantTableRepository restaurantTableRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedAdminUser();
        Map<String, Category> categoryMap = seedCategories();
        seedMenuItems(categoryMap);
        seedTables();
    }

    private void seedAdminUser() {
        String adminEmail = "kqtthings@gmail.com";
        Optional<User> existingUser = userRepository.findByEmail(adminEmail);

        if (existingUser.isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode("Abc@1234"))
                    .fullName("Quản Trị Viên")
                    .role(Role.ADMIN)
                    .status(Status.ACTIVE)
                    .build();
            userRepository.save(admin);
            log.info("✅ TẠO TÀI KHOẢN ADMIN THÀNH CÔNG: {}", adminEmail);
        } else {
            User admin = existingUser.get();
            admin.setPassword(passwordEncoder.encode("Abc@1234"));
            userRepository.save(admin);
            log.info("⚡ Tài khoản Admin ({}) đã tồn tại. Đã reset lại mật khẩu: Abc@1234", adminEmail);
        }
    }

    private Map<String, Category> seedCategories() {
        Map<String, Category> categoryMap = new HashMap<>();

        List<Category> definedCategories = List.of(
                Category.builder().name("Nước Lẩu Hoàng Gia").slug("nuoc-lau").description("Các loại nước lẩu tinh hoa cung đình Hỏa Diệm Các").displayOrder(1).isActive(true).isSystem(false).build(),
                Category.builder().name("Bò Thượng Hạng & Wagyu").slug("bo-wagyu").description("Thịt bò Wagyu và Black Angus nhập khẩu cao cấp").displayOrder(2).isActive(true).isSystem(false).build(),
                Category.builder().name("Hải Sản Tươi Sống").slug("hai-san").description("Hải sản tự nhiên tươi sống hảo hạng").displayOrder(3).isActive(true).isSystem(false).build(),
                Category.builder().name("Đồ Nhúng Cung Đình").slug("do-nhung").description("Các món nhúng bổ dưỡng, nấm quý và rau hữu cơ").displayOrder(4).isActive(true).isSystem(false).build(),
                Category.builder().name("Khai Vị & Dimsum").slug("khai-vi").description("Món ăn khai vị, điểm tâm chuẩn phong vị cung đình").displayOrder(5).isActive(true).isSystem(false).build(),
                Category.builder().name("Nước Uống & Tráng Miệng").slug("nuoc-uong").description("Thức uống thanh mát và tráng miệng ngự thiện").displayOrder(6).isActive(true).isSystem(false).build()
        );

        for (Category cat : definedCategories) {
            Optional<Category> opt = categoryRepository.findBySlug(cat.getSlug());
            if (opt.isEmpty()) {
                Category saved = categoryRepository.save(cat);
                categoryMap.put(saved.getSlug(), saved);
            } else {
                categoryMap.put(opt.get().getSlug(), opt.get());
            }
        }

        log.info("✅ Đồng bộ {} nhóm danh mục món ăn trong Database", categoryMap.size());
        return categoryMap;
    }

    private void seedMenuItems(Map<String, Category> categoryMap) {
        if (menuItemRepository.count() > 0) {
            log.info("⚡ Đã có {} món ăn trong Database, bỏ qua bước khởi tạo mẫu.", menuItemRepository.count());
            return;
        }

        List<DishSeed> dishSeeds = List.of(
                new DishSeed("M01", "Lẩu 9 Ngăn Trùng Khánh Đại Hồng Bào", "nuoc-lau", "Vị cay nồng chuẩn Tứ Xuyên với ớt mắt chim, hoa tiêu đỏ và bơ bò hoàng gia ủ 24 giờ", new BigDecimal("389000"), "Nồi 9 ngăn", "https://lh3.googleusercontent.com/aida/AEtjO1UKQpR933zOehynzk2JjexNJy6d-RDcS3v_fIkhA0Jqf0QI6PQQMHNfpQSG7FVUHYJ-rNV4UJbAxtcmWVk6zHPeiliNhvCLnfB_KWPgklYweRIIYGJ7sh4WKoxkUKJjoX-509Qq6uDTwD395e20govXlp2m4ImjSWCLxyUnGjlIDpD_pMTIKXu5WY3q4s8T7fCkGtOC-EgmHEpbuGY3mVwcqhn2HOtSuIvBim3_YuWII8VfRyxBAJeILw", true, true, 120L),
                new DishSeed("M02", "Bò Wagyu A5 Cánh Sen Hoàng Triều", "bo-wagyu", "Từng thớ thịt cẩm thạch xếp tạo hình đóa hồng sen vương giả, nhúng chín tới 8 giây", new BigDecimal("890000"), "Khay 250g", "https://lh3.googleusercontent.com/aida/AEtjO1VBM5kYipjZ7X94_jNEpqj8Rx-d4BVzww-E4mks2gh7U7iAyjbbESZ7NnOGFpmO8CuZIbWlABGyVkWQnqvg9qHe73RzqONNjSHYHHqxQyZE1j421TSi5wtktfcUJMSqU3G5rJoxDKT21KJqsjiHZtSBwDiy8TVbaXP7qZsGN7kNU3rQVDhXKWe_AgWWnY39Tf8FSx5suDIfON4xJdXrjGzacBsSOuNQhNUNHP8uzcpRHHpQiCE3xkZjO3I", true, true, 85L),
                new DishSeed("M03", "Đĩa Ba Chỉ Bò Mỹ Hoa Sơn", "bo-wagyu", "Ba chỉ bò Black Angus thượng hạng với tỷ lệ nạc mỡ đan xen hoàn mỹ, béo ngậy mềm tan", new BigDecimal("220000"), "Khay 300g", "https://lh3.googleusercontent.com/aida/AEtjO1Vm5S8evpHG7KMwx_Gz0862BnaL4gso__MqfZXzi8p4e5C4kY138nEXA2-Cco23iHRfzw5YzKfaQBNO9C8uhWHMp_TX98WAEOx1iRxuQ4ekppIZZxdCJJafx-9heW4Tp38rUoR17o-m3MAeXDd8K9_XSb7zESgHJev9GUcH1Sw744Lym2XfVVUPT6KTb5IVQsEzy7HDnvtM5XJhaWzKcRux3-l7RsE_l92ggOAT17loUM7KQFDjB4d40rw", true, false, 95L),
                new DishSeed("M04", "Thuyền Tôm Càng Hoàng Đế & Đuôi Tôm Hùm", "hai-san", "Tôm hùm Alaska cùng tôm càng xanh tươi rói phục vụ trên thuyền băng tuyết nghệ thuật", new BigDecimal("1450000"), "Thuyền lớn", "https://lh3.googleusercontent.com/aida/AEtjO1WXyHziaqp7H3c1LptSTxTE9gCwr3JHagzZpWIMBjpNQCRmEMXNLor3osYND7_R9mI7uSxqYJLbXIk-4DbwEYzg5vgV95SLLNGYrnLdWf2qwCA284MTKV83qbKS6g3blVvvGI4QSY5XYA4iUr7gUd4xANCgiIgyvwBRVYQUvZasSovo8Xc0PZIsUdOakuTFvSXCwRV95FmbguGvKl-57dKVuMBJ-10HWSMATOeV-D2w44D1rY4ZGd0OgXo", false, true, 42L),
                new DishSeed("M05", "Lẩu Nấm Tùng Nhung Bổ Dưỡng", "nuoc-lau", "Nấu từ 8 loại nấm quý Tây Tạng và sâm ngọc linh thanh ngọt bồi bổ khí huyết", new BigDecimal("320000"), "Nồi 1 - 2 ngăn", "https://images.unsplash.com/photo-1547928576-a4a33237cbc3?auto=format&fit=crop&w=400&q=80", true, false, 64L),
                new DishSeed("M06", "Sủi Cảo Tôm Tươi Bọc Vàng Cung Đình", "khai-vi", "Nhân tôm sú tự nhiên giòn ngọt bọc lá hoành thánh mỏng mịn dát ánh vàng ngọc hoàng gia", new BigDecimal("1450000"), "Xửng 6 viên", "https://images.unsplash.com/photo-1496116218417-1a781b1c416c?auto=format&fit=crop&w=400&q=80", true, false, 78L),
                new DishSeed("M07", "Gầu Bò Úc Hoa Mai Thượng Hạng", "bo-wagyu", "Thịt gầu giòn sần sật xen viền mỡ hoa mai béo thơm nức mũi khi nhúng chín", new BigDecimal("260000"), "Khay 250g", "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=400&q=80", true, false, 55L),
                new DishSeed("M08", "Bạch Tuộc Đại Dương Sống Baby", "hai-san", "Bạch tuộc tươi sống nguyên con nhúng lẩu giòn ngọt sần sật chấm cùng sốt mè cay", new BigDecimal("195000"), "Đĩa 200g", "https://images.unsplash.com/photo-1565557623262-b51c2513a641?auto=format&fit=crop&w=400&q=80", true, false, 71L),
                new DishSeed("M09", "Combo Nấm Cung Đình & Rau Hữu Cơ", "do-nhung", "Gồm nấm đông cô, nấm kim châm, nấm đùi gà, cải thìa, ngô ngọt hữu cơ Đà Lạt", new BigDecimal("125000"), "Khay lớn", "https://images.unsplash.com/photo-1540420773420-3366772f4999?auto=format&fit=crop&w=400&q=80", true, false, 110L),
                new DishSeed("M10", "Nước Mận Khói Cung Đình Ướp Lạnh", "nuoc-uong", "Giải ngấy cay nồng hoàn hảo, nấu thủ công từ mận hun khói và sơn tra thảo mộc gia truyền", new BigDecimal("45000"), "Bình 500ml", "https://images.unsplash.com/photo-1513558161293-cdaf765ed2fd?auto=format&fit=crop&w=400&q=80", true, true, 230L),
                new DishSeed("M11", "Chè Trầm Hương Tuyết Yến Ngự Thiện", "nuoc-uong", "Món tráng miệng thanh mát với tuyết yến tự nhiên, bồ đào, kỷ tử đỏ và long nhãn Hưng Yên", new BigDecimal("65000"), "Thố sứ", "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=400&q=80", true, false, 88L),
                new DishSeed("M12", "Mực Trứng Phú Quốc Tươi Căng", "hai-san", "Mực trứng chọn lọc ôm đầy nang trứng béo bùi, vị giòn dai ngọt đậm đà vùng biển Tây", new BigDecimal("285000"), "Đĩa 300g", "https://images.unsplash.com/photo-1559742811-822873691df8?auto=format&fit=crop&w=400&q=80", false, false, 37L)
        );

        for (DishSeed ds : dishSeeds) {
            Category category = categoryMap.get(ds.categorySlug);
            Set<Category> cats = new HashSet<>();
            if (category != null) {
                cats.add(category);
            }

            MenuItem item = MenuItem.builder()
                    .code(ds.code)
                    .name(ds.name)
                    .description(ds.description)
                    .price(ds.price)
                    .unit(ds.unit)
                    .imageUrl(ds.imageUrl)
                    .isAvailable(ds.isAvailable)
                    .isFeatured(ds.isFeatured)
                    .isDeleted(false)
                    .totalOrderedCount(ds.totalOrderedCount)
                    .categories(cats)
                    .build();

            menuItemRepository.save(item);
        }

        log.info("✅ Đã khởi tạo thành công {} món ăn mẫu vào Database", dishSeeds.size());
    }

    private void seedTables() {
        if (restaurantTableRepository.count() > 0) {
            log.info("⚡ Đã có {} bàn ăn trong Database, bỏ qua bước khởi tạo bàn.", restaurantTableRepository.count());
            return;
        }

        Random random = new Random();
        List<com.hoadiemcat.entity.RestaurantTable> tables = new ArrayList<>();

        // 10 bàn khu vực chung (B01 -> B10)
        for (int i = 1; i <= 10; i++) {
            String num = String.format("%02d", i);
            String passcode = String.format("%04d", random.nextInt(9000) + 1000);
            tables.add(com.hoadiemcat.entity.RestaurantTable.builder()
                    .tableNumber("B" + num)
                    .name("BÀN " + num)
                    .area(com.hoadiemcat.entity.enums.TableArea.COMMON)
                    .capacity(4)
                    .maxActiveDevices(6)
                    .status(com.hoadiemcat.entity.enums.TableStatus.AVAILABLE)
                    .currentPasscode(passcode)
                    .activeDeviceCount(0)
                    .isOrderLocked(false)
                    .failedAttempts(0)
                    .build());
        }

        // 10 phòng VIP hoàng gia (VIP11 -> VIP20)
        for (int i = 11; i <= 20; i++) {
            String passcode = String.format("%04d", random.nextInt(9000) + 1000);
            tables.add(com.hoadiemcat.entity.RestaurantTable.builder()
                    .tableNumber("VIP" + i)
                    .name("VIP " + i)
                    .area(com.hoadiemcat.entity.enums.TableArea.VIP)
                    .capacity(10)
                    .maxActiveDevices(15)
                    .status(com.hoadiemcat.entity.enums.TableStatus.AVAILABLE)
                    .currentPasscode(passcode)
                    .activeDeviceCount(0)
                    .isOrderLocked(false)
                    .failedAttempts(0)
                    .build());
        }

        restaurantTableRepository.saveAll(tables);
        log.info("✅ Đã khởi tạo thành công {} bàn ăn sẵn sàng (AVAILABLE, 0 thiết bị) vào Database", tables.size());
    }

    private record DishSeed(
            String code,
            String name,
            String categorySlug,
            String description,
            BigDecimal price,
            String unit,
            String imageUrl,
            Boolean isAvailable,
            Boolean isFeatured,
            Long totalOrderedCount
    ) {}
}
