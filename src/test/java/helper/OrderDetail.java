package helper;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Data
public class OrderDetail {
    private Long id;
    private SnapshotAddress address;
    private List<SnapshotProduct> productList;
    private String paymentType;
    private BigDecimal paymentAmt;
    private String paymentDate;
    private PaymentStatus paymentStatus;
    private Date modifiedByUserAt;
    private Boolean expired;
    private Boolean revoked;

}

