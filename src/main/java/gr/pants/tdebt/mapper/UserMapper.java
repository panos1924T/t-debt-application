package gr.pants.tdebt.mapper;

import gr.pants.tdebt.dto.user_dto.UserInsertDTO;
import gr.pants.tdebt.dto.user_dto.UserReadOnlyDTO;
import gr.pants.tdebt.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserReadOnlyDTO toReadOnlyDTO(User user) {
        return new UserReadOnlyDTO(
                user.getUuid().toString(),
                user.getEmail(),
                user.isDeleted(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public User toEntity(UserInsertDTO insertDTO, String hashedPassword) {
        User user = new User();
        user.setEmail(insertDTO.email());
        user.setPassword(hashedPassword);

        return user;
    }
}
