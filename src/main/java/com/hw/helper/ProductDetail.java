package com.hw.helper;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProductDetail extends ProductSimple {

    private Long id;

    private List<ProductOption> selectedOptions;


    private Set<String> imageUrlLarge;

    private Set<String> specification;
}
