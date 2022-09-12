package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;

@Slf4j
@SpringBootTest
public class InitTxTest {

    @Autowired
    Hello hello;

    @Test
    void go() {
        // 초기화 코드는 스프링 초기화 시점에 호출한다.
    }

    @TestConfiguration
    static class InitTxTestConfig {

        @Bean
        Hello hello() {
            return new Hello();
        }
    }

    static class Hello {

        @PostConstruct // 의존성 주입 후 초기화 수행, 다른 리소스에서 호출되지 않아도 수행됨
        @Transactional
        public void initV1() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init @PostConstruct tx active={}", isActive);
            // 결과로그 : 트랜젝션 적용 X
            // hello.springtx.apply.InitTxTest          : Hello init @PostConstruct tx active=false
            // 초기화 코드가 먼저 호출되고, 그다음에 트랜젝션 AOP 가 적용되기 때문에 초기화 시점에서는 트랜젝션 획득할 수 없다.
        }

        @EventListener(value = ApplicationReadyEvent.class)
        @Transactional
        public void init2() {
            boolean isActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("Hello init ApplicationReadyEvent tx active={}", isActive);
            // 결과로그 : 트랜젝션 적용 O
            // o.s.t.i.TransactionInterceptor           : Getting transaction for [hello.springtx.apply.InitTxTest$Hello.init2]
            // hello.springtx.apply.InitTxTest          : Hello init ApplicationReadyEvent tx active=true
            // o.s.t.i.TransactionInterceptor           : Completing transaction for [hello.springtx.apply.InitTxTest$Hello.init2]
            // 가장 확실한 대안 : ApplicationReadyEvent
            // 이 이벤트는 트랜젝션 AOP 를 포함한 스프링이 컨테이너가 완전히 생성되고 난 후 이벤트가 붙은 메소드를 호출한다.
            // 따라서 init2() 에서는 트랜젝션이 적용된 것을 확인할 수 있다.
        }
    }

}
// 트랜젝션 AOP 주의 사항 - 초기화 시점
// 스프링 라이프사이클로 인해 스프링의 초기화 시점에는 트랜젝션 AOP 가 적용되지 않을 수 있다.

// @PostConstruct 와 @Transactional 의 설정
// @PostConstruct 는 해당 빈 자체만 생성되었다고 가정하고 호출한다.
// 초기화 코드가 먼저 호출되고, 그다음에 트랜젝션 AOP 가 적용되기 때문에 초기화 시점에서는 트랜젝션을 획득할 수 없다.