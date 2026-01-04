# Payment System

Spring Boot 기반의 간단하고 확장 가능한 결제 시스템 API

## 목차
- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [설계 결정사항](#설계-결정사항)
- [실행 방법](#실행-방법)
- [API 명세](#api-명세)
- [테스트](#테스트)

## 프로젝트 개요

이 프로젝트는 회원-주문-결제 도메인을 포함한 RESTful API 기반 결제 시스템입니다. <br>
도메인 주도 설계(DDD) 원칙, 계층화 아키텍처, 전략 패턴을 적용하여 유지보수성과 확장성을 고려했습니다.

**핵심 특징:**
- 회원 등급별 차등 할인 정책 적용 (전략 패턴)
- 결제 상태 관리 및 상태 전이 규칙 (도메인 모델)
- 주문-결제 연관 관계 및 할인 자동 적용

## 기술 스택

- **Java 17**
- **Spring Boot 3.3.6**: 프레임워크
- **Spring Data JPA**: 데이터 영속성 관리
- **H2 Database**: 인메모리 데이터베이스
- **Lombok**: 보일러플레이트 코드 감소
- **SpringDoc OpenAPI**: API 문서화 (Swagger)
- **JUnit 5 & Mockito**: 테스트
- **Gradle**: 빌드 도구

## 주요 기능

### 1. 회원 관리
- 회원 등록 (이름, 등급)
- 회원 조회 (단건, 전체 목록)
- 회원 등급: NORMAL, VIP, VVIP

### 2. 주문 생성
- 상품명, 가격, 회원 정보로 주문 생성
- 회원과 주문의 연관 관계 관리
- 주문 조회 (단건, 전체 목록, 회원별 조회)

### 3. 결제 생성 및 할인 적용
- 주문 기반 결제 생성
- **회원 등급별 자동 할인 적용**
    - NORMAL: 할인 없음
    - VIP: 1,000원 고정 할인
    - VVIP: 10% 비율 할인
- 할인 전 금액, 할인 금액, 최종 금액 자동 계산
- 중복 결제 방지 (같은 주문에 승인된 결제가 있으면 차단)
- 초기 상태: PENDING

### 4. 결제 승인
- PENDING 상태의 결제를 APPROVED 상태로 변경
- 상태 전이 규칙 검증

### 5. 결제 취소
- PENDING 또는 APPROVED 상태의 결제를 CANCELLED 상태로 변경
- 이미 취소/환불된 결제는 취소 불가

### 6. 결제 환불
- APPROVED 상태의 결제만 REFUNDED 상태로 변경 가능
- 승인되지 않은 결제는 환불 불가

### 7. 결제 조회
- 단건 조회 (ID)
- 전체 목록 조회
- 회원별 조회
- 상태별 조회

## 프로젝트 구조

```
src/main/java/com/polycube/
├── domain/                          # 도메인 모델
│   ├── Member.java                  # 회원 엔티티
│   ├── Order.java                   # 주문 엔티티
│   ├── Payment.java                 # 결제 엔티티
│   └── enums/
│       ├── MemberGrade.java         # 회원 등급
│       ├── PaymentStatus.java       # 결제 상태
│       └── PaymentMethod.java       # 결제 수단
├── repository/                      # 데이터 접근 계층
│   ├── MemberRepository.java
│   ├── OrderRepository.java
│   └── PaymentRepository.java
├── service/                         # 비즈니스 로직 계층
│   ├── MemberService.java
│   ├── OrderService.java
│   ├── PaymentService.java
│   └── discount/                    # 할인 정책 (전략 패턴)
│       ├── DiscountPolicy.java      # 할인 정책 인터페이스
│       ├── MemberGradeDiscountPolicy.java  # 회원 등급별 할인
│       ├── FixedAmountDiscountPolicy.java  # 고정 금액 할인
│       └── PercentageDiscountPolicy.java   # 비율 할인
├── controller/                      # API 컨트롤러 계층
│   ├── MemberController.java
│   ├── OrderController.java
│   └── PaymentController.java
├── dto/                             # 데이터 전송 객체
│   ├── MemberRequest.java
│   ├── MemberResponse.java
│   ├── OrderRequest.java
│   ├── OrderResponse.java
│   ├── PaymentRequest.java
│   ├── PaymentResponse.java
│   └── ErrorResponse.java
├── config/                          # 설정
│   └── SwaggerConfig.java
└── exception/                       # 예외 처리
    ├── MemberNotFoundException.java
    ├── OrderNotFoundException.java
    ├── PaymentNotFoundException.java
    ├── InvalidPaymentStateException.java
    ├── DuplicatePaymentException.java
    └── GlobalExceptionHandler.java
```

## 설계 결정사항

### 1. 도메인 주도 설계 (DDD) 적용

**결정 이유**
- 비즈니스 로직을 도메인 모델에 캡슐화하여 응집도 향상
- Payment 엔티티 내부에 `approve()`, `cancel()`, `refund()` 메서드를 구현하여 상태 전이 규칙을 엔티티가 직접 관리

**구현**
```java
public void approve() {
    if (this.status != PaymentStatus.PENDING) {
        throw new IllegalStateException("PENDING 상태의 결제만 승인할 수 있습니다.");
    }
    this.status = PaymentStatus.APPROVED;
}
```

### 2. 계층화 아키텍처 (Layered Architecture)

**결정 이유**
- Controller, Service, Repository로 명확한 책임 분리
- 각 계층의 독립적인 테스트 가능
- 유지보수성 및 확장성 향상

**계층별 역할**
- **Controller**: HTTP 요청/응답 처리, 입력 검증
- **Service**: 트랜잭션 관리, 비즈니스 로직 조율
- **Repository**: 데이터 영속성 관리

### 3. 불변성과 상태 관리

**결정 이유**
- Payment 엔티티의 상태 변경을 메서드를 통해서만 가능하도록 제한
- 직접적인 setter 사용을 배제하고 Lombok의 `@Getter`만 사용
- 상태 전이 규칙을 도메인 모델이 강제

### 4. 예외 처리 전략

**결정 이유**
- `@RestControllerAdvice`를 사용한 전역 예외 처리
- 커스텀 예외로 명확한 에러 메시지 제공
- 일관된 에러 응답 형식 (ErrorResponse DTO)

**구현 범위**
- 비즈니스 예외: `PaymentNotFoundException`, `DuplicatePaymentException` 등
- 유효성 검증: `@Valid`, Bean Validation
- 시스템 예외: JSON 파싱 오류, 타입 불일치, DB 제약 조건 위반
- 총 11개 예외 핸들러로 다양한 오류 상황 처리

### 5. DTO 패턴 사용

**결정 이유**
- 엔티티를 직접 노출하지 않고 DTO를 통해 필요한 정보만 전달
- API 응답 스키마와 도메인 모델의 독립성 보장
- 순환 참조 방지 및 보안 향상

### 6. H2 인메모리 데이터베이스 사용

**결정 이유**
- 개발 및 테스트 환경의 간편한 설정
- 프로덕션 환경에서는 PostgreSQL, MySQL 등으로 쉽게 전환 가능
- JPA 추상화 덕분에 데이터베이스 변경 시 코드 수정 최소화

### 7. 할인 정책 - 전략 패턴 (Strategy Pattern) 적용

**결정 이유**
- 다양한 할인 정책을 런타임에 유연하게 교체/확장 가능
- OCP(개방-폐쇄 원칙): 새로운 할인 정책 추가 시 기존 코드 수정 없이 확장 가능
- 할인 정책별 단일 책임 분리

**구현**
```java
// 인터페이스
public interface DiscountPolicy {
    BigDecimal discount(Member member, BigDecimal price);
}

// 구현체
- FixedAmountDiscountPolicy: 고정 금액 할인 (VIP: 1,000원)
- PercentageDiscountPolicy: 비율 할인 (VVIP: 10%)
- MemberGradeDiscountPolicy: 회원 등급별 정책 선택
```

**장점**
- 새로운 할인 정책(시즌 할인, 쿠폰 할인 등) 추가 시 기존 코드 수정 불필요
- 각 할인 정책을 독립적으로 테스트 가능
- 정책 조합 및 우선순위 변경 용이

### 8. 인증/인가 미구현 (의도적 범위 제외)

**결정 이유**
- 과제 범위를 핵심 비즈니스 로직(결제 도메인, 할인 정책, DDD, 디자인 패턴)에 집중
- Spring Security 구현 시 프로젝트 복잡도가 크게 증가하여 핵심 설계가 흐려질 수 있음
- 현재는 단순 ID 기반 조회이지만, 실제 프로덕션에서는 필수 구현 사항임을 인지

**현재 구조의 한계점**
- ID만 알면 다른 사용자의 정보 조회 가능
- 인증 없이 모든 API 호출 가능
- 본인 확인 로직 없음

**프로덕션 적용 시 필요 사항**
- JWT/OAuth 기반 인증
- 본인 확인 로직 (토큰의 userId와 요청 데이터의 memberId 검증)
- 역할 기반 접근 제어 (일반 사용자 vs 관리자)

### 9. 테스트 전략

**결정 이유**
- **단위 테스트**: Service 계층 로직을 Mockito로 격리 테스트
- **통합 테스트**: Repository 계층은 실제 DB와 통합하여 테스트
- 할인 정책별 독립적인 테스트
- 높은 테스트 커버리지로 안정성 보장

## 실행 방법

### 1. 사전 요구사항
- Java 17 이상
- Gradle (또는 내장된 Gradle Wrapper 사용)

### 2. 프로젝트 빌드
```bash
./gradlew clean build
```

### 3. 애플리케이션 실행
```bash
./gradlew bootRun
```

또는

```bash
java -jar build/libs/payment-system-0.0.1-SNAPSHOT.jar
```

### 4. 애플리케이션 접속
- API 서버: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console
    - JDBC URL: `jdbc:h2:mem:paymentdb`
    - Username: `sa`
    - Password: (비워두기)

### 5. 기본 동작 확인 (Quick Start)

```bash
# 1. VIP 회원 생성
curl -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"John Doe","grade":"VIP"}'
# Response: {"id":1,"name":"John Doe","grade":"VIP",...}

# 2. 주문 생성
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"productName":"Laptop","originalPrice":1500000.00}'
# Response: {"id":1,"productName":"Laptop","originalPrice":1500000.00,...}

# 3. 결제 생성 (VIP 할인 1,000원 자동 적용)
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"orderId":1,"amount":1500000.00,"currency":"KRW","paymentMethod":"CREDIT_CARD","description":"Laptop purchase"}'
# Response: {"id":1,"amount":1500000.00,"discountAmount":1000.00,"finalAmount":1499000.00,...}

# 4. 결제 승인
curl -X POST http://localhost:8080/api/payments/1/approve
# Response: {"id":1,"status":"APPROVED",...}

# 5. 결제 조회
curl http://localhost:8080/api/payments/1
```

## API 명세

### Base URL
```
http://localhost:8080/api
```

### 1. 회원 관리 API

#### 1.1 회원 생성
```http
POST /api/members
Content-Type: application/json

{
  "name": "이호연",
  "grade": "VIP"
}
```

**Response:**
```json
{
  "id": 1,
  "name": "이호연",
  "grade": "VIP",
  "createdAt": "2026-01-04T10:00:00",
  "updatedAt": "2026-01-04T10:00:00"
}
```

#### 1.2 회원 조회
```http
# 단건 조회
GET /api/members/{id}

# 전체 목록 조회
GET /api/members
```

### 2. 주문 관리 API

#### 2.1 주문 생성
```http
POST /api/orders
Content-Type: application/json

{
  "memberId": 1,
  "productName": "노트북",
  "originalPrice": 1500000.00
}
```

**Response:**
```json
{
  "id": 1,
  "productName": "노트북",
  "originalPrice": 1500000.00,
  "memberId": 1,
  "memberName": "이호연",   "createdAt": "2026-01-04T10:01:00",
  "updatedAt": "2026-01-04T10:01:00"
}
```

#### 2.2 주문 조회
```http
# 단건 조회
GET /api/orders/{id}

# 전체 목록 조회
GET /api/orders

# 회원별 조회
GET /api/orders?memberId=1
```

### 3. 결제 관리 API

#### 3.1 결제 생성 (할인 자동 적용)
```http
POST /api/payments
Content-Type: application/json

{
  "orderId": 1,
  "amount": 1500000.00,
  "currency": "KRW",
  "paymentMethod": "CREDIT_CARD",
  "description": "노트북 구매"
}
```

**Response (VIP 회원의 경우):**
```json
{
  "id": 1,
  "orderId": 1,
  "amount": 1500000.00,
  "discountAmount": 1000.00,
  "finalAmount": 1499000.00,
  "currency": "KRW",
  "status": "PENDING",
  "paymentMethod": "CREDIT_CARD",
  "description": "노트북 구매",
  "createdAt": "2026-01-04T10:02:00",
  "updatedAt": "2026-01-04T10:02:00"
}
```

#### 3.2 결제 승인
```http
POST /api/payments/{id}/approve
```

#### 3.3 결제 취소
```http
POST /api/payments/{id}/cancel
```

#### 3.4 결제 환불
```http
POST /api/payments/{id}/refund
```

#### 3.5 결제 조회
```http
# 단건 조회
GET /api/payments/{id}

# 전체 목록 조회
GET /api/payments

# 회원별 조회
GET /api/payments?memberId=1

# 상태별 조회
GET /api/payments?status=APPROVED

# 회원+상태 조합 조회
GET /api/payments?memberId=1&status=APPROVED
```

### Enum 값 정의

#### Member Grade
- `NORMAL`: 일반 회원 (할인 없음)
- `VIP`: VIP 회원 (1,000원 할인)
- `VVIP`: VVIP 회원 (10% 할인)

#### Payment Status
- `PENDING`: 결제 대기
- `APPROVED`: 결제 승인
- `CANCELLED`: 결제 취소
- `REFUNDED`: 환불 완료

#### Payment Method
- `CREDIT_CARD`: 신용카드
- `DEBIT_CARD`: 체크카드
- `BANK_TRANSFER`: 계좌이체
- `VIRTUAL_ACCOUNT`: 가상계좌
- `POINT`: 포인트

## 테스트

### 전체 테스트 실행
```bash
./gradlew test
```

### 테스트 구성
- **PaymentServiceTest**: Payment 서비스 단위 테스트 (Mockito 사용)
- **PaymentServiceWithDiscountTest**: 할인 정책 통합 테스트
- **MemberGradeDiscountPolicyTest**: 할인 정책별 단위 테스트
- **PaymentRepositoryTest**: Repository 통합 테스트 (실제 DB 사용)

### 테스트 커버리지
- 결제 생성/승인/취소/환불의 정상 케이스
- 회원 등급별 할인 정책 적용 검증
    - NORMAL: 할인 없음
    - VIP: 1,000원 고정 할인
    - VVIP: 10% 비율 할인
- 예외 상황 처리 (존재하지 않는 결제, 잘못된 상태 전이)
- Repository의 다양한 쿼리 메서드 검증

## 향후 개선 사항

### 1. 인증/인가 (최우선 과제)
- JWT 기반 인증 구현
- 본인 확인 로직 및 역할 기반 접근 제어
- **상세 내용**: 설계 결정사항 8번 참조

### 2. 실제 결제 연동
- PG사 연동 및 웹훅 처리
- 외부 API 호출 및 트랜잭션 관리

### 3. 이벤트 기반 아키텍처
- 결제 상태 변경 이벤트 발행
- 비동기 처리 (이메일, 알림 등)

### 4. 할인 정책 확장
- 쿠폰 시스템
- 할인 정책 조합 및 우선순위

### 5. 모니터링 및 운영
- 헬스체크 및 메트릭
- 로깅 개선 및 추적

### 6. 데이터베이스 최적화
- 인덱스 및 쿼리 튜닝
- 프로덕션 DB 전환

## 라이선스

이 프로젝트는 과제 목적으로 제작되었습니다.