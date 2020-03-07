package helper;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductSimple {

    private Long id;

    private String imageUrlSmall;

    private String name;

    private Integer orderStorage;

    private Integer actualStorage;

    private Integer increaseOrderStorageBy;

    private Integer decreaseOrderStorageBy;

    private Integer increaseActualStorageBy;

    private Integer decreaseActualStorageBy;

    private String description;

    private String rate;

    private BigDecimal price;

    private Integer sales;

    private String category;

}
