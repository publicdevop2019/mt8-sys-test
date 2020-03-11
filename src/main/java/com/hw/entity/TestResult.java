package com.hw.entity;

import lombok.Data;

import javax.persistence.*;

@Entity
@Data
@Table(name = "TestResult")
@SequenceGenerator(name = "entityId_gen", sequenceName = "entityId_seq", initialValue = 1)
public class TestResult {
    @Id
    @GeneratedValue(generator = "entityId_gen")
    private Long id;
    @Column
    private Integer testExecuted;
    @Column
    private Integer ignored;
    @Column
    private Integer failed;
    @Column
    private Long elapse;
    @Column
    private String status;
    @Column(length = 10000)
    private String failedMsg;

}
