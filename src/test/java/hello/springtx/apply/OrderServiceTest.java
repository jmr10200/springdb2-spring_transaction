package hello.springtx.apply;

import hello.springtx.order.NotEnoughMoneyException;
import hello.springtx.order.Order;
import hello.springtx.order.OrderRepository;
import hello.springtx.order.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@Slf4j
@SpringBootTest
public class OrderServiceTest {

    @Autowired
    OrderService orderService;
    @Autowired
    OrderRepository orderRepository;

    // 참고
    // 메모리 DB 를통해 테스트를 수행하면 테이블 자동 생성 옵션이 활성화된다.
    @Test
    void complete() throws NotEnoughMoneyException {
        // given
        Order order = new Order();
        order.setUsername("정상");

        // when
        orderService.order(order);

        // then
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("완료");
    }

    @Test
    void runtimeException() {
        // given
        Order order = new Order();
        order.setUsername("예외");

        // when, then
        assertThatThrownBy(() -> orderService.order(order))
                .isInstanceOf(RuntimeException.class);

        // then : rollback 되었으므로 데이터가 없어야 함
        Optional<Order> orderOptional = orderRepository.findById(order.getId());
        assertThat(orderOptional.isEmpty()).isTrue();
    }

    @Test
    void bizException() {
        // given
        Order order = new Order();
        order.setUsername("잔고부족");

        // when
        try {
            orderService.order(order);
            fail("잔고부족 예외가 발생해야 합니다.");
        } catch (NotEnoughMoneyException e) {
            log.info("고객에게 잔고 부족을 알리고 별도의 계좌로 입금하도록 안내");
        }
        // then
        Order findOrder = orderRepository.findById(order.getId()).get();
        assertThat(findOrder.getPayStatus()).isEqualTo("대기");
    }

}
// 정리
// NotEnoughMoneyException 은 시스템에 문제가 발생한 것이 아니다.
// 비즈니스 문제 상황을 예외를 통해 알려준다. 마치 예외가 리턴 값처럼 사용된다.
// 그래서 이경우 트랜젝션을 커밋하는 것이 맞다. 롤백하면 생성한 Order 자체가 사라져 버린다.
// 그러면 고객에게 잔고부족을 알리고 별도의 계좌로 입금하도록 안내해도 주문(order) 자체가 사라지기 때문에 문제가 된다.

// 비즈니스 상황에 따라 체크 예외도 커밋하지않고 롤백해야 할 때가 있다. 이때는 rollbackFor 옵션을 사용하면 된다.

// 런타임 예외는 항상 롤백된다. 체크 예외의 경우 rollbackFor 옵션을 사용해서 비즈니스 상황에 따라서 커밋과 롤백을 선택하면 된다.
