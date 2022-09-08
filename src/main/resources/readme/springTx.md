### 스프링 트랜젝션

#### 스프링 트랜젝션 추상화
데이터 접근 기술들은 트랜젝션을 처리하는 방식에 차이가 있다.

###### JDBC 트랜젝션 코드 예시
```
public void accountTransfer(String fromId, String toId, int money) throws SQLException {
 Connection con = dataSource.getConnection();
 try {
    con.setAutoCommit(false); //트랜잭션 시작
    //비즈니스 로직
    bizLogic(con, fromId, toId, money);
    con.commit(); //성공시 커밋
 } catch (Exception e) {
    con.rollback(); //실패시 롤백
    throw new IllegalStateException(e);
 } finally {
    release(con);
 }
}
```

###### JPA 트랜젝션 코드 예시
```
public static void main(String[] args) {
 // 엔티티 매니저 팩토리 생성
 EntityManagerFactory emf = Persistence.createEntityManagerFactory("jpabook");
 EntityManager em = emf.createEntityManager(); //엔티티 매니저 생성
 EntityTransaction tx = em.getTransaction(); //트랜잭션 기능 획득
 try {
    tx.begin(); // tx start
    logic(em); // bizlogic
    tx.commit(); // tx commit
 } catch (Exception e) {
    tx.rollback(); // tx rollback
 } finally {
    em.close(); // 엔티티 매니저 종료
 }
 emf.close(); // 엔티티 매니저 팩토리 종료
}
```
즉, JDBC 기술을 사용하다가 JPA 기술로 변경하게 되면 트랜젝션을 사용하는 코드도 변경해야 한다.
<br>
스프링은 이러한 문제 해결을 위해 트랜젝션 추상화를 제공한다. 트랜젝션을 사용하는 입장에서는 스프링 트랜젝션 추상화를 통해 동일한 방식으로 사용할 수 있게 된다.
<br>
스프링은 PlatformTransactionManager 라는 인터페이스로 트랜젝션을 추상화 한다.

###### PlatformTransactionManager 인터페이스
```
package org.springframework.transaction;

public interface PlatformTransactionManager extends TransactionManager {
    TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException;
    
    void commit(TransactionStatus status) throws TransactionException;
    void rollback(TransactionStatus status) throws TransactionException;
}
```
* 트랜젝션 시작(획득), 커밋, 롤백으로 단순히 추상화 할 수 있다.
* 스프링은 트랜젝션을 추상화해서 제공할 뿐만 아니라, 실무에서 주로 사용하는 데이터 접근기술에 대한 트랜젝션 매니저의 구현체도 제공한다. 즉 필요한 구현체를 스프링 빈으로 등록하고 주입받아 사용기만 하면 된다.
* 스프링 부트는 어떤 데이터 접근 기술을 사용하는지를 자동으로 인식해서 적절한 트랜젝션 매니저를 선택해서 스프링 빈으로 등록해주기 때문에 트랜젝션 매니저를 선택하고 등록하는 과정도 생략할 수 있다.
  * JdbcTemplate, MyBatis 를 사용하면 DataSourceTransactionManager(JdbcTransactionManager) 를 스프링 빈으로 등록해줌
  * JPA 를 사용하면 JpaTransactionManager 를 스프링 빈으로 등록해줌
    * 참고
    * 스프링 5.3부터 JDBC 트랜젝션을 관리할 때 DataSourceTransactionManager 를 상속받아 약간의 기능을 확장한 JdbcTransactionManager 를 제공한다
    * 기능차이는 크지 않으므로 같은 것으로 이해하면 된다.

<br>

##### 스프링 트랜젝션 사용 방식
###### 선언적 트랜젝션 관리 (Declarative Transaction Management)
* @Transactional 어노테이션 하나만 선언해서 매우 편리하게 트랜젝션을 적용하는 것
* 과거 XML 에 설정하기도 했다.
* 이름 그대로 해당 로직에 트랜젝션을 적용하겠다 라고 어딘가에 선언하기만 하면 트랜젝션이 적용되는 방식이다.

###### 프로그래밍 방식 트랜젝션 관리 (Programmatic Transaction Management)
* 트랜젝션 매니저 또는 트랜젝션 템플릿 등을 사용해서 트랜젝션 관리 코드를 직접 작성하는 것을 프로그래밍 방식의 트랜젝션 관리라 한다.

<br>

* 프로그래밍 방식의 트랜젝션 관리를 사용하게 되면, 어플리케이션 코드가 트랜젝션이라는 기술 코드와 강하게 결합된다.
* 선언적 트랜젝션 관리가 프로그래밍 방식에 비해서 훨씬 간편하고 실용적이기 때문에 실무에서는 대부분 선언적 트랜젝션 관리를 사용한다.


#### 선언적 트랜젝션과 AOP
@Transactional 를 통한 선언적 트랜젝션 관리 방식을 사용하게 되면 기본적으로 프록시 방식의 AOP가 적용된다.
* 프록시 도입 전
  * 트랜젝션을 처리하기 위한 프록시를 도입하기 전에는 서비스의 로직에서 트랜젝션을 직접 시작한다.

***서비스 계층의 트랜젝션 사용 코드 예시***
```
//트랜잭션 시작
TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());

try {
    //비즈니스 로직
    bizLogic(fromId, toId, money);
    transactionManager.commit(status); //성공시 커밋
} catch (Exception e) {
    transactionManager.rollback(status); //실패시 롤백
    throw new IllegalStateException(e);
}
```

* 프록시 도입 후
  * 트랜젝션을 처리하기 위한 프록시를 적용하면 트랜젝션을 처리하는 객체와 비즈니스 로직을 처리하는 서비스 객체를 명확히 분리할 수 있다.

***트랜젝션 프록시 코드 예시***
```
public class TransactionProxy {
  private MemberService target;
  
  public void logic() {
    //트랜잭션 시작
    TransactionStatus status = transactionManager.getTransaction(..);
    try {
      //실제 대상 호출
      target.logic();
      transactionManager.commit(status); //성공시 커밋
    } catch (Exception e) {
      transactionManager.rollback(status); //실패시 롤백
      throw new IllegalStateException(e);
    }
 }
}
```
***트랜젝션 프록시 적용 후 서비스 코드 예시***
```
public class Service {
  public void logic() {
    //트랜잭션 관련 코드 제거, 순수 비즈니스 로직만 남음
    bizLogic(fromId, toId, money);
  }
}
```
* 프록시 도입 전
  * 서비스에 비즈니스 로직과 트랜젝션 처리 로직이 함께 섞여있다.
* 프록시 도입 후
  * 트랜젝션 프록시가 트랜젝션 처리 로직을 모두 가져가서 트랜젝션을 시작한 후에 실제 서비스를 대신 호출한다. 트랜젝션 프록시 덕분에 서비스 계층에서는 순수한 비즈니스 로직만 남길 수 있다.

##### 프록시 도입 후 전체 과정
* 트랜젝션은 커넥션에 conn.setAutocommit(false) 를 지정하면서 시작한다.
* 같은 트랜젝션을 유지하려면 같은 DB 커넥션을 사용해야 한다.
* 이를 위해 스프링 내부에서는 트랜젝션 동기화 매니저가 사용된다.
* JdbcTemplate 을 포함한 대부분의 데이터 접근 기술들은 트랜젝션을 유지하기 위해 내부에서 트랜젝션 동기화 매니저를 통해 리소스(커넥션)를 동기화 한다.

##### 스프링이 제공하는 트랜젝션 AOP
* 스프링은 트랜젝션 AOP 를 처리하기 위한 모든 기능을 제공한다. 스프링 부트를 사용하면 트랜젝션 AOP 를 처리하기위해 필요한 스프링 빈들도 자동으로 등록해준다.
* @Transactional 만 선언해주면 된다. 스프링의 트랜젝션 AOP 는 이 어노테이션을 인식해서 트랜젝션을 처리하는 프록시를 적용해준다.

###### @Transactional
org.springframework.transaction.annotation.Transactional

