package com.nexus.hotelpms.infraestructura.pdf;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Generador mínimo de PDF de una página (texto Helvetica + QR dibujado con rectángulos). Sin dependencias de PDF. */
final class MiniPdf {
    private final StringBuilder contenido = new StringBuilder();
    private double y = 800;

    void titulo(String texto) { texto(50, y, 18, true, texto); y -= 30; }
    void linea(String texto) { if (y > 40) { texto(50, y, 10.5, false, texto); } y -= 15; }
    void negrita(String texto) { if (y > 40) { texto(50, y, 11, true, texto); } y -= 18; }
    void espacio(double h) { y -= h; }

    private void texto(double x, double yy, double size, boolean bold, String t) {
        contenido.append(String.format(Locale.ROOT, "BT /%s %.1f Tf %.1f %.1f Td (%s) Tj ET\n",
                bold ? "F2" : "F1", size, x, yy, escapar(t)));
    }

    private String escapar(String t) {
        return t.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    /** Dibuja un QR con {@code lado} puntos, esquina superior izquierda en la posición actual. */
    void qr(String datos, double lado) {
        try {
            BitMatrix m = new QRCodeWriter().encode(datos, BarcodeFormat.QR_CODE, 0, 0);
            double mod = lado / m.getWidth();
            double top = y;
            for (int fila = 0; fila < m.getHeight(); fila++) {
                for (int col = 0; col < m.getWidth(); col++) {
                    if (m.get(col, fila)) {
                        contenido.append(String.format(Locale.ROOT, "%.2f %.2f %.2f %.2f re f\n",
                                50 + col * mod, top - (fila + 1) * mod, mod, mod));
                    }
                }
            }
            y -= lado + 10;
        } catch (WriterException e) {
            throw new IllegalStateException("No se pudo generar el QR", e);
        }
    }

    byte[] construir() {
        byte[] stream = contenido.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<String> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        objetos.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R /F2 6 0 R >> >> /Contents 5 0 R >>");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>");
        objetos.add(null); // 5: stream
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        escribir(out, "%PDF-1.4\n");
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(out.size());
            escribir(out, (i + 1) + " 0 obj\n");
            if (objetos.get(i) == null) {
                escribir(out, "<< /Length " + stream.length + " >>\nstream\n");
                out.writeBytes(stream);
                escribir(out, "\nendstream");
            } else {
                escribir(out, objetos.get(i));
            }
            escribir(out, "\nendobj\n");
        }
        int xref = out.size();
        escribir(out, "xref\n0 " + (objetos.size() + 1) + "\n0000000000 65535 f \n");
        for (int off : offsets) {
            escribir(out, String.format(Locale.ROOT, "%010d 00000 n \n", off));
        }
        escribir(out, "trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n");
        return out.toByteArray();
    }

    private void escribir(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }
}
