package hello.springtx.order;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 스프링 데이터 JPA 사용 인터페이스
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
