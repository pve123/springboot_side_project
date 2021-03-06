package com.example.demo.domain.user.controller;

import com.example.demo.domain.user.entity.request.RequestUser;
import com.example.demo.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class UserController {


    private final UserService userService;

    @PostMapping(path = "/sign")
    @Operation(summary = "회원가입", description = "사용자 회원가입 API", responses = {
            @ApiResponse(responseCode = "201", description = "회원가입 성공"),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")})
    public ResponseEntity sign(@RequestBody RequestUser requestUser) {

        return ResponseEntity.status(HttpStatus.CREATED).body(userService.sign(requestUser));
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "사용자 로그인 API", responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이메일")})
    public ResponseEntity login(@RequestBody RequestUser requestUser) {

        return ResponseEntity.status(HttpStatus.OK).body(userService.login(requestUser));
    }

    @PostMapping("/reissue")
    @Operation(summary = "사용자 토큰 재발급", description = "사용자 RefreshToken으로 Access 토큰 재발급 API", responses = {
            @ApiResponse(responseCode = "200", description = "사용자 Access 토큰 재발급 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")})
    public ResponseEntity reissue(@RequestBody RequestUser requestUser) {

        return ResponseEntity.status(HttpStatus.OK).body(userService.reissue(requestUser));
    }

    @PutMapping(path = "/user")
    @Operation(summary = "내 정보 수정", description = "회원정보 수정 API", responses = {
            @ApiResponse(responseCode = "200", description = "회원정보 수정 성공"),
            @ApiResponse(responseCode = "400", description = "회원정보 수정 실패")})
    public ResponseEntity update(@RequestBody RequestUser requestUser) {


        return ResponseEntity.status(HttpStatus.OK).body(userService.update(requestUser));
    }

    @DeleteMapping(path = "/user/{id}")
    @Operation(summary = "회원탈퇴", description = "회원탈퇴 API", responses = {
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "회원탈퇴 실패")})
    public ResponseEntity delete(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.delete(id));
    }


    @PutMapping(path = "/user/password")
    @Operation(summary = "비밀번호 변경", description = "비밀번호 변경 API", responses = {
            @ApiResponse(responseCode = "200", description = "비밀번호 변경 성공"),
            @ApiResponse(responseCode = "400", description = "비밀번호 변경 실패")})
    public ResponseEntity updatePassword(@RequestBody RequestUser requestUser) {
        return ResponseEntity.status(HttpStatus.OK).body(userService.updatePassword(requestUser));
    }

    @PostMapping(path = "/send/email")
    @Operation(summary = "이메일 인증 코드 발송", description = "이메일 인증코드 발송 API ( Redis )", responses = {
            @ApiResponse(responseCode = "204", description = "사용자 이메일 인증코드 발송 성공"),
            @ApiResponse(responseCode = "404", description = "가입되어 있지 않은 이메일")})
    public ResponseEntity sendEmail(@RequestBody RequestUser requestUser) {

        userService.sendEmail(requestUser);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping(path = "/auth/email")
    @Operation(summary = "이메일 인증 코드 검증", description = "이메일 인증코드 검증 후 임시 비밀번호 발급 API ( Redis )", responses = {
            @ApiResponse(responseCode = "200", description = "사용자 이메일 인증코드 검증 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인증 코드")})
    public ResponseEntity authEmail(@RequestBody RequestUser requestUser) {


        return ResponseEntity.status(HttpStatus.OK).body(userService.authEmail(requestUser));
    }


}
