package com.murder.service;

import com.murder.common.result.PageResult;
import com.murder.dto.BindEmailDTO;
import com.murder.dto.BindPhoneDTO;
import com.murder.dto.DeactivateAccountDTO;
import com.murder.dto.RealNameVerifyDTO;
import com.murder.dto.UpdatePasswordDTO;
import com.murder.entity.User;
import com.murder.vo.UserLoginLogVO;

import java.util.Map;

public interface UserAccountService {

    boolean updateCurrentUserProfile(Long userId, User user);

    void updatePassword(Long userId, UpdatePasswordDTO dto);

    Map<String, Object> sendPhoneCode(String phone);

    void bindPhone(Long userId, BindPhoneDTO dto);

    Map<String, Object> sendEmailCode(String email);

    void bindEmail(Long userId, BindEmailDTO dto);

    void verifyRealName(Long userId, RealNameVerifyDTO dto);

    PageResult<UserLoginLogVO> getLoginLogs(Long userId, Integer page, Integer pageSize);

    void recordLoginSuccess(Long userId, String ip, String location, String device);

    void deactivateAccount(Long userId, DeactivateAccountDTO dto);
}
