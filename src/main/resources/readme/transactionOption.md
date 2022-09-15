### 트랜젝션 옵션
```
public @interface Transactional {

  String value() default "";
  String transactionManager() default "";
  
  Class<? extends Throwable>[] rollbackFor() default {};
  Class<? extends Throwable>[] noRollbackFor() default {};
  
  Propagation propagation() default Propagation.REQUIRED;
  Isolation isolation() default Isolation.DEFAULT;
  int timeout() default TransactionDefinition.TIMEOUT_DEFAULT;
  boolean readOnly() default false;
  String[] label() default {};
  
}
```

##### value, transactionManager
트랜젝션을 사용하려면 먼저 어떤 스프링 빈에 등록된 어떤 트랜젝션 매니저를 사용할지를 알아야 한다.<br>
코드로 직접 트랜젝션을 사용할 때 트랜젝션 매니저를 주입받아야 했다.<br>
@Transactional 에서도 트랜젝션 프록시가 사용할 트랜젝션 매니저를 지정해주어야 한다.<br>
사용할 트랜젝션 매니저를 지정할 때는, value, transactionManager 둘 중 하나에 트랜젝션 매니저의 스프링 빈 이름을 설정하면 된다.<br>
이 값을 생략하면 기본으로 등록된 트랜젝션 매니저를 사용하기 때문에 대부분 생략한다.<br>
그런데, 사용하는 트랜젝션 매니저가 둘 이상이라면 트랜젝션 매니저의 이름을 지정해서 구분하면 된다.<br>

***트랜젝션 매니저 구분 지정***
```
public class TxService {

  @Transactional("memberTxManager")
  public void member() {...}
  
  @Transactional("orderTxManager")
  public void order() {...}

}
```
※ 어노테이션 옵션 1개인 경우 value 생략 가능<br>

##### rollbackFor
예외 발생시 스프링 트랜젝션의 기본 정책<br>
* 언체크예외인 RuntimeException, Error 와 그 하위 예외가 발생하면 롤백
* 체크예외인 Exception 과 그 하위 예외들은 커밋

rollbackFor 옵션으로 기본정책에 추가로 롤백시킬 예외를 지정할 수 있다.<br>
```
@Transactional(rollbackFor = Exception.class)
```
이경우, 체크 예외인 Exception 가 발생해도 rollback 한다.<br>

rollbackForClassName 도 있는데,<br>
rollbackFor 는 예외 클래스를 직접 지정하고<br>
rollbackForClassName 은 예외 이름을 문자로 넣으면 된다.<br>


##### noRollbackFor
rollbackFor 의 반대<br>
기본 정책에 추가로 어떤 예외가 발생했을 때 롤백하면 안되는지 지정할 수 있다.<br>
예외 이름을 문자로 지정할 수 있는 noRollbackForClassName 도 있다.<br>


##### propagation
트랜젝션 전파에 대한 옵션


##### isolation
트랜젝션 격리 수준 지정<br>
기본값은 DB 에서 설정한 트랜젝션 격리 수준을 사용하는 DEFAULT 이다.<br>
대부분 DB 에서 설정한 기준을 따른다. 어플리케이션 개발자가 직접 지정하는 경우는 드물다.<br>
* DEFAULT : 데이터베이스에서 설정한 격리 수준을 따른다.
* READ_UNCOMMITTED : 커밋되지 않은 읽기 (일반적으로 많이 사용함)
* READ_COMMITTED : 커밋된 읽기
* REPEATABLE_READ : 반복가능한 읽기
* SERIALIZABLE : 직렬화 기능


##### timeout
트랜젝션 수행 시간에 대한 타임아웃을 초 단위로 지정 <br>
기본값은 트랜젝션 시스템의 타임아웃을 사용한다.<br>
운영환경에 따라 동작하는 경우도 있고, 동작하지 않는 경우도 있어 확인이 필요하다.<br>
숫자 대신 문자로 지정할 수 있는 timeoutString 도 존재한다. 


##### label
트랜젝션 어노테이션에 있는 값을 직접 읽어서 어떤 동작을 하고 싶을 때 사용 가능<br>
일반적으로 사용하지 않는다.<br>


##### readOnly
트랜젝션은 기본적으로 읽기, 쓰기 모두 가능한 트랜젝션이 생성된다.<br>
readOnly=true 옵션을 사용하면 읽기전용 트랜젝션이 생성된다.<br>
이 경우 등록, 수정, 삭제가 안되고 조회만 동작한다. 다만, 드라이버나 데이터베이스에 따라 정상 동작하지 않는 경우도 있다.<br>
그리고 readOnly 옵션 사용을 통해 읽기에서 다양한 성능 최적화가 발생할 수 있다.<br>

※ readOnly 옵션은 크게 3곳에서 적용된다<br>
* 프레임워크
  * JdbcTemplate 은 읽기전용 트랜젝션 안에서 변경 기능 실행시 예외를 던진다.
  * JPA(Hibernate) 는 읽기전공 트랜젝션의 경우 커밋 시점에 flush 를 호출하지 않는다.
    * 읽기 전용이니 변경에 사용되는 flush 를 호출할 필요가 없다.
    * 추가로 변경이 필요 없으니 변경감지를 위한 스냅샷 객체도 생성하지 않는다.
    * 이렇게 JPA 에서는 다양한 최적화가 발생한다.


* JDBC 드라이버 (DB 및 드라이버 버전에 따라 다를 수 있으므로 확인 필요)
  * 읽기전용 트랜젝션에서 변경 쿼리가 발생하면 예외를 던진다.
  * 읽기, 쓰기(마스터, 슬레이브) 데이터베이스를 구분해서 요청한다.
    * 읽기전용 트랜젝션의 경우 읽기(슬레이브) 데이터베이스의 커넥션을 획득해서 사용한다.


* 데이터베이스
  * 데이터베이스에 따라 읽기전용 트랜젝션의 경우 읽기만 하면 되므로, 내부에서 성능최적화가 발생한다.


### 트랜젝션 전파 (propagation)
트랜젝션 진행중에 추가로 트랜젝션을 수행

###### 외부 트랜젝션 수행중, 내부 트랜젝션이 추가 수행
* 외부 트랜젝션이 수행중이고, 아직 끝나지 않은 상태에 내부 트랜젝션이 수행된다.
* 외부 트랜젝션 : 둘 중 상대적으로 밖에 있는 트랜젝션. 처음 시작된 트랜젝션
* 내부 트랜젝션 : 외부 트랜젝션 수행중 호출되어 내부에 있는 것처럼 보이는 트랜젝션
* 스프링의 경우, 외부 트랜젝션과 내부 트랜젝션을 묶어서 하나의 트랜젝션을 만들어준다.
  * 내부 트랜젝션이 외부 트랜젝션에 참여하는 것이다. 이것이 기본 동작이고, 옵션을 통해 다른 동작방식도 선택할 수 있다.


###### 물리 트랜젝션, 논리 트랜젝션
* 스프링은 이해를 돕기 위해 물리 트랜젝션과 논리 트랜젝션이라는 개념을 나눈다.
* 논리 트랜젝션은 하나의 물리 트랜젝션으로 묶인다.
* 물리 트랜젝션은 실제 데이터베이스에 적용되는 트랜젝션을 의미한다.
  * 실제 커넥션을 통해 트랜젝션을 시작(setAutoCommit(false))하고, 실제 커넥션을 통해 커밋, 롤백하는 단위이다.
* 논리 트랜젝션은 트랜젝션 매니저를 통해 트랜젝션을 사용하는 단위이다.
* 논리 트랜젝션 개념은 트랜젝션이 진행되는 중에 내부에 추가로 트랜젝션을 사용하는 경우에 나타난다.
  * 단순히 트랜젝션이 하나인 경우 둘을 구분하지 않는다. 
  * (더 정확히는 REQUIRED 전파 옵션을 사용하는 경우에 나타난다.)

**원칙**<br>
* **모든 논리 트랜젝션이 커밋되어야 물리 트랜젝션이 커밋된다.**
* **하나의 논리 트랜젝션이라도 롤백되면 물리 트랜젝션은 롤백된다.**
