package com.mall.order.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderExportVO {

    @ExcelProperty("订单号")
    private String orderNo;

    @ExcelProperty("收货人")
    private String receiverName;

    @ExcelProperty("联系电话")
    private String receiverPhone;

    @ExcelProperty("收货地址")
    private String receiverAddress;

    @ExcelProperty("订单金额")
    private BigDecimal totalAmount;

    @ExcelProperty("订单状态")
    private String statusText;

    @ExcelProperty("支付方式")
    private String paymentMethod;

    @ExcelProperty("支付时间")
    private String paymentTime;

    @ExcelProperty("发货时间")
    private String deliveryTime;

    @ExcelProperty("确认收货时间")
    private String receiveTime;

    @ExcelProperty("物流公司")
    private String logisticsCompany;

    @ExcelProperty("运单编号")
    private String logisticsNo;

    @ExcelProperty("商品信息")
    private String productInfo;

    @ExcelProperty("下单时间")
    private String createdTime;
}
