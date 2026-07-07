```
  █ THRESHOLDS

    http_req_duration
    ✓ 'p(95)<5000' p(95)=21.22ms

    http_req_failed
    ✗ 'rate<0.01' rate=54.74%


  █ TOTAL RESULTS

    checks_total.......: 172628 657.109729/s
    checks_succeeded...: 72.62% 125372 out of 172628
    checks_failed......: 27.37% 47256 out of 172628

    ✗ status is 201
      ↳  45% — ✓ 39063 / ✗ 47251
    ✗ has response body
      ↳  99% — ✓ 86309 / ✗ 5

    CUSTOM
    tx_failed......................: 6440   24.513907/s
    tx_success.....................: 32623  124.179685/s

    HTTP
    http_req_duration..............: avg=8.68ms  min=-1362886811ns med=5.39ms  max=4.13s p(90)=15.72ms p(95)=21.22ms
      { expected_response:true }...: avg=14.38ms min=-1362886811ns med=10.31ms max=4.13s p(90)=21.75ms p(95)=28.75ms
    http_req_failed................: 54.74% 47251 out of 86314
    http_reqs......................: 86314  328.554864/s

    EXECUTION
    iteration_duration.............: avg=1.01s   min=1s            med=1s      max=31s   p(90)=1.01s   p(95)=1.02s
    iterations.....................: 86314  328.554864/s
    vus............................: 10     min=4              max=500
    vus_max........................: 500    min=500            max=500

    NETWORK
    data_received..................: 32 MB  123 kB/s
    data_sent......................: 31 MB  119 kB/s




running (4m22.7s), 000/500 VUs, 86314 complete and 0 interrupted iterations
default ✓ [ 100% ] 000/500 VUs  4m0s
time="2026-07-07T15:46:03Z" level=error msg="thresholds on metrics 'http_req_failed' have been crossed"
```
