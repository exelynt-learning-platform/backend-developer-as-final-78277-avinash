package com.example.booking.dto;

import com.example.booking.entity.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResourceResponse {

    private Long id;
    private String name;
    private String description;
    private String type;
    private Boolean available;
    private BigDecimal price;

    public static ResourceResponse fromEntity(Resource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getDescription(),
                resource.getType(),
                resource.getAvailable(),
                resource.getPrice()
        );
    }
}
