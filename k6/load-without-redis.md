```
  █ THRESHOLDS

    http_req_duration
    ✗ 'p(95)<5000' p(95)=5.18s

    http_req_failed
    ✓ 'rate<0.01' rate=0.00%


  █ TOTAL RESULTS

    checks_total.......: 57122   217.537298/s
    checks_succeeded...: 100.00% 57122 out of 57122
    checks_failed......: 0.00%   0 out of 57122

    ✓ status is 201
    ✓ has response body

    CUSTOM
    tx_failed......................: 4847  18.458795/s
    tx_success.....................: 23714 90.309854/s

    HTTP
    http_req_duration..............: avg=2.24s min=94.96µs med=2.4s  max=8s    p(90)=3.54s p(95)=5.18s
      { expected_response:true }...: avg=2.24s min=94.96µs med=2.4s  max=8s    p(90)=3.54s p(95)=5.18s
    http_req_failed................: 0.00% 0 out of 28561
    http_reqs......................: 28561 108.768649/s

    EXECUTION
    iteration_duration.............: avg=3.06s min=1.01s   med=3.38s max=6.45s p(90)=4.1s  p(95)=4.4s
    iterations.....................: 28561 108.768649/s
    vus............................: 10    min=4          max=500
    vus_max........................: 500   min=500        max=500

    NETWORK
    data_received..................: 11 MB 44 kB/s
    data_sent......................: 10 MB 39 kB/s




running (4m22.6s), 000/500 VUs, 28561 complete and 0 interrupted iterations
default ✓ [ 100% ] 000/500 VUs  4m0s
time="2026-07-07T15:38:41Z" level=error msg="thresholds on metrics 'http_req_duration' have been crossed"
```