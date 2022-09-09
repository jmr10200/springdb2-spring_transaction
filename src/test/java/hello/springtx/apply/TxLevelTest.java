package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * @Transactional 우선순위
 * 더 구체적이고 자세한 것이 높은 우선순위를 갖는다.
 */
@SpringBootTest
public class TxLevelTest {

    @Autowired
    LevelService levelService;

    @Test
    void orderTest() {
        levelService.write();
        levelService.read();
    }

    @TestConfiguration
    static class TxApplyLevelConfig {

        @Bean
        LevelService levelService() {
            return new LevelService();
        }
    }


    @Slf4j
    @Transactional(readOnly = true)
    static class LevelService {

        @Transactional(readOnly = false)
        public void write() {
            log.info("call write");
            printTxInfo();
        }

        public void read() {
            log.info("call read");
            printTxInfo();
        }

        private void printTxInfo() {
            boolean txActive = TransactionSynchronizationManager.isActualTransactionActive();
            log.info("tx active={}", txActive);
            boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
            log.info("tx readOnly={}", readOnly);
        }

    }
}
/* @Transactional 2가지 규칙 */
// 1. 우선순위 규칙
// LevelService : @Transactional(readOnly = true)
// write() : @Transactional(readOnly = false)
// 클래스보다 메소드가 구체적이므로 메소드에 설정한 readOnly = false 가 우선적용된다.

// 2. 클래스에 적용하면 메소드는 자동 적용
// LevelService : @Transactional(readOnly = true)
// read() : 설정 x
// 클래스에 설정된 @Transactional(readOnly = ture) 가 적용된다.

// 참고
// @Transactional 의 default 값 : readOnly = false
// readOnly 값 확인 : TransactionSynchronizationManager.isCurrentTransactionReadOnly();

/* 인터페이스에 @Transactional 적용 */
// 다음 순서로 적용된다.
// 1. 클래스의 메소드 (가장 높은 우선순위)
// 2. 클래스의 타입
// 3. 인터페이스의 메소드
// 4. 인터페이스의 타입 (가장 낮은 우선순위)
// 그런데, 인터페이스에 @Transactional 을 사용하는 것은 스프링 공식 매뉴얼에서 권장하지 않는다.
// AOP 를 적용하는 방식에 따라서 인터페이스에 어노테이션을 두면 AOP 가 적용되지 않는 경우가 있기 때문이다.
// 가급적 구체적인 클래스에 @Transactional 을 사용하자.

// 참고
// 스프링은 인터페이스에 @Transactional 을 사용하는 방식을 스프링 5.0에서 많은 부분 개선했다.
// 과거에는 구체 클래스를 기반으로 프록시를 생성하는 CGLIB 방식을 사용하면 인터페이스에 있는 @Transactional 을 인식하지 못했다.
// 스프링 5.0 부터는 이 부분을 개선해서 인터페이스에 설정한 것도 인식한다.
// 하지만, 다른 AOP 방식에서 또 적용되지 않을 수 있어 공식 매뉴얼의 가이드가 권장하듯 구체클래스에 사용하자.