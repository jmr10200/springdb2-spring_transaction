package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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