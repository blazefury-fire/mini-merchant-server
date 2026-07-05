package com.mini_merchant.pay.domain.auth.service;

import com.mini_merchant.pay.domain.auth.dto.register.RegisterReqModel;
import com.mini_merchant.pay.domain.auth.dto.register.RegisterResModel;

public interface IAuthService {
    RegisterResModel register(RegisterReqModel request);
}
