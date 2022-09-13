package hello.springtx.order;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Order Entity (JPA 사용)
 */
@Entity
@Table(name = "orders") // DB 예약어 order by 때문에 orders 지정함
@Getter
@Setter
public class Order {

    @Id
    @GeneratedValue
    private Long id;

    // 정상, 예외, 잔고부족
    private String username;

    // 대기, 완료
    private String payStatus;
}
