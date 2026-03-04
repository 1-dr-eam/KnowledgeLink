package org.example.book.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Order {
    private Integer id;
    private Integer bookId;
    private String bookName;
    private Integer sellerId;
    private Integer buyerId;
    private double price;
    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishedTime;

    private String address;
    private String deliveryTime;

    // getter 和 setter 方法
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getBookId() { return bookId; }
    public void setBookId(Integer bookId) { this.bookId = bookId; }

    public String getBookName() { return bookName; }
    public void setBookName(String bookName) { this.bookName = bookName; }

    public Integer getSellerId() { return sellerId; }
    public void setSellerId(Integer sellerId) { this.sellerId = sellerId; }

    public Integer getBuyerId() { return buyerId; }
    public void setBuyerId(Integer buyerId) { this.buyerId = buyerId; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    // 添加字符串设置方法，用于处理数据库返回的字符串时间
    public void setCreateTime(String createTimeStr) {
        if (createTimeStr != null && !createTimeStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                this.createTime = LocalDateTime.parse(createTimeStr, formatter);
            } catch (Exception e) {
                System.err.println("时间格式转换错误: " + createTimeStr);
                // 如果转换失败，设置为当前时间
                this.createTime = LocalDateTime.now();
            }
        }
    }

    public LocalDateTime getFinishedTime() { return finishedTime; }
    public void setFinishedTime(LocalDateTime finishedTime) { this.finishedTime = finishedTime; }

    // 添加字符串设置方法，用于处理数据库返回的字符串时间
    public void setFinishedTime(String finishedTimeStr) {
        if (finishedTimeStr != null && !finishedTimeStr.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                this.finishedTime = LocalDateTime.parse(finishedTimeStr, formatter);
            } catch (Exception e) {
                System.err.println("时间格式转换错误: " + finishedTimeStr);
                // 如果转换失败，设置为null
                this.finishedTime = null;
            }
        }
    }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getDeliveryTime() { return deliveryTime; }
    public void setDeliveryTime(String deliveryTime) { this.deliveryTime = deliveryTime; }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", bookId=" + bookId +
                ", bookName='" + bookName + '\'' +
                ", sellerId=" + sellerId +
                ", buyerId=" + buyerId +
                ", price=" + price +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", finishedTime=" + finishedTime +
                ", address='" + address + '\'' +
                ", deliveryTime='" + deliveryTime + '\'' +
                '}';
    }
}