package learning_exchange_platform.model;

import lombok.Data;

@Data
public class ShoppingAddress {
    private Integer id;
    private Integer user_id;
    private String shopping_address;
    private String label;
    private boolean default_flag;

    public ShoppingAddress(Integer id, Integer user_id, String shopping_address, String label, boolean default_flag) {
        this.id = id;
        this.user_id = user_id;
        this.shopping_address = shopping_address;
        this.label = label;
        this.default_flag = default_flag;
    }
}
