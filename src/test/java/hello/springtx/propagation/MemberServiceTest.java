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

        // then : 모든 데이터 정상 저장
        assertThat(memberRepository.find(username)).isPresent();
        assertThat(logRepository.find(username)).isEmpty();

        // 서비스 계층에 트랜젝션이 없을때 - 회원 커밋, 로그 예외발생 롤백
        // memberRepository, logRepository 가 각각 트랜젝션을 시작
        // memberRepository : 커밋 , logRepository : 런타임예외 롤백
        // 서로 다른 트랜젝션이므로 각각 동작한다. (커넥션도 다르다)
        // 이경우, 데이터 정합성 문제가 발생할 수 있으므로 하나의 트랜젝션으로 묶을 필요가 있다.
    }
}
// JPA 와 데이터 변경
// JPA 를 통한 모든 데이터 변경 (등록, 수정, 삭제) 에는 트랜젝션이 필요하다.
// 조회는 트랜젝션 없이 가능하다.