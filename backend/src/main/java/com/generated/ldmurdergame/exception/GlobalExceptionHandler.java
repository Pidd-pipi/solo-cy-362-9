package com.generated.ldmurdergame.exception;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, String>> handleApiException(ApiException exception) {
    return ResponseEntity.status(exception.getStatus()).body(Map.of("message", exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getDefaultMessage() == null ? "参数不合法" : error.getDefaultMessage())
        .distinct()
        .collect(Collectors.joining("；"));
    return ResponseEntity.badRequest().body(Map.of("message", message.isBlank() ? "参数不合法" : message));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<Map<String, String>> handleUnreadableBody(HttpMessageNotReadableException exception) {
    return ResponseEntity.badRequest().body(Map.of("message", "请求体格式错误"));
  }

  /** 唯一约束兜底：行锁之外的极端并发下也不会出现同场次重复占位。 */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", "数据冲突：该玩家可能已在此场次报名"));
  }
}
