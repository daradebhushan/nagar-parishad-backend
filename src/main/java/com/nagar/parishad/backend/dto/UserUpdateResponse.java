package com.nagar.parishad.backend.dto;

import com.nagar.parishad.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateResponse {
    private User user;
    private String token;

}
