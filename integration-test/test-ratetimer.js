import http from 'k6/http';
import execution from 'k6/execution'
import { sleep } from 'k6';
export const options = {
  stages: [
    { duration: "30s", target: 200 },
    {duration: "3m", target: 200},
    {duration: "30s", target: 0}
  ]
};
export default function () {

  const params = {
    "headers": {
      "X-Auth-UserId": "LoadTestUser-" + execution.vu.idInTest,
      "X-LoadTest": "true"
    }
  }

  const res = http.get('http://localhost:7703/api/test-data', params);
  console.log(res.status_text, "-",
    res.headers['X-Ratelimit-Limit'],
    res.headers['X-Ratelimit-Remaining'],
    res.headers['X-Ratelimit-Reset'])
  sleep(1)
}