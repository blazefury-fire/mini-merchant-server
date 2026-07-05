package com.mini_merchant.pay.domain.auth.service;

import com.mini_merchant.pay.domain.auth.dto.login.LoginReqModel;
import com.mini_merchant.pay.domain.auth.dto.login.LoginResModel;
import com.mini_merchant.pay.domain.auth.dto.logout.LogoutReqModel;
import com.mini_merchant.pay.domain.auth.dto.refresh.RefreshReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterResModel;

public interface IAuthService {
    RegisterResModel register(RegisterReqModel request);

    LoginResModel login(LoginReqModel request);

    LoginResModel refresh(RefreshReqModel request);

    void logout(LogoutReqModel request);
}
