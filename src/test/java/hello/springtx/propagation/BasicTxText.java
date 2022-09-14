package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.interceptor.DefaultTransactionAttribute;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;

@Slf4j
@SpringBootTest
public class BasicTxText {

    @Autowired
    PlatformTransactionManager txManager;

    @TestConfiguration
    static class Config {
        @Bean
        public PlatformTransactionManager transactionManager(DataSource dataSource) {
            // DataSourceTransactionManager 를 스프링 빈으로 등록
            return new DataSourceTransactionManager(dataSource);
        }
    }

    @Test
    void commit() {
        log.info("트랜젝션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜젝션 커밋 시작");
        txManager.commit(status);
        log.info("트랜젝션 커밋 완료");
    }

    @Test
    void rollback() {
        log.info("트랜젝션 시작");
        TransactionStatus status = txManager.getTransaction(new DefaultTransactionAttribute());

        log.info("트랜젝션 롤백 시작");
        txManager.rollback(status);
        log.info("트랜젝션 롤백 완료");
    }

    @Test
    void double_commit() {
        log.info("트랜젝션 1 시작");
        TransactionStatus status1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜젝션 1 커밋");
        txManager.commit(status1);

        log.info("트랜젝션 2 시작");
        TransactionStatus status2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜젝션 2 커밋");
        txManager.commit(status2);
    }

    @Test
    void double_commit_rollback() {
        log.info("트랜젝션 1 시작");
        TransactionStatus status1 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜젝션 1 커밋");
        txManager.commit(status1);

        log.info("트랜젝션 2 시작");
        TransactionStatus status2 = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("트랜젝션 2 롤백");
        txManager.rollback(status2);
    }
}
/* double_commit() 분석 */
// 실행결과, 트랜젝션 1 과 트랜젝션 2 가 같은 conn0 커넥션을 사용중이다. connectionPool 때문이다.
// 트랜젝션 1 실행 후 커넥션 반납 -> 트랜젝션 2 실행 후 커넥션 반납 : 같은 conn0 사용하지만 엄연히 다른 커넥션을 이용했다.(재사용한것)
// 이를 구분할 수 있는 방법은 이하 로그에서 @이하 주소가 다름으로 확인할 수 있다.
// 트랜젝션 1 : Acquired Connection [HikariProxyConnection@211207319 wrapping conn0
// 트랜젝션 2 : Acquired Connection [HikariProxyConnection@1848867745 wrapping conn0

// 트랜젝션이 각각 수행되면서 사용되는 DB 커넥션도 각각 다르다.
// 이경우, 트랜젝션을 각자 관리하므로 전체 트랜젝션을 묶을 수 없다.

/* double_commit_rollback() 분석 */
// 트랜젝션 1 이 커밋하고, 트랜젝션 2 가 롤백하는 경우, 각각 1 은 커밋, 2 는 롤백된다.
// 트랜젝션이 각자 관리되므로 각각 동작한다.
