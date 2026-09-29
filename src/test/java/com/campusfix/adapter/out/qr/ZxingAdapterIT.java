package com.campusfix.adapter.out.qr;

import com.campusfix.adapter.out.catalog.CatalogoObjetosEnMemoria;
import com.campusfix.domain.catalogo.CodigoQr;
import com.campusfix.domain.error.QrNoLegibleException;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reto 3: generar (simular impresion) y leer QR de verdad con ZXing. */
class ZxingAdapterIT {

    private final ZxingAdapter zxing = new ZxingAdapter();

    @Test
    void elQrGeneradoEsUnPngValido() {
        byte[] png = zxing.generarPng("campusfix://objeto/PROY-B302");

        assertTrue(png.length > 100);
        assertEquals((byte) 0x89, png[0]);
        assertEquals((byte) 'P', png[1]);
        assertEquals((byte) 'N', png[2]);
        assertEquals((byte) 'G', png[3]);
    }

    @Test
    void generarYLeerDevuelveElMismoContenido() {
        // Arrange
        String contenido = new CodigoQr("PROY-B302").contenido();

        // Act
        String leido = zxing.decodificar(zxing.generarPng(contenido));

        // Assert
        assertEquals(contenido, leido);
    }

    @Test
    void todosLosObjetosDelCatalogoSobrevivenElViajeGenerarYLeer() {
        for (String id : new String[]{
                "PROY-B302", "PC-C101-01", "WIFI-A2", "PUERTA-A201", "LUZ-A201", "BANO-B1"}) {
            assertTrue(CatalogoObjetosEnMemoria.conDatosDeEjemplo().buscarPorId(id).isPresent());
            String contenido = new CodigoQr(id).contenido();
            assertEquals(contenido, zxing.decodificar(zxing.generarPng(contenido)));
        }
    }

    @Test
    void unaImagenSinQrNoSePuedeLeer() throws IOException {
        BufferedImage blanca = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                blanca.setRGB(x, y, 0xFFFFFF);
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(blanca, "PNG", salida);

        assertThrows(QrNoLegibleException.class, () -> zxing.decodificar(salida.toByteArray()));
    }

    @Test
    void unArchivoQueNoEsImagenNoSePuedeLeer() {
        assertThrows(QrNoLegibleException.class,
                () -> zxing.decodificar("esto no es una imagen".getBytes()));
    }

    @Test
    void unaImagenVaciaNoSePuedeLeer() {
        assertThrows(QrNoLegibleException.class, () -> zxing.decodificar(new byte[0]));
    }
}
