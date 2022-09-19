package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.UnexpectedRollbackException;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
class MemberServiceTest {

    @Autowired
    MemberService memberService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    LogRepository logRepository;

    /**
     * MemberService     @Transactional : OFF
     * MemberRepository  @Transactional : ON
     * LogRepository     @Transactional : ON
     */
    @Test
    void outerTxOff_success() {
        // given
        String username = "outerTxOff_success";

        // when
        memberService.joinV1(username);

        // then : 모든 데이터 정상 저장
        assertThat(memberRepository.find(username)).isPresent();
        assertThat(logRepository.find(username)).isPresent();

        // 서비스 계층에 트랜젝션이 없을때 - 회원, 로그 리포지토리 둘다 커밋성공
        // memberRepository, logRepository 가 각각 트랜젝션을 시작하고 둘다 정상 커밋한다.
        // 서로 다른 트랜젝션이다. (커넥션도 다르다)
    }

    /**
     * MemberService     @Transactional : OFF
     * MemberRepository  @Transactional : ON
     * LogRepository     @Transactional : ON Exception
     */
    @Test
    void outerTxOff_fail() {
        // given
        String username = "로그예외_outerTxOff_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        // then
        assertThat(memberRepository.find(username)).isPresent();
        assertThat(logRepository.find(username)).isEmpty();

        // 서비스 계층에 트랜젝션이 없을때 - 회원 커밋, 로그 예외발생 롤백
        // memberRepository, logRepository 가 각각 트랜젝션을 시작
        // memberRepository : 커밋 , logRepository : 런타임예외 롤백
        // 서로 다른 트랜젝션이므로 각각 동작한다. (커넥션도 다르다)
        // 이경우, 데이터 정합성 문제가 발생할 수 있으므로 하나의 트랜젝션으로 묶을 필요가 있다.
    }

    /**
     * MemberService     @Transactional : ON
     * MemberRepository  @Transactional : OFF
     * LogRepository     @Transactional : OFF
     */
    @Test
    void singleTx() {
        // given
        String username = "singleTx";

        // when
        memberService.joinV1(username);

        // then : 모든 데이터 정상 저장
        assertThat(memberRepository.find(username)).isPresent();
        assertThat(logRepository.find(username)).isPresent();

        // 서비스 계층에 트랜젝션을 선언
        // MemberService 가 시작할때부터 종료할 때까지 하나의 트랜젝션으로 관리
        // memberRepository , logRepository : 같은 트랜젝션 사용
        // MemberService 만 트랜젝션을 처리하므로
        // 논리 트랜젝션, 물리 트랜젝션, 외부 트랜젝션, 내부 트랜젝션, readOnly, 신규 트랜젝션, 트랜젝션 전파 등 신경 X
    }

    /**
     * 트랜젝션 전파 : 모든 논리 트랜젝션 성공 -> 물리 트랜젝션 커밋
     * MemberService     @Transactional : ON
     * MemberRepository  @Transactional : ON
     * LogRepository     @Transactional : ON
     */
    @Test
    void outerTxOn_success() {
        // given
        String username = "outerTxOn_success";

        // when
        memberService.joinV1(username);

        // then : 모든 데이터 정상 저장
        assertThat(memberRepository.find(username)).isPresent();
        assertThat(logRepository.find(username)).isPresent();

        // client A (outerTxOn_success()메소드) 가 MemberService 호출하면서 트랜젝션 AOP 호출
        // -> 신규 트랜젝션 생성, 물리 트랜젝션 시작
        // MemberRepository 호출 : 기존 트랜젝션에 참여 -> 트랜젝션 AOP 호출 -> 정상응답 -> txManager 에 커밋 요청 (신규 트랜젝션 아니므로 실제 커밋은 x)
        // LogRepository 호출 : 기존 트랜젝션 참여 -> 트랜젝션 AOP 호출 -> 정상응답 -> txManager 에 커밋요청 (신규 트랜젝션 아니므로 실제 커밋은 x)
        // MemberService 로직 호출 끝나고 트랜젝션 AOP 호출 -> 정상응답 -> txManager 에 커밋요청 -> 신규 트랜젝션 이므로 물리 커밋
    }

    /**
     * 트랜젝션 전파 : 로그 논리 트랜젝션 실패 -> 물리 트랜젝션 롤백
     * MemberService     @Transactional : ON
     * MemberRepository  @Transactional : ON
     * LogRepository     @Transactional : ON Exception
     */
    @Test
    void outerTxOn_fail() {
        // given
        String username = "로그예외_outerTxOn_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV1(username))
                .isInstanceOf(RuntimeException.class);

        // then : 모든 데이터 롤백
        assertThat(memberRepository.find(username)).isEmpty();
        assertThat(logRepository.find(username)).isEmpty();

        // client A (outerTxOn_success()메소드) 가 MemberService 호출하면서 트랜젝션 AOP 호출
        // -> 신규 트랜젝션 생성, 물리 트랜젝션 시작
        // MemberRepository 호출 : 기존 트랜젝션에 참여 -> 트랜젝션 AOP 호출 -> 정상응답 -> txManager 에 커밋 요청 (신규 트랜젝션 아니므로 실제 커밋은 x)
        // LogRepository 호출 : 기존 트랜젝션 참여 -> 트랜젝션 AOP 호출 -> 예외발생 -> txManager 에 롤백요청 (신규 트랜젝션 아니므로 실제 롤백 x : rollbackOnly 설정)
        //  -> 예외 던짐, 트랜젝션 AOP 도 그대로 예외를 밖으로 던짐
        // MemberService 런타임 예외를 받게 됨 -> txManager 에 롤백 요청 -> 신규 트랜젝션 이므로 물리 롤백 호출
        //  (참고 : 이경우 예외가 던져져 어차피 롤백되었기 때문에 rollbackOnly 설정 참고 안하고 롤백)
        // 이렇게 함으로서 회원과 로그를 하나의 트랜젝션으로 묶어 하나라도 롤백시 전체 롤백되므로
        // 데이터 정합성에 문제가 발생하지 않는다.
    }

    /**
     * 회원가입을 시도한 로그를 남기는데 실패하더라도 회원가입은 유지해야된다.
     * 요구사항 실패 예제 : 롤백되고 UnexpectedRollbackException 발생
     * MemberService     @Transactional : ON
     * MemberRepository  @Transactional : ON
     * LogRepository     @Transactional : ON Exception
     */
    @Test
    void recoverException_fail() {
        // given
        String username = "로그예외_recoverException_fail";

        // when
        assertThatThrownBy(() -> memberService.joinV2(username))
                .isInstanceOf(UnexpectedRollbackException.class);

        // then : 모든 데이터 롤백
        assertThat(memberRepository.find(username)).isEmpty();
        assertThat(logRepository.find(username)).isEmpty();

        // 내부 트랜젝션에서 rollbackOnly 를 설정하므로 결과적으로 물리트랜젝션은 롤백한다.
        // UnexpectedRollbackException 가 던져진다.

        // 정리하면,
        // 논리 트랜젝션 중 하나라도 롤백되면 전체 트랜젝션 롤백 된다.
        // 내부 트랜젝션이 롤백 되었는데, 외부 트랜젝션이 커밋되면 UnexpectedRollbackException 발생한다.
        // rollbackOnly 상황에서 커밋이 발생하면 UnexpectedRollbackException 발생한다.
    }

    /**
     * 회원가입을 시도한 로그를 남기는데 실패하더라도 회원가입은 유지해야된다.
     * 요구사항 OK
     * MemberService     @Transactional : ON
     * MemberRepository  @Transactional : ON
     * LogRepository     @Transactional(REQUIRES_NEW) : 신규 트랜젝션 OPTION 설정
     */
    @Test
    void recoverException_success() {
        // given
        String username = "로그예외_recoverException_success";

        // when
        memberService.joinV2(username);

        // then : member 저장, log 롤백
        assertThat(memberRepository.find(username)).isPresent();
        assertThat(logRepository.find(username)).isEmpty();

        // 물리 트랜젝션 1 : MemberService, MemberRepository
        // 물리 트랜젝션 2 : LogRepository (REQUIRES_NEW 옵션으로 새 트랜젝션)
        // 회원 저장 OK 커밋, 로그 실패 롤백

        // 정리
        // REQUIRES_NEW 를 사용하면 물리 트랜젝션 자체가 완전히 분리된다.
        // 항상 신규로 트랜젝션을 생성하는 옵션이기 때문이다.
        // 신규 트랜젝션이므로 rollbackOnly 표시가 되지 않는다.
    }
}
// JPA 와 데이터 변경
// JPA 를 통한 모든 데이터 변경 (등록, 수정, 삭제) 에는 트랜젝션이 필요하다.
// 조회는 트랜젝션 없이 가능하다.

/* singleTx() 추가 */
// MemberService 가 MemberRepository, LogRepository 를 하나의 트랜젝션으로 관리해서 편해진다.
// 그런데, 각각의 트랜젝션이 필요한 상황이라면?
// client A : MemberService, MemberRepository, LogRepository 를 하나의 트랜젝션으로 관리
// client B : MemberRepository 만 호출하고 여기서만 트랜젝션 사용
// client C : LogRepository 만 호출하고 여기서만 트랜젝션 사용
// -> client A 는 MemberService 에 트랜젝션을 선언해서 관리하면 될 듯 하다.
//    그런데, 이렇게하면 client B, client C 의 요구조건을 적용할 수 없다.
//    이러한 문제를 해결하기 위해 트랜젝션 전파(propagation) 이 필요한 것이다.

/* recoverException_success() 추가 */
// 논리 트랜젝션은 하나라도 롤백되면 관련된 물리 트랜젝션 전체가 롤백된다.
// 이때문에 로그 저장이 실패해도 회원 저장을 한다는 요구사항에 부응하기 위해서는 트랜젝션을 분리해야한다.
// 이때 사용하는 옵션이 REQUIRES_NEW 이다.

// 주의
// REQUIRES_NEW 를 사용하면 하나의 HTTP 요청에 동시에 2개의 데이터베이스 커넥션을 사용하게 된다.
// 따라서 성능이 중요한 곳에서는 이를 주의해서 사용해야 한다.
// REQUIRES_NEW 를 사용하지 않고 문제를 해결할 수 있는 단순한 방법이 있다면 그 방법을 택하는 것이 좋다.

// 예를들면, 아래처럼 구조를 변경하는 것이다.
// 클라이언트 호출 -> MemberFacade ->
// 분기1 : -> 물리 트랜젝션1 : MemberService, MemberRepository
// 분기2 : -> 물리 트랜젝션2 : LogRepository
// 이렇게 하면 HTTP 요청에 동시에 2개 커넥션을 사용하지 않는다. 순차적으로 사용후 반환한다.
// 구조상으로 Facade 가 생기므로 REQUIRES_NEW 가 깔끔한 경우도 있으므로 장단점을 이해하고 적절한 선택이 요구된다.
