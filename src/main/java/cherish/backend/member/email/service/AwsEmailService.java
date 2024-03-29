package cherish.backend.member.email.service;

import cherish.backend.member.dto.EmailSenderDto;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.model.SendEmailResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSendException;

@Slf4j
@Profile("aws")
@RequiredArgsConstructor
public class AwsEmailService implements EmailService {
    private final AmazonSimpleEmailService amazonSimpleEmailService;
    @Override
    public void sendMessage(String to, String code) {
        final EmailSenderDto senderDto = EmailSenderDto.builder() // 1
                .to(to)
                .content(code)
                .build();

        final SendEmailResult sendEmailResult = amazonSimpleEmailService // 2
                .sendEmail(senderDto.toSendRequestDto());

        sendingResultMustSuccess(sendEmailResult); // 3
    }

    private void sendingResultMustSuccess(SendEmailResult sendEmailResult) {
        if (sendEmailResult.getSdkHttpMetadata().getHttpStatusCode() != 200) {
            log.error("{}", sendEmailResult.getSdkResponseMetadata().toString());
            throw new MailSendException("이메일 인증코드 발송에 성공하지 못했습니다.");
        }
    }
}
