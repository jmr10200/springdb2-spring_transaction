package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@SpringBootTest
public class InternalCallV1Test {

    @Autowired
    CallService callService;

    @Test
    void printProxy() {
        log.info("callService class={}", callService.getClass());
        // @Transactional 이 하나라도 있으면 트랜젝션 프록시 객체가 생성된다.
        // 실행 결과 -> CGLIB 이 붙어 생성된 것을 확인할 수 있다. (원본 객체 대신 트랜젝션 프록시 객체 주입 받은 것)
        // callService class=class hello.springtx.apply.InternalCallV1Test$CallService$$EnhancerBySpringCGLIB$$79cd24f4

        // 트랜젝션 선언 X: callService class=class hello.springtx.apply.InternalCallV1Test$CallService$$
    }

    /**
     * @Transactional 선언 O internal() 호출
     */
    @Test
    void internalCall() {
        callService.internal();
        // 여기서 callService 는 트랜젝션 프록시
        // @Transactional 선언 O : 트랜젝션 프록시 적용 후, 실제 internal() 을 호출
        // 실제 callService 에서 처리완료 되면 응답이 트랜젝션 프록시로 돌아오고, 트랜젝션 프록시가 트랜젝션 완료
    }

    /**
     * @Transactional 선언 X external() 호출
     */
    @Test
    void externalCall() {
        callService.external();
    }


    @TestConfiguration
    static class InternalCallV1Config {
        @Bean
        CallService callService() {
            return new CallService();
        }
    }

    @Slf4j
    static class CallService {

        /**
         * @Transacational 선언 X
         */
        public void external() {
            log.info("call external");
            printTxInfo();
            internal();
        }

        /**
         * @Transactional 선언 O
         */
        @Transactional
        public void internal() {
            log.info("call internal");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }

    }
}
/* CallService */
// external() : @Transactional 선언 X
// internal() : @Transactional 선언 O
// @Transactional 이 하나라도 있으면 트랜젝션 프록시 객체가 생성되고
// callService 빈을 주입받으면 트랜젝션 프록시 객체가 주입된다.

// internal() 실행로그 분석
// o.s.t.i.TransactionInterceptor       : Getting transaction for [hello.springtx.apply.InternalCallV1Test$CallService.internal]
// h.s.a.InternalCallV1Test$CallService : call internal
// h.s.a.InternalCallV1Test$CallService : tx active=true
// o.s.t.i.TransactionInterceptor       : Completing transaction for [hello.springtx.apply.InternalCallV1Test$CallService.internal]
// 트랜젝션 획득 Getting (트랜젝션 프록시 객체 : TransactionInterceptor)
// -> 실제 CallService 처리 (트랜젝션 프록시 객체가 호출한 실제 객체 : CallService)
// -> 트랜젝션 종료 Completing (트랜젝션 프록시 객체 : TransactionInterceptor)

// external() 실행로그 분석
// h.s.a.InternalCallV1Test$CallService     : call external
// h.s.a.InternalCallV1Test$CallService     : tx active=false
// h.s.a.InternalCallV1Test$CallService     : call internal
// h.s.a.InternalCallV1Test$CallService     : tx active=false
// @Transactional 선언 X 이므로 트랜젝션 없이 실행 -> 실제 객체 CallService 만 동작
// 내부에서 트랜젝션이 선언되어있는 internal() 를 호출하고있는데, 적용 X 이다.

/* 프록시와 내부 호출 */
// 1. callService.external() 을 호출 (callService 는 트랜젝션 프록시)
// 2. callService 의 트랜젝션 프록시가 호출 됨
// 3. external() 에는 @Transactional 선언 X : 트랜젝션 프록시는 트랜젝션 적용 X
// 4. 실제 callService 객체 인스턴스의 external() 을 호출
// 5. external() 은 내부에서 internal() 메소드를 호출

// 문제 원인
// java 에서 메소드 앞에 별도의 참조가 없으면 this 라는 의미로 자기자신의 인스턴스를 가리킨다.
// 즉, 자기자신의 내부 메소드를 호출하는 this.internal() 이 되는데,
// 여기서 this(자기자신)은 실제 대상 객체(target) 의 인스턴스를 의미한다.
// 결과적으로 이러한 내부호출은 프록시를 거치지 않는다. = 트랜젝션이 적용되지 않는다.
// target 에 있는 internal() 을 직접 호출한 것이기 때문이다.
// 이를 해결하기 위한 방법은?
// 가장 단순한 방법은 internal() 메소드를 별도의 클래스로 분리하는 것이다. : InternalCallV2Test 참고