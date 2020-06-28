package com.hw.helper;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class ProductSkuCustomerRepresentation {
    private Set<String> attributeSales;
    private Integer storageOrder;
    private BigDecimal price;
}
