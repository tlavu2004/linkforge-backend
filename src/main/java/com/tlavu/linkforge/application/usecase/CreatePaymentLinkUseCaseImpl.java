package com.tlavu.linkforge.application.usecase;

import com.tlavu.linkforge.domain.entity.PaymentTransaction;
import com.tlavu.linkforge.domain.repository.PaymentTransactionRepository;
import com.tlavu.linkforge.infrastructure.config.VNPayConfig;
import com.tlavu.linkforge.infrastructure.util.VNPayUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CreatePaymentLinkUseCaseImpl implements CreatePaymentLinkUseCase {

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final VNPayConfig vnPayConfig;

    @Override
    @Transactional
    public String execute(Long userId, String packageCode, String ipAddress) {
        com.tlavu.linkforge.domain.entity.VipPackage vipPackage = com.tlavu.linkforge.domain.entity.VipPackage
                .fromCode(packageCode);
        long amountVal = vipPackage.getPriceVnd(); // Dynamic amount
        String orderCode = "VIP" + io.hypersistence.tsid.TSID.fast().toLong();

        long id = io.hypersistence.tsid.TSID.fast().toLong();
        PaymentTransaction transaction = PaymentTransaction.create(id, userId, orderCode, (int) amountVal,
                vipPackage.getCode());
        paymentTransactionRepository.save(transaction);

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnPayConfig.getVersion());
        vnp_Params.put("vnp_Command", vnPayConfig.getCommand());
        vnp_Params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnp_Params.put("vnp_Amount", String.valueOf(amountVal * 100)); // VND scaled to 100
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", orderCode);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan nang cap VIP LinkForge - " + orderCode);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnp_Params.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        // Sort parameters to create HmacSHA512 checksum string
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (int i = 0; i < fieldNames.size(); i++) {
            String fieldName = fieldNames.get(i);
            String fieldValue = vnp_Params.get(fieldName);
            if (fieldValue != null && fieldValue.length() > 0) {
                // Build hash string (no query encoding needed before Hashing in new VNPay
                // versions? Actually yes, it uses URL Encoder!)
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                // Build final Query String
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));

                if (i < fieldNames.size() - 1) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        String vnp_SecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return vnPayConfig.getPayUrl() + "?" + queryUrl;
    }
}
