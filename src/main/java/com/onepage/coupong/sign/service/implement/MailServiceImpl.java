package com.onepage.coupong.sign.service.implement;

import com.onepage.coupong.common.RandomNumber;
import com.onepage.coupong.sign.dto.request.CheckEmailCertificationRequestDto;
import com.onepage.coupong.sign.dto.request.EmailCertificationRequestDto;
import com.onepage.coupong.sign.dto.response.ResponseDto;
import com.onepage.coupong.sign.dto.response.auth.CheckEmailCertificationResponseDto;
import com.onepage.coupong.sign.dto.response.auth.EmailCertificationResponseDto;
import com.onepage.coupong.sign.entity.Certification;
import com.onepage.coupong.sign.repository.CertificationRepository;
import com.onepage.coupong.sign.service.MailService;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
@Transactional
public class MailServiceImpl implements MailService {

    /* MailConfig 에서 등록해둔 Bean을 autowired 해 사용하기 */
    private final JavaMailSender emailSender;
    private final CertificationRepository certificationRepository;

    /* 사용자가 메일로 받을 인증번호 */
    private String key;

    @Value("${mail.naver.id}")
    private String id;

    /* 메일 발송
    * createMessage() 의 매개변수 to는 이메일 주소가 되고,
    * MimeMessage 객체 안에 전송할 메일의 내용을 담는다.
    * Bean으로 등록해둔 javaMail 객체를 사용해 이메일을 발송한다. */
    @Override
    public ResponseEntity<? super EmailCertificationResponseDto> sendMessage(EmailCertificationRequestDto dto) throws Exception {

        key = RandomNumber.getCertificationNumber();
        String to = dto.getEmail();
        String username = dto.getUsername();

        /* to로 메일 발송 */
        MimeMessage message = createMessage(to);

        try {

            /* 메일로 보내주는 메서드 */
            emailSender.send(message);

            /* 이메일 인증 번호 정보 DB 저장 */
            Certification certification = new Certification(username, to, key);
            certificationRepository.save(certification);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.databaseError();
        }

        return EmailCertificationResponseDto.success();
    }

    private MimeMessage createMessage(String to) throws MessagingException, UnsupportedEncodingException {

        MimeMessage message = emailSender.createMimeMessage();
        message.addRecipients(Message.RecipientType.TO, to);

        /* 이메일 제목 */
        message.setSubject("[coupong] 회원가입 이메일 인증코드");

        String mailMessage = "";
        mailMessage += "<h1 style='text-align: center;'>[coupong] 회원가입 인증메일</h1>";
        mailMessage += "<h3 style='text-align: center;'>인증코드 : <strong style='font-size: 32px; letter-spacing: 8px;'>"
                +  key + "</strong></h3>";

        /* 메일 내용, charset타입, subtype */
        message.setText(mailMessage, "utf-8", "html");
        /* 보내는 사람의 이메일 주소, 보내는 사람 이름 */
        message.setFrom(id);

        return message;
    }

    /* 사용자가 입력한 인증번호와 서버에서 생성한 인증번호를 비교하는 메서드 */
    @Override
    public ResponseEntity<? super CheckEmailCertificationResponseDto> verifyCode(CheckEmailCertificationRequestDto dto) {

        String code = dto.getCertification();
        Certification certification = certificationRepository.findCertificationByUsername(dto.getUsername());

        try {
            if (!code.equals(certification.getCertification())) {
                return CheckEmailCertificationResponseDto.certificationFailed();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.databaseError();
        }
        return CheckEmailCertificationResponseDto.success();
    }
}
