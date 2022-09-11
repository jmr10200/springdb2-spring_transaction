package hello.springtx.apply;

import lombok.RequiredArgsConstructor;
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
public class InternalCallV2Test {


    @Autowired
    CallService callService;

    @Test
    void externalCallV2() {
        callService.external();
    }

    @TestConfiguration
    static class InternalCallV2Config {
        @Bean
        CallService helloService() {
            return new CallService(innerService());
        }

        @Bean
        InternalService innerService() {
            return new InternalService();
        }
    }

    static class InternalService {

        /**
         * @Transactional 선언 O internal() 호출
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

    @RequiredArgsConstructor
    static class CallService {

        private final InternalService internalService;

        /**
         * @Transactional 선언 X external() 호출
         */
        public void external() {
            log.info("call external");
            printTxInfo();
            // Point : @Transactional 을 선언한 internal() 을 외부 클래스로 옮겼다.
            internalService.internal();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }
    }
}
// 호출 흐름 분석
// 1. 클라이언트인 테스트 코드는 callService.external() 을 호출한다.
// 2. callService 는 실제 callService 객체 인스턴스이다. (@Transactional 없으므로 프록시 객체 생성 X)
// 3. callService 는 주입받은 internalService.internal() 을 호출한다.
// 4. internalService 는 @Transactional 이 선언되어있으므로 트랜젝션 프록시이다.
// 5. 트랜젝션 적용 후 실제 internalService 객체 인스턴스의 internal() 을 호출한다.

// 실행 로그 분석
// 1. 실제 객체 인스턴스 callService 통해 외부 external() 호출
// hello.springtx.apply.InternalCallV2Test  : call external
// 2. 실제 객체 이므로 프록시 객체 생성 X
// hello.springtx.apply.InternalCallV2Test  : tx active=false
// 3. 내부 호출 대상이되는 internal() 은 클래스를 외부로 분리했으므로 트랜젝션 프록시 동작 확인
// o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.InternalCallV2Test$InternalService.internal]
// hello.springtx.apply.InternalCallV2Test  : call internal
// hello.springtx.apply.InternalCallV2Test  : tx active=true
// o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springtx.apply.InternalCallV2Test$InternalService.internal]

// -> 실무적으로도 이렇게 별도의 클래스를 분리하는 방식을 주로 사용한다.

/* public 메소드만 트랜젝션 적용 */
// 스프링의 트랜젝션 AOP 기능은 public 메소드만 트랜젝션이 적용되도록 기본설정 되어있다.
// 그래서 protected, private, package-visible 은 트랜젝션이 적용되지 않는다.
// 생각해보면 protected, package-visible 도 외부에서 호출은 가능하지만 스프링이 적용되지 않도록 해두었다.
// 트랜젝션을 의도하지 않는 곳 까지 과도하게 적용될 가능성이 있기 때문이다.
// 트랜젝션은 주로 비즈니스 로직의 시작점에 걸기 때문에, 대부분 외부에 열어준 곳을 시작점으로 하기 때문이다.
// public 이 아닌 곳에 @Transactional 을 선언하면 트랜젝션만 무시된다. 예외가 발생하지 않는다.