package com.onemsg.javaservice.patterns;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onemsg.commonservice.mq.Topics;
import com.onemsg.commonservice.mq.event.AsyncWorkEvent;
import com.onemsg.commonservice.store.AsyncJob;
import com.onemsg.commonservice.store.AsyncJobResult;
import com.onemsg.commonservice.store.AsyncJobStore;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@RestController
@RequestMapping("/api/asyncwork")
public class AsyncWorkController {

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String TOPIC = Topics.ASYNC_JOB_TOPIC;

    public record AsyncWorkRequest(String name, String value) {}

    @PostMapping("")
    public ResponseEntity<Object> postAsyncWork(@RequestBody AsyncWorkRequest work) {
        
        // 生成任务
        String jobId = UUID.randomUUID().toString();

        var jsonDBTemplate = AsyncJobStore.jsondb();
        AsyncJob job = new AsyncJob();
        job.setId(jobId);
        job.setName(work.name());
        job.setValue(work.value());
        job.setState(AsyncJob.State.NEW);
        job.setCreatedTime(LocalDateTime.now());
        job.setCreatedTime(job.getCreatedTime());
        jsonDBTemplate.insert(job);

        log.info("Job已创建 {}", jobId);

        // 发送任务处理事件到消息队列
        AsyncWorkEvent event = new AsyncWorkEvent(jobId, work.name(), work.value());

        sendEvent(event);

        // 返回带状态检查路径响应
        String statucEndpoint = "/api/asyncwork/state?jobId=" + jobId;
        var data = Map.of("jobId", jobId, "stateEndpoint", statucEndpoint, "retryAfter", 1000);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(data);
    }

    @GetMapping("/state")
    public ResponseEntity<Object> getState(@RequestParam String jobId) {

        var jsonDBTemplate = AsyncJobStore.jsondb();
        jsonDBTemplate.reLoadDB();
        var job = jsonDBTemplate.findById(jobId, AsyncJob.class);
        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        if (job.getState() == AsyncJob.State.FINISH) {
            var resultEndpoint = "/api/asyncwork/result?jobId=" + jobId;
            var data = Map.of("jobId", jobId, "state", job.getState(), "resultEndpoint", resultEndpoint);
            return ResponseEntity.ok(data);
        }
        
        var data = Map.of("jobId", job.getId(), "state", job.getState());
        return ResponseEntity.ok(data);
    }

    @GetMapping()
    public ResponseEntity<Object> getDetail(@RequestParam String jobId) {
        var jsonDBTemplate = AsyncJobStore.jsondb();
        jsonDBTemplate.reLoadDB();
        var job = jsonDBTemplate.findById(jobId, AsyncJob.class);

        if (job == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.status(200).body(job);
    }

    @GetMapping("/result")
    public ResponseEntity<Object> getResult(@RequestParam String jobId) {

        var jsonDBTemplate = AsyncJobStore.jsondb();
        jsonDBTemplate.reLoadDB();
        var result = jsonDBTemplate.findById(jobId, AsyncJobResult.class);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }


    private String toJsonString(AsyncWorkEvent event) throws ResponseStatusException {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请求数据无效", e);
        }
    }

    private void sendEvent(AsyncWorkEvent event) throws ResponseStatusException  {
        var value = toJsonString(event);
        var future = kafkaTemplate.send(TOPIC, value);
        try {
            var result = future.get(5, TimeUnit.SECONDS);
            log.info("Kafka 事件已发送 {} {} {}", result.getRecordMetadata().topic(), 
                result.getRecordMetadata().offset(), event.jobId());
        } catch (ExecutionException e) {
            log.warn("发送 Kafka 失败 {} {} {}", TOPIC, value, e.getCause());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部处理错误");
        } catch (Exception e) {
            log.warn("发送 Kafka 失败 {} {} {}", TOPIC, value, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部处理错误");
        }
    }

}
