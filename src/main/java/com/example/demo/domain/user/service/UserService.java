package com.example.demo.domain.user.service;

import com.example.demo.domain.user.entity.QUser;
import com.example.demo.domain.user.entity.QWithdrawal;
import com.example.demo.domain.user.entity.User;
import com.example.demo.domain.user.entity.Withdrawal;
import com.example.demo.domain.user.entity.request.RequestUser;
import com.example.demo.domain.user.entity.response.ResponseUser;
import com.example.demo.domain.user.repository.UserRepository;
import com.example.demo.domain.user.repository.WithdrawalRepository;
import com.example.demo.exception.ResponseMessage;
import com.example.demo.jwt.TokenProvider;
import com.example.demo.util.MailSenderRunner;
import com.example.demo.util.RedisUtil;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class UserService {


    private final TokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserRepository userRepository;
    private final JPAQueryFactory queryFactory;
    private final WithdrawalRepository withdrawalRepository;

    private final PasswordEncoder passwordEncoder;
    private final MailSenderRunner mailSenderRunner;

    private final RedisUtil redisUtil;


    public ResponseUser sign(RequestUser requestUser) {

        QUser qUser = QUser.user;

        User temp = queryFactory.selectFrom(qUser)
                .where(qUser.userEmail.eq(requestUser.getUserEmail()))
                .fetchFirst();

        if (temp != null) throw new ResponseStatusException(HttpStatus.CONFLICT, "?????? ???????????? ??????????????????.");

        requestUser = requestUser.toMember(passwordEncoder);
        User user = new User(requestUser);
        userRepository.save(user);
        ResponseUser responseUser = new ResponseUser(user);
        return responseUser;
    } // ????????????

    @Transactional
    public ResponseUser login(RequestUser requestUser) {

        QUser qUser = QUser.user;
        // 1. Login ID/PW ??? ???????????? AuthenticationToken ??????
        UsernamePasswordAuthenticationToken authenticationToken = requestUser.toAuthentication();

        // 2. ????????? ?????? (????????? ???????????? ??????) ??? ??????????????? ??????
        //    authenticate ???????????? ????????? ??? ??? CustomUserDetailsService ?????? ???????????? loadUserByUsername ???????????? ?????????
        Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

        // 3. ?????? ????????? ???????????? JWT ?????? ??????

        User user = queryFactory.selectFrom(qUser)
                .where(qUser.userEmail.eq(requestUser.getUserEmail()))
                .fetchOne();
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "???????????? ?????? ??????????????????.");

        ResponseUser responseUser = new ResponseUser(user);
        responseUser = tokenProvider.generateUserInfo(authentication, responseUser);
        user = new User(responseUser);

        // 4. RefreshToken ??????
        userRepository.save(user);
        // 5. ?????? ??????
        return responseUser;

    }//?????????


    @Transactional
    public ResponseUser reissue(RequestUser requestUser) {
        // 1. Refresh Token ??????

        if (!tokenProvider.validateToken(requestUser.getRefreshToken())) {
            throw new RuntimeException("Refresh Token ??? ???????????? ????????????.");
        }

        // 2. Access Token ?????? Member ID ????????????
        Authentication authentication = tokenProvider.getAuthentication(requestUser.getAccessToken());


        // 3. ??????????????? Member ID ??? ???????????? Refresh Token ??? ?????????


        User userInfoTemp = userRepository.findById(Long.parseLong(authentication.getName()))
                .orElseThrow(() -> new RuntimeException("???????????? ??? ??????????????????."));

        // 4. Refresh Token ??????????????? ??????
        if (!userInfoTemp.getRefreshToken().equals(requestUser.getRefreshToken())) {
            throw new RuntimeException("????????? ?????? ????????? ???????????? ????????????.");
        }

        // 5. ????????? ?????? ??????

        Optional<User> optUserInfo = userRepository.findByRefreshToken(requestUser.getRefreshToken());
        if (!optUserInfo.isPresent()) throw new RuntimeException("???????????? ????????? ????????? ????????? ????????????");
        User user = optUserInfo.get();
        ResponseUser responseUser = new ResponseUser(user);
        responseUser = tokenProvider.generateUserInfo(authentication, responseUser);

        user = new User(responseUser);
        // 6. ????????? ?????? ????????????
        userRepository.save(user);
        // ?????? ??????
        return responseUser;

    }// ?????? ?????????


    @Transactional
    public ResponseUser update(RequestUser requestUser) {

        QUser qUser = QUser.user;

        Long result = queryFactory.update(qUser)
                .set(qUser.userAddress, requestUser.getUserAddress())
                .set(qUser.userBirth, requestUser.getUserBirth())
                .set(qUser.userNickname, requestUser.getUserNickname())
                .set(qUser.updateDateTime, LocalDateTime.now())
                .where(qUser.userId.eq(requestUser.getUserId()))
                .execute();
        if (result == 1) {
            User user = queryFactory
                    .selectFrom(qUser)
                    .where(qUser.userId.eq(requestUser.getUserId()))
                    .fetchOne();
            ResponseUser responseUser = new ResponseUser(user);
            return responseUser;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "?????? ????????? ???????????? ???????????????.");
        }
    }//?????? ??????

    @Transactional
    public Withdrawal delete(Long id) {

        QUser qUser = QUser.user;
        QWithdrawal qWithdrawal = QWithdrawal.withdrawal;


        User user = queryFactory
                .selectFrom(qUser)
                .where(qUser.userId.eq(id))
                .fetchOne();

        Long result = queryFactory.delete(qUser)
                .where(qUser.userId.eq(id).and(qUser.userAuthority.notEqualsIgnoreCase("ROLE_ADMIN")))
                .execute();
        
        if (result == 1) {
            Withdrawal withdrawal = new Withdrawal(user);
            withdrawalRepository.save(withdrawal); // ??????
            return withdrawal;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "?????? ????????? ???????????? ???????????????.");
        }

    }//?????? ??????

    @Transactional
    public ResponseUser updatePassword(RequestUser requestUser) {

        QUser qUser = QUser.user;

        Long result = queryFactory.update(qUser)
                .set(qUser.userPassword, passwordEncoder.encode(requestUser.getNewPassword()))
                .set(qUser.updateDateTime, LocalDateTime.now())
                .where(qUser.userId.eq(requestUser.getUserId()).and(qUser.userPassword.eq(requestUser.getUserPassword())))
                .execute();
        if (result == 1) {

            User user = queryFactory
                    .selectFrom(qUser)
                    .where(qUser.userId.eq(requestUser.getUserId()))
                    .fetchOne();
            ResponseUser responseUser = new ResponseUser(user);
            return responseUser;
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "???????????? ????????? ??????????????????.");
        }
    } //???????????? ??????


    public void sendEmail(RequestUser requestUser) {


        QUser qUser = QUser.user;

        User user = queryFactory
                .selectFrom(qUser)
                .where(qUser.userEmail.eq(requestUser.getUserEmail()))
                .fetchOne();

        if (user != null) {
            Random random = new Random();
            String authKey = String.valueOf(random.nextInt(888888) + 111111);      // ?????? : 111111 ~ 999999

            redisUtil.setDataExpire(requestUser.getUserEmail(), authKey, 60 * 3l); //3???
            mailSenderRunner.sendAuthEmail(requestUser.getUserEmail(), authKey, "code");
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "???????????? ?????? ??????????????????.");
        }
    } //????????? ???????????? ??????


    @Transactional
    public ResponseMessage authEmail(RequestUser requestUser) {

        String emailVerificationCode = redisUtil.getData(requestUser.getUserEmail());

        if (requestUser.getEmailVerificationCode().equals(emailVerificationCode)) {

            QUser qUser = QUser.user;
            String tempPassword = mailSenderRunner.getRamdomPassword(10);
            mailSenderRunner.sendAuthEmail(requestUser.getUserEmail(), tempPassword, "password");
            queryFactory
                    .update(qUser)
                    .set(qUser.userPassword, passwordEncoder.encode(tempPassword))
                    .execute();
            return ResponseMessage.builder().Message("????????? ?????????????????????.").statusCode(200).build();
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "????????? ?????? ???????????????.");
        }
    }// ????????? ???????????? ??????

}
