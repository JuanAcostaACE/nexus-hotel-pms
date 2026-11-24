package com.nexus.hotelpms.presentacion.excepcion;

import com.nexus.hotelpms.dominio.excepcion.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Traduce excepciones de dominio a respuestas HTTP con mensajes en español (los textos salen de las HU). */
@RestControllerAdvice
public class ManejadorExcepciones {
    private static final Logger log = LoggerFactory.getLogger(ManejadorExcepciones.class);

    @ExceptionHandler(ReglaNegocioException.class)
    ResponseEntity<Map<String, Object>> regla(ReglaNegocioException e) { return resp(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), null); }

    @ExceptionHandler(ConflictoException.class)
    ResponseEntity<Map<String, Object>> conflicto(ConflictoException e) { return resp(HttpStatus.CONFLICT, e.getMessage(), null); }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException e) { return resp(HttpStatus.NOT_FOUND, e.getMessage(), null); }

    @ExceptionHandler(AccesoDenegadoException.class)
    ResponseEntity<Map<String, Object>> denegado(AccesoDenegadoException e) { return resp(HttpStatus.FORBIDDEN, e.getMessage(), null); }

    @ExceptionHandler(CredencialesInvalidasException.class)
    ResponseEntity<Map<String, Object>> credenciales(CredencialesInvalidasException e) { return resp(HttpStatus.UNAUTHORIZED, e.getMessage(), null); }

    @ExceptionHandler(CuentaBloqueadaException.class)
    ResponseEntity<Map<String, Object>> bloqueada(CuentaBloqueadaException e) { return resp(HttpStatus.LOCKED, e.getMessage(), null); }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> validacion(MethodArgumentNotValidException e) {
        var errores = e.getBindingResult().getFieldErrors();
        List<String> detalle = errores.stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        String mensaje = errores.size() == 1
                ? errores.get(0).getDefaultMessage()
                : "Faltan campos obligatorios o son inválidos: "
                        + String.join(", ", errores.stream().map(f -> f.getField()).distinct().toList());
        return resp(HttpStatus.BAD_REQUEST, mensaje, detalle);
    }

    /** Valores de enum inválidos (p. ej. categoría "Otro", HU-16) con la lista de valores permitidos. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<Map<String, Object>> cuerpoIlegible(HttpMessageNotReadableException e) {
        Throwable causa = e.getCause();
        if (causa instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException ife
                && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String campo = ife.getPath().isEmpty() ? "valor" : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            String permitidos = String.join(", ", java.util.Arrays.stream(ife.getTargetType().getEnumConstants())
                    .map(Object::toString).toList());
            String msg = "categoria".equals(campo)
                    ? "Categoría no válida. Valores permitidos: " + permitidos
                    : "Valor no válido para '" + campo + "'. Valores permitidos: " + permitidos;
            return resp(HttpStatus.BAD_REQUEST, msg, null);
        }
        return formato(e);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<Map<String, Object>> formato(Exception e) {
        return resp(HttpStatus.BAD_REQUEST, "Formato de datos inválido (revise fechas AAAA-MM-DD y números)", null);
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    ResponseEntity<Map<String, Object>> rutaInexistente(Exception e) {
        return resp(HttpStatus.NOT_FOUND, "La ruta solicitada no existe", null);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Map<String, Object>> metodo(Exception e) {
        return resp(HttpStatus.METHOD_NOT_ALLOWED, "Método HTTP no soportado para esta ruta", null);
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    ResponseEntity<Map<String, Object>> parametro(org.springframework.web.bind.MissingServletRequestParameterException e) {
        return resp(HttpStatus.BAD_REQUEST, "Falta el parámetro obligatorio: " + e.getParameterName(), null);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    ResponseEntity<Map<String, Object>> sinPermiso(org.springframework.security.access.AccessDeniedException e) {
        return resp(HttpStatus.FORBIDDEN, "No tiene permisos para acceder a esta sección", null);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> inesperado(Exception e) {
        log.error("Error inesperado", e);
        return resp(HttpStatus.INTERNAL_SERVER_ERROR, "Error interno del servidor", null);
    }

    private ResponseEntity<Map<String, Object>> resp(HttpStatus st, String mensaje, Object detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("estado", st.value());
        body.put("error", st.getReasonPhrase());
        body.put("mensaje", mensaje);
        if (detalle != null) {
            body.put("detalle", detalle);
        }
        return ResponseEntity.status(st).body(body);
    }
}
