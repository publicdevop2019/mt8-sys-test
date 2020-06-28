package com.hw.helper;

import lombok.Data;

import java.util.Set;

@Data
public class StorageChangeDetail {
    private Long productId;
    private Set<String> attributeSales;
    private Integer amount;

}
