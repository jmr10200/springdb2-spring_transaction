package hello.springtx.apply;

import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@SpringBootTest
public class RollbackTest {

    @Autowired
    RollbackService service;

    @Test
    void runtimeException() {
        assertThatThrownBy(() -> service.runtimeException())
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void checkedException() {
        assertThatThrownBy(() -> service.checkedException())
                .isInstanceOf(MyException.class);
    }

    @Test
    void rollbackFor() {
        assertThatThrownBy(() -> service.rollbackFor())
                .isInstanceOf(MyException.class);
    }

    @TestConfiguration
    static class RollbackTestConfig {
        @Bean
        RollbackService rollbackService() {
            return new RollbackService();
        }
    }

    static class RollbackService {

        // 런타임 예외 발생 : rollback
        @Transactional
        public void runtimeException() {
            log.info("call runtimeException");
            throw new RuntimeException();
        }

        // 체크 예외 발생 : commit
        @Transactional
        public void checkedException() throws MyException {
            log.info("call checkedException");
            throw new MyException();
        }

        // 체크 예외 rollbackFor 지정 : rollback
        @Transactional(rollbackFor = MyException.class)
        public void rollbackFor() throws MyException {
            log.info("call rollbackFor");
            throw new MyException();
        }

    }

    static class MyException extends Exception {}
}

/* 예외와 트랜젝션 커밋, 롤백 - 기본 */
// 예외가 발생했을때 내부에서 예외를 처리하지 못하고, 트랜젝션 범위(@Transactional 적용된 AOP) 밖으로 예외를 던지면?
// 예외 발생시 스프링 트랜젝션 AOP 는 예외의 종류에 따라 트랜젝션을 커밋하거나 롤백한다
// ・언체크 예외인 RuntimeException, Error 와 그 하위 예외가 발생하면 트랜젝션을 롤백한다.
// ・체크 예외인 Exception 과 그 하위 예외가 발생하면 트랜젝션을 커밋한다.
// ・정상 응답(리턴)하면 트랜젝션을 커밋한다.