package kz.ksifactor.client.dto;

import jakarta.persistence.Column;
import lombok.Data;

@Data
public class OrderDto {
    Long id;
    String name;
    Double value;
}
