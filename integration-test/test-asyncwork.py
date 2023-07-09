"""
测试 异步请求-答复模式
"""

import requests
import random
import json
import time

JAVA_SERVICE = "http://127.0.0.1:7701"
VERTX_SERVICE = "http://127.0.0.1:7703"

create_job_api = JAVA_SERVICE + "/api/asyncwork"
job_detail_api = JAVA_SERVICE + "/api/asyncwork"

HEADERS = {
    "Content-Type": "application/json"
}

def print_response(title, res):
    print(title + ":", res.status_code, res.json())


def create_job():
    n = random.randint(1, 1000)
    request_data = {
        "name": "name-" + str(n),
        "value": "value-" + str(n)
    }
    res = requests.post(create_job_api, json.dumps(request_data), headers=HEADERS)
    data = res.json()
    if res.status_code == 202:
        print("Create job:", data["jobId"])
        return data
    else:
        print("Create job failed:", res.status_code, data)
        raise Exception("Create job failed")

def check_job_state(state_endpoint):
    url = JAVA_SERVICE + state_endpoint
    i = 0
    while i < 20:
        i += 1
        res = requests.get(url)
        if res.ok:
            data = res.json()
            print("Job state:", data["state"])
            if data["state"] in ["RUNNING", "NEW"]:
                time.sleep(1.0)
            else:
                return data
        else:
            print("Job state failed:", res.status_code, res.json())
            raise Exception("Check job state failed")
    raise Exception("Retry timeout")

def check_job_result(result_endpoint):
    url = JAVA_SERVICE + result_endpoint
    res = requests.get(url)
    data = res.json()

    if res.ok:
        print("Job result:", data["result"])
    else:
        print("Job result failed:", res.status_code, data)
        raise Exception("Check job result failed")

if __name__ == "__main__":
    
    data = create_job()
    data = check_job_state(data["stateEndpoint"])

    if "resultEndpoint" in data:
        check_job_result(data["resultEndpoint"])
