package com.onemsg.vertxservice.patterns;

import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.onemsg.commonservice.mq.Topics;
import com.onemsg.commonservice.mq.event.AsyncWorkEvent;
import com.onemsg.commonservice.store.AsyncJob;
import com.onemsg.commonservice.store.AsyncJobResult;
import com.onemsg.commonservice.store.AsyncJobStore;

import io.netty.util.internal.ThreadLocalRandom;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AsyncJobWorkerVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) throws Exception {

        Map<String, String> config = new HashMap<>();
        
        String host = Objects.requireNonNullElse(System.getenv("WSL_HOST"), "localhost");
        config.put("bootstrap.servers", host + ":9092");
        config.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("group.id", "vertx-service-group");
        // latest earliest
        config.put("auto.offset.reset", "latest");
        config.put("enable.auto.commit", "false");

        KafkaConsumer<String, String> consumer = KafkaConsumer.create(vertx, config);

        consumer.handler(kafkaRecord -> {
            handleEvent(kafkaRecord.value());
            consumer.commit();
        });

        consumer.subscribe(Topics.ASYNC_JOB_TOPIC)
            .onSuccess(startPromise::complete)
            .onFailure(startPromise::fail);
    }


    private void handleEvent(String event) {
        AsyncWorkEvent asyncWorkEvent;
        try {
            asyncWorkEvent = Json.decodeValue(event, AsyncWorkEvent.class);
        } catch (DecodeException  e) {
            log.warn("Kafka 反序列化失败 {} {}", event, e);
            return;
        }

        vertx.executeBlocking(promise -> {
            runJob(asyncWorkEvent.jobId());
            promise.complete();
        });
    }
    
    private void runJob(String jobId) {

        // 模拟任务执行

        var jsonDBTemplate = AsyncJobStore.jsondb();
        jsonDBTemplate.reLoadDB();
        var job = jsonDBTemplate.findById(jobId, AsyncJob.class);

        if (job == null) {
            log.warn("不存在Job {}", jobId);
            return;
        }

        if (job.getState() != AsyncJob.State.NEW && job.getState() != AsyncJob.State.PAUSE) {
            log.warn("无法开始执行Job {}, 状态:{}", job.getId(), job.getState() );
            return;
        }

        log.info("开始执行Job {}", jobId);

        long startTime = System.currentTimeMillis();
        job.setState(AsyncJob.State.RUNNING);
        job.setUpdatedTime(LocalDateTime.now());
        jsonDBTemplate.upsert(job);

        sleep(ThreadLocalRandom.current().nextLong(3000, 10000));
        // 1/5 概率失败
        boolean failed = ThreadLocalRandom.current().nextInt(5) == 1;
        if (!failed) {
            String content = Arrays.toString(ThreadLocalRandom.current().ints(10).toArray());
            AsyncJobResult result = new AsyncJobResult();
            result.setJobId(jobId);
            result.setResult(content);
            result.setFinishedTime(LocalDateTime.now());
            jsonDBTemplate.insert(result);
        }
        long endTime = System.currentTimeMillis();

        job.setState(failed ? AsyncJob.State.FAILURE : AsyncJob.State.FINISH);
        job.setUpdatedTime(LocalDateTime.now());
        job.setExecutionTime((int) (endTime - startTime));

        jsonDBTemplate.upsert(job);

        log.info("执行结束Job {}, {}, {} ms", jobId, job.getState(), job.getExecutionTime());
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) { }
    }

}
