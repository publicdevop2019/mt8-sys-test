package com.hw.helper;

import lombok.Data;

import java.util.Set;

@Data
public class Catalog {

    private Long id;

    private String name;

    private Long parentId;

    private Set<String> attributesSearch;

    private CatalogType catalogType;
}
