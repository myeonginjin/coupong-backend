package com.onepage.coupong.sign.service.implement;

import com.onepage.coupong.sign.dto.request.auth.IdCheckRequestDto;
import com.onepage.coupong.sign.dto.request.auth.SignInRequestDto;
import com.onepage.coupong.sign.dto.request.auth.SignUpRequestDto;
import com.onepage.coupong.sign.dto.response.ResponseDto;
import com.onepage.coupong.sign.dto.response.auth.IdCheckResponseDto;
import com.onepage.coupong.sign.dto.response.auth.SignInResponseDto;
import com.onepage.coupong.sign.dto.response.auth.SignUpResponseDto;
import com.onepage.coupong.sign.entity.Certification;
import com.onepage.coupong.entity.User;
import com.onepage.coupong.sign.provider.JwtProvider;
import com.onepage.coupong.sign.repository.CertificationRepository;
import com.onepage.coupong.sign.repository.UserRepository;
import com.onepage.coupong.sign.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final CertificationRepository certificationRepository;
    private final JwtProvider jwtProvider;

    /* BCryptPasswordEncoder() -> 스프링 시큐리티 프레임워크에서 제공하는 클래스 중 하나로 비밀번호를 암호화하는데 사용할 수 있는 메서드를 가진 클래스
    * BCrypt 해싱함수를 사용해서 비밀번호를 인코딩해주는 메서드와 사용자에 의해 제출된 원본 비밀번호와 DB에 저장되어 있는 암호화된 비밀번호의 일치 여부를 확인해주는 matches() 메서드를 제공
    * PasswordEncoder interface를 구현한 클래스이다. */
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /* 아이디 중복 검사 확인 서비스 */
    @Override
    public ResponseEntity<? super IdCheckResponseDto> idCheck(IdCheckRequestDto dto) {
        try {
            String username = dto.getUsername();
            boolean isExist = userRepository.existsByUsername(username);
            /* 요청으로부터 받은 username이 이미 DB에 존재하는 경우 중복 코드와 메시지 반환 */
            if (isExist) {
                return IdCheckResponseDto.duplicated();
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.databaseError();
        }
        return IdCheckResponseDto.success();
    }

    /* 회원가입 서비스 */
    @Override
    public ResponseEntity<? super SignUpResponseDto> signUp(SignUpRequestDto dto) {
        try {
            /* idCheck()와 동일한 작업 -> 굳이 없어도 될것 같은데 추후에 생각 */
            String username = dto.getUsername();
            boolean isExistUser = userRepository.existsByUsername(username);
            if (isExistUser) {
                return SignUpResponseDto.duplicated();
            }

            String email = dto.getEmail();
            String certificationNumber = dto.getCertification();
            Certification certification = certificationRepository.findCertificationByUsername(username);

            /* 요청으로부터 받은 username으로 전송한 Certification 정보가 없다면 에러 반환 */
            if (certification == null) {
                return ResponseDto.validationFailed();
            }

            /* 요청으로부터 받은 email과 그에 대응하는 인증번호와 DB에 저장된 email - 인증번호가 일치하는지 확인 */
            boolean isMatched = certification.getEmail().equals(email)
                    && certification.getCertification().equals(certificationNumber);

            /* 만약 요청한 인증번호와 DB의 인증번호가 다르다면 에러 반환 */
            if (!isMatched) {
                return SignUpResponseDto.certificationFailed();
            }

            /* BCryptPasswordEncoder 클래스의 encode() 메서드를 이용해 클라이언트가 입력한 비밀번호를 암호화시키기고
            * 암호화된 비밀번호를 DB에 저장하기 위해 dto에 다시 넣어준다. */
            String password = dto.getPassword();
            String encodedPassword = passwordEncoder.encode(password);
            dto.setPassword(encodedPassword);

            /* User 정보를 저장하기 위한 새로운 User 객체 생성후 등록할 정보가 담긴 dto 저장 */
            User user = new User(dto);
            userRepository.save(user);

            /* 회원가입을 위한 인증번호를 사용한 뒤, 해당 데이터는 필요없으므로 삭제 */
            certificationRepository.deleteByUsername(username);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.databaseError();
        }
        return SignUpResponseDto.success();
    }

    /* 로그인 서비스 */
    @Override
    public ResponseEntity<? super SignInResponseDto> signIn(SignInRequestDto dto) {

        /* 로그인이 성공적으로 되면 token을 생성해서 보내줘야함. */
        String token = null;

        try {
            /* 로그인 요청에 입력한 username이 DB에 존재하지 않으면 로그인 실패 에러를 보내준다. */
            String username = dto.getUsername();
            User user = userRepository.findUserByUsername(username);
            if (user == null) {
                return SignInResponseDto.signInFailed();
            }

            /* matches() 메서드를 통해 요청으로부터 받은 원본 비밀번호와 DB에 저장된 암호화된 비밀번호를 매칭한 결과를 이용해
            * 비밀번호가 다르다면 로그인 실패 에러를 보내준다. */
            String password = dto.getPassword();
            String encodedPassword = user.getPassword();
            boolean isMatched = passwordEncoder.matches(password, encodedPassword);
            if (!isMatched) {
                return SignInResponseDto.signInFailed();
            }

            /* 로그인이 성공한 경우 토큰 생성 */
            token = jwtProvider.create(username);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.databaseError();
        }

        /* 성공 코드, 메시지와 토큰을 함께 보내준다. */
        return SignInResponseDto.success(token);
    }
}
