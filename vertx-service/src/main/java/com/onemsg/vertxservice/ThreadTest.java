package com.onemsg.vertxservice;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

public class ThreadTest {
    
    static final Executor executor = Executors.newFixedThreadPool(3);

    static void serverSocket() throws Exception {

        Thread.currentThread().setName("ServerSocket-Thread-Accept");
        printThreadStateAfter(Thread.currentThread(), 200);

        ServerSocket serverSocket = new ServerSocket(6060);
        while (true) {
            
            // 这里会IO阻塞，会打印这时候的线程状态
            Socket socket = serverSocket.accept();
            
            var in = socket.getInputStream();
            var out = socket.getOutputStream();
            
            byte[] request = in.readNBytes(in.available());
            
            sleep(500);

            var data = "Response-" + ThreadLocalRandom.current().nextInt(100);
            out.write(data.getBytes());
            out.flush();
            socket.close();
        }
    }

    static void socket() throws Exception {
        executor.execute(() -> {
            try {
                serverSocket();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        sleep(250);

        Socket socket = new Socket("127.0.0.1", 6060);

        Thread.currentThread().setName("Socket-Thread");
        printThreadStateAfter(Thread.currentThread(), 350);

        socket.getOutputStream().write("Request-1".getBytes());
        socket.getOutputStream().flush();
        // 这里会打印 Socket 读取数据时线程的状态
        var response = socket.getInputStream().readAllBytes();
        System.out.println("Get response: " + new String(response));
        socket.close();
    }

    public static void main(String[] args) throws Exception {

        Thread t = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                ThreadLocalRandom.current().ints(10000).sorted().sum();
                System.out.print(i + " ");
                if (Thread.currentThread().interrupted()) {
                    System.out.println(Thread.currentThread() + "被打断了");
                    // return;
                }
            }
            System.out.println("Execution OK");
        });

        t.start();
        Thread.sleep(20);
        t.interrupt();
        Thread.sleep(20);
        t.interrupt();
    }


    public static void printThreadStateAfter(Thread thread, long millis) {
        Executors.newSingleThreadExecutor().submit(() -> {
            sleep(millis);
            System.out.println(thread + ": " + thread.getState());
        });
    }

    public static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (Exception e) {
            // TODO: handle exception
        }
    }
}
