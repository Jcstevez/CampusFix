package com.campusfix.adapter.out.qr;

import com.campusfix.application.port.out.DecodificadorQr;
import com.campusfix.application.port.out.GeneradorQr;
import com.campusfix.domain.error.QrNoLegibleException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/** Adaptador de salida: genera y lee QR con la libreria ZXing. */
public class ZxingAdapter implements GeneradorQr, DecodificadorQr {

    private static final int LADO_PX = 300;

    @Override
    public byte[] generarPng(String contenido) {
        try {
            BitMatrix matriz = new QRCodeWriter().encode(
                    contenido,
                    BarcodeFormat.QR_CODE,
                    LADO_PX,
                    LADO_PX,
                    Map.of(EncodeHintType.CHARACTER_SET, "UTF-8",
                            EncodeHintType.MARGIN, 2));
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }

    @Override
    public String decodificar(byte[] imagen) {
        if (imagen == null || imagen.length == 0) {
            throw new QrNoLegibleException("La imagen esta vacia");
        }
        try {
            BufferedImage bmp = ImageIO.read(new ByteArrayInputStream(imagen));
            if (bmp == null) {
                throw new QrNoLegibleException("El archivo no es una imagen valida");
            }
            BinaryBitmap mapa = new BinaryBitmap(
                    new HybridBinarizer(new BufferedImageLuminanceSource(bmp)));
            return new MultiFormatReader().decode(mapa, Map.of(
                    DecodeHintType.POSSIBLE_FORMATS, List.of(BarcodeFormat.QR_CODE),
                    DecodeHintType.TRY_HARDER, Boolean.TRUE)).getText();
        } catch (NotFoundException e) {
            throw new QrNoLegibleException("No se encontro un QR legible en la imagen");
        } catch (IOException e) {
            throw new QrNoLegibleException("No se pudo leer la imagen");
        }
    }
}
