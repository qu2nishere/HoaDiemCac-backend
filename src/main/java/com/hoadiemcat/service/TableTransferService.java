package com.hoadiemcat.service;

import com.hoadiemcat.dto.request.TableDirectTransferRequest;
import com.hoadiemcat.dto.request.TableTransferConfirmRequest;
import com.hoadiemcat.dto.request.TableTransferRequest;
import com.hoadiemcat.dto.response.TableTransferConfirmResponse;
import com.hoadiemcat.dto.response.TableTransferResponse;

public interface TableTransferService {

    /**
     * Khách hàng (Chủ Bàn) yêu cầu tạo mã Chuyển hoặc Ghép bàn (TTL 5 phút).
     */
    TableTransferResponse requestTransfer(TableTransferRequest request, String deviceToken, String sessionToken);

    /**
     * Khách hàng xác nhận nhập mã Chuyển hoặc Ghép bàn tại bàn đích.
     */
    TableTransferConfirmResponse confirmTransfer(TableTransferConfirmRequest request, String deviceToken);

    /**
     * Khách hàng hủy yêu cầu chuyển/ghép bàn tại bàn cũ.
     */
    TableTransferResponse cancelTransfer(String transferCode, String deviceToken);

    /**
     * Nhân viên / Quản lý thực hiện chuyển hoặc ghép bàn trực tiếp 1 chạm từ POS.
     */
    TableTransferConfirmResponse directTransfer(TableDirectTransferRequest request, String username);

    /**
     * Quét và hủy các mã chuyển bàn quá hạn 5 phút.
     */
    void expireOutdatedTransfers();
}
