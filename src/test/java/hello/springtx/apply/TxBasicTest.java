package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@SpringBootTest
public class TxBasicTest {

    @Autowired
    BasicService basicService;

    @TestConfiguration
    static class TxApplyBasicConfig {
        @Bean
        BasicService basicService() {
            return new BasicService();
        }
    }

    @Test
    void txTest() {
        basicService.tx();
        // 프록시의 tx() 를 호출하고, 프록시가 트랜젝션 여부를 확인한다.
        // 트랜젝션 적용 O -> 실제 basicService.tx() 호출
        // 실제 basicService.tx() 의 호출이 끝나서 프록시로 제어가 돌아오면(리턴)
        // 프록시는 트랜젝션 로직을 커밋하거나 롤백해서 트랜젝션을 종료한다.
        basicService.nonTx();
        // 프록시의 nonTx() 를 호출하고, 프록시가 트랜젝션 여부를 확인한다.
        // 트랜젝션 적용 X -> 트랜젝션 시작 X, basicService.nonTx() 호출 후 종료
    }

    @Test
    void proxyCheck() {
        // BasicService$$EnhancerBySpringCGLIB...
        log.info("app class={}", basicService.getClass());
        assertThat(AopUtils.isAopProxy(basicService)).isTrue();
    }

    @Slf4j
    static class BasicService {

        @Transactional
        public void tx() {
            log.info("call tx");
            // 현재 쓰레드에 트랜젝션이 적용되어 있는지 확인하는 기능
            // true = 적용 O / false = 적용 X
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
        }

        public void nonTx() {
            log.info("call nonTx");
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("nonTx active={}", txActive);
        }
    }
}
/* proxyCheck() 실행 */
// AopUtils.isAopProxy() : 선언적 트랜젝션 방식에서 스프링 트랜젝션은 AOP 기반으로 동작한다.
// @Transactional 을 메소드나 클래스에 붙이면 해당 객체는 트랜젝션 AOP 적용의 대상이 되고,
// 결과적으로 실제 객체 대신에 트랜젝션을 처리해주는 프록시 객체가 스프링 빈에 등록된다.
// 그리고 주입을 받을 때도 실제 객체 대신에 프록시 객체가 주입된다.

// 클래스 이름을 출력해보면 basicService$$EnhancerBySpringCGLIB... 라고 프록시 클래스의 이름이 출력되는 것을 확인할 수 있다.
// proxyCheck() 실행결과
// TxBasicTest$BasicService$$EnhancerBySpringCGLIB$$...

/* 스프링 컨테이터에 트랜젝션 프록시 등록 */
// @Transactional 어노테이션이 특정 클래스나 메소드에 하나라도 있으면 트랜젝션 AOP 는 프록시를 만들어서 스프링 컨테이너에 등록한다.
// 그리고 실제 basicService 객체 대신에 프록시인 basicService$$CGLIB 를 스프링 빈에 등록하고 프록시는 실제 basicService 를 참조하게 된다.
// -> 실제 객체 대신에 프록시가 스프링 컨테이너에 등록된다.

// 클라이언트인 txBasicTest 는 스프링 컨테이너에
// @Autowired BasicService basicService 으로 의존관계 주입을 요청한다.
// 스프링 컨테이너는 실제 객체 대신 프록시가 스프링 빈으로 등록되어 있기 때문에 프록시를 주입한다.

// 프록시는 BasicService 를 상속해서 만들어지기 때문에 다형성을 활용할 수 있다.
// 따라서 BasicService 대신에 프록시인 BasicService$$CGLIB 를 주입할 수 있다.

/* 트랜젝션 프록시 동작 방식 */
// 클라이언트가 주입 받은 basicService$$CGLIB 는 트랜젝션을 적용하는 프록시이다.