package com.hw.helper;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class SnapshotProduct {

    private String name;

    private List<ProductOption> selectedOptions;

    private String finalPrice;

    private String imageUrlSmall;

    private String productId;

    private Set<String> attributesSales;

}
