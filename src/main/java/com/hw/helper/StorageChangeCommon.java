package com.hw.helper;

import lombok.Data;

import java.util.List;

@Data
public class StorageChangeCommon {
    private String txId;
    private List<StorageChangeDetail> changeList;
}
