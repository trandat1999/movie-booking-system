package com.moviebooking.infrastructure.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Tạo ảnh QR dạng data URI PNG để nhúng trực tiếp vào trang vé và email (BKG11). */
@Component
public class QrCodeGenerator {

    private static final int SIZE = 240;

    public String toDataUri(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter()
                    .encode(
                            content,
                            BarcodeFormat.QR_CODE,
                            SIZE,
                            SIZE,
                            Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 1));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (WriterException | IOException ex) {
            throw new IllegalStateException("Cannot generate QR code", ex);
        }
    }
}
