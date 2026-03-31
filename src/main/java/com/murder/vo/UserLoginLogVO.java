package com.murder.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginLogVO implements Serializable {

    private String time;

    private String ip;

    private String location;

    private String device;

    private String status;
}
