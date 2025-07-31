package kz.ksifactor.client.service;

import kz.ksifactor.client.dto.OrderDto;
import kz.ksifactor.client.entity.OrderEntity;
import kz.ksifactor.client.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    private OrderDto mapToDto(OrderEntity en) {
        OrderDto dto = new OrderDto();
        dto.setId(en.getId());
        dto.setName(en.getName());
        dto.setValue(en.getValue());
        return dto;
    }

    public OrderDto get(Long id) {
        return mapToDto(orderRepository.findById(id).orElseThrow());
    }

    public List<OrderDto> findAll() {
        return orderRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }
}
