package com.piggy.piggybackend.global.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PARAMETER) // 파라미터에만 붙일 수 있게 설정
@Retention(RetentionPolicy.RUNTIME) // 실행 중에 참조 가능하도록 설정
public @interface LoginUser {
}