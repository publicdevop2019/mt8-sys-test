package com.hw.helper;

import lombok.Data;

import java.util.List;

@Data
public class ProductTotalResponse {
    public List<ProductSimple> productSimpleList;
    public Integer totalPageCount;
    public Long totalProductCount;
}
