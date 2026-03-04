package learning_exchange_platform.mapper;

import learning_exchange_platform.model.ShoppingAddress;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingAddressMapper {
    public List<ShoppingAddress> selectShoppingAddressByUserId(int user_id);
    public boolean insertShoppingAddress(int user_id, String shopping_address,String label,boolean default_flag);
    public boolean deleteShoppingAddressById(int id);
    public boolean updateShoppingAddress(int id, String shopping_address,String label,boolean default_flag);
}
