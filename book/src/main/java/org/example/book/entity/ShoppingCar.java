package org.example.book.entity;

public class ShoppingCar {
        private Integer id;
        private Integer userId;
        private Integer sellerId;
        private Integer bookId;
        private String bookName;
        private Double price;
        private Book book;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }
        public Integer getSellerId() {
        return sellerId;
    }

    public void setSellerId(Integer sellerId) {
        this.sellerId = sellerId;
    }

    public Integer getBookId() {
        return bookId;
    }

    public void setBookId(Integer bookId) {
        this.bookId = bookId;
    }

    public String getBookName() {
        return bookName;
    }

    public void setBookName(String bookName) {
        this.bookName = bookName;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    @Override
    public String toString() {
        return "ShoppingCar{" +
                "id=" + id +
                ", userId=" + userId +
                ", sellerId=" + sellerId +
                ", bookId=" + bookId +
                ", bookName='" + bookName + '\'' +
                ", price=" + price +
                ", book=" + book +
                '}';
    }
}
