/**
 * 测试 异步请求-答复模式
 */

import { check, sleep } from "k6"
import http from "k6/http"

const java_service = "http://localhost:7701"
const vertx_service = "http://localhost:7703"

export const options = {
};

const headers = {
  'Content-Type': 'application/json' 
}

const printResponse = (title, res) => {
  console.log(title + " :", res.status, res.json())
}


export default function () {

  let n = Math.floor(Math.random() * 1000)
  const data = {
    "name": "TestJob-" + n,
    "value": "value-" + n
  }
  const res = http.post(`${java_service}/api/asyncwork`, JSON.stringify(data), {headers: headers})

  printResponse("Create Job", res)

  if (res.status >= 300 ) {
    return
  }

  let stateEndpoint = res.json().stateEndpoint
  let retryAfter = res.json().retryAfter

  let resultEndpoint

  while(true) {
    const res2 = http.get(java_service + stateEndpoint)
    printResponse("Job State", res2)
    const data = res2.json()
    if (data.state == "NEW" || data.state == "RUNNING") {
      sleep(1)
    } else if (data.state == "FINISH") {
      resultEndpoint = data.resultEndpoint
      break
    } else {
      break
    }
  }

  if (resultEndpoint) {
    const res3 = http.get(java_service + resultEndpoint)
    printResponse("Job Result", res3)
  }

}
