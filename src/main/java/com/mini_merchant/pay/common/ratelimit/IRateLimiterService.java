package com.mini_merchant.pay.common.ratelimit;

import java.util.UUID;

public interface IRateLimiterService {

    /**
     * Records one request for the given merchant under the fixed-window policy.
     *
     * @return {@code true} if the request is within the per-merchant limit (allow),
     *         {@code false} if the limit is exceeded (reject with 429).
     */
    boolean tryAcquire(UUID merchantId);
}
