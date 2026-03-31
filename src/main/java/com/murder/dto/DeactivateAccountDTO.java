package com.murder.dto;

import lombok.Data;

@Data
public class DeactivateAccountDTO {

    private String confirmText;

    private String password;
}
