package com.hw.helper;

import lombok.Data;

import java.util.List;
import java.util.Set;

@Data
public class ProductDetail extends ProductSimple {

    private Long id;

    private String imageUrlSmall;

    private String name;

    private String description;

    private List<ProductOption> selectedOptions;

    private Set<String> imageUrlLarge;

    private Set<String> specification;

    private Set<String> attrKey;

    private Set<String> attrProd;

    private Set<String> attrGen;

    private List<ProductSku> productSkuList;

    private ProductStatus status;
}
