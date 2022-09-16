package hello.springtx.propagation;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
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

    /**
     * 외부 트랜젝션 : 커밋
     * 내부 트랜젝션 : 커밋
     */
    @Test
    void inner_commit() {
        log.info("외부 트랜젝션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜젝션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        log.info("내부 트랜젝션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜젝션 커밋");
        txManager.commit(outer);

    }

    /**
     * 외부 트랜젝션 : 롤백
     * 내부 트랜젝션 : 커밋
     */
    @Test
    void outer_rollback() {
        log.info("외부 트랜젝션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜젝션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        log.info("내부 트랜젝션 커밋");
        txManager.commit(inner);

        log.info("외부 트랜젝션 롤백");
        txManager.rollback(outer);
        // 외부 트랜젝션이 롤백되면서 전체가 롤백된다.
    }

    /**
     * 외부 트랜젝션 : 커밋
     * 내부 트랜젝션 : 롤백
     */
    @Test
    void inner_rollback() {
        log.info("외부 트랜젝션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜젝션 시작");
        TransactionStatus inner = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());
        log.info("내부 트랜젝션 롤백");
        txManager.rollback(inner);
        // Participating transaction failed - marking existing transaction as rollback-only
        // 내부 트랜젝션 롤백하면, 트랜젝션에 롤백전용(rollback-only) 이 체크된다.

        log.info("외부 트랜젝션 커밋");
        txManager.commit(outer);
        // Global transaction is marked as rollback-only
        // 커밋을 호출했지만 롤백전용(rollback-only) 표시가되어 있어 물리 트랜젝션을 롤백한다.
        // 롤백후 UnexpectedRollbackException 예외를 던진다.
    }

    /**
     * 트랜젝션 전파 옵션 : EQUIRES_NEW
     */
    @Test
    void inner_rollback_requires_new() {
        log.info("외부 트랜젝션 시작");
        TransactionStatus outer = txManager.getTransaction(new DefaultTransactionAttribute());
        log.info("outer.isNewTransaction()={}", outer.isNewTransaction());

        log.info("내부 트랜젝션 시작");
        DefaultTransactionAttribute definition = new DefaultTransactionAttribute();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus inner = txManager.getTransaction(definition);
        log.info("inner.isNewTransaction()={}", inner.isNewTransaction());

        log.info("내부 트랜젝션 롤백");
        txManager.rollback(inner);

        log.info("외부 트랜젝션 커밋");
        txManager.commit(outer);
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

/* inner_commit() 분석 */
// 외부 트랜젝션 수행중, 내부 트랜젝션을 수행한다
// 외부 트랜젝션은 처음 수행되므로 신규 트랜젝션 이다. (outer.isNewTransaction()=true)
// 내부 트랜젝션은 이미 외부 트랜젝션이 수행중이므로 false 이다.(inner.isNewTransaction()=false)
// 즉 내부 트랜젝션이 외부 트랜젝션에 참여 했다. = 외부트랜젝션을 그대로 이어 사용한다.
// 트랜젝션 참여 : 외부 트랜젝션과 내부 트랜젝션이 하나의 물리 트랜젝션으로 묶이는 것이다.

// 실행결과 : 로그분석
// 외부 트랜젝션 시작
// Creating new transaction with name [null]: PROPAGATION_REQUIRED,ISOLATION_DEFAULT
// Acquired Connection [HikariProxyConnection@209516740 wrapping conn0: url=jdbc:h2:mem:5b615e4a-d952-4c7e-aee8-6f69882a27aa user=SA] for JDBC transaction
// Switching JDBC Connection [HikariProxyConnection@209516740 wrapping conn0: url=jdbc:h2:mem:5b615e4a-d952-4c7e-aee8-6f69882a27aa user=SA] to manual commit
//    -> "물리 트랜젝션 시작 (manual commit)"
// outer.isNewTransaction()=true
// 내부 트랜젝션 시작
// Participating in existing transaction -> "존재하는 트랜젝션에 참여한다"
// inner.isNewTransaction()=false
// 내부 트랜젝션 커밋
// 외부 트랜젝션 커밋
// Initiating transaction commit
// Committing JDBC transaction on Connection [HikariProxyConnection@209516740 wrapping conn0: url=jdbc:h2:mem:5b615e4a-d952-4c7e-aee8-6f69882a27aa user=SA]
//     -> "물리 트랜젝션의 커밋"
// Releasing JDBC Connection [HikariProxyConnection@209516740 wrapping conn0: url=jdbc:h2:mem:5b615e4a-d952-4c7e-aee8-6f69882a27aa user=SA] after transaction

// 정리
// 스프링은 이렇게 여러 트랜젝션이 함께 사용되는 경우,
// 처음 트랜젝션을 시작한 외부 트랜젝션이 실제 물리트랜젝션을 관리하도록 한다.
// 이를 통해 트랜젝션 중복 커밋 문제를 해결한다.

/* inner_rollback_requires_new() 분석 */
// 트랜젝션 전파 옵션 : TransactionDefinition.PROPAGATION_REQUIRES_NEW
// 내부 트랜젝션을 시작할 때 기존 트랜젝션에 참여하는 것이 아니라 새로운 물리 트랜젝션을 만들어 시작한다.
// REQUIRES_NEW 옵션을 사용하면 물리 트랜젝션이 명확히 분리된다.
// REQUIRES_NEW 옵션은 DB 커넥션이 동시에 2개 생성되는 점을 유의해야 한다.

// 로그분석
// 외부 트랜젝션 시작 : conn0 획득 (Acquired Connection), manual commit 으로 변경해서 물리 트랜젝션 시작
// 신규 이므로 : outer.isNewTransaction()=true
// 내부 트랜젝션 시작 : conn1 획득 (Acquired Connection), manual commit 으로 변경해서 물리 트랜젝션 시작
// 참여 아닌 신규이므로 : inner.isNewTransaction()=true
// 내부 트랜젝션 롤백 : Rolling back (conn1) 물리 롤백 실행
// 외부 트랜젝션 커밋 : Committing (conn0) 물리 커밋 실행