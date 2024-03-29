package cherish.backend.common.aop;

import cherish.backend.common.dto.ErrorResponseDto;
import cherish.backend.member.constant.Constants;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.TypeMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mail.MailSendException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String DEFAULT_ERROR_MSG = "알 수 없는 에러가 발생했습니다. 운영자에게 문의 바랍니다.";
    private static final ErrorResponseDto DEFAULT_ERROR_RESPONSE = new ErrorResponseDto(DEFAULT_ERROR_MSG);

    private ErrorResponseDto createError(Exception e) {
        log.error(e.getMessage(), e);
        return DEFAULT_ERROR_RESPONSE;
    }

    private ErrorResponseDto createError(Exception e, String message) {
        log.error(e.getMessage(), e);
        return new ErrorResponseDto(message);
    }

    // 자바 빈 검증 예외 처리
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ErrorResponseDto handleBindException(BindException e) {
        BindingResult bindingResult = e.getBindingResult();

        StringBuilder builder = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append("[");
            builder.append(fieldError.getField());
            builder.append("](은)는 ");
            // type mismatch exception
            if (fieldError.getCode() != null && fieldError.getCode().contains("type")) {
                builder.append("타입이 잘못 되었습니다.");
            } else {
                builder.append(fieldError.getDefaultMessage());
            }
            builder.append(" 입력된 값: [");
            builder.append(fieldError.getRejectedValue());
            builder.append("]");
        }

        return createError(e, builder.toString());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({TypeMismatchException.class, HttpMessageNotReadableException.class})
    public ErrorResponseDto handleTypeException(Exception e){
        return createError(e, "타입 오류입니다.");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ErrorResponseDto handleIllegalStateException(RuntimeException e){
        return createError(e, e.getMessage());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BadCredentialsException.class)
    public ErrorResponseDto handleCredential(BadCredentialsException e){
        return createError(e, Constants.FAILED_TO_LOGIN);
    }

    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ErrorResponseDto handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return createError(e, "잘못된 요청입니다.");
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ErrorResponseDto handleAuthenticationCredentialsNotFoundException(AuthenticationCredentialsNotFoundException e) {
        return createError(e, Constants.FAILED_TO_LOGIN);
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(JwtException.class)
    public ErrorResponseDto handleJwtException(JwtException e) {
        return createError(e, e.getMessage());
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(MailSendException.class)
    public ErrorResponseDto handleMailSendException(MailSendException e) {
        return createError(e, "메일 전송에 실패했습니다.");
    }

    // 공통 예외 처리
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ErrorResponseDto handleException(Exception e) {
        return createError(e);
    }
}
