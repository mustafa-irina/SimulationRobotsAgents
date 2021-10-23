package com.company;

import javax.swing.text.StyleConstants;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class BrainServer {
    private int time_out;

    private boolean workServer = false;
    public boolean isWorkServer() { return this.workServer; }

    private boolean robotSentMessage = false;
    public boolean isRobotSentMessage() { return this.robotSentMessage; }

    private String robotMessage = null;
    public String getRobotMessage() { return this.robotMessage; }

    private String brainMessage = null;
    public String getBrainMessage() { return this.brainMessage; }
    public void setBrainMessage(String new_message) { this.brainMessage = new_message; }

    private final String AGENT_HOST;
    public String getAGENT_HOST(){ return this.AGENT_HOST; }

    private final int AGENT_PORT;
    public int getAGENT_PORT(){ return this.AGENT_PORT; }

    public BrainServer(String host, int port, int time_out) {
        this.AGENT_HOST = host;
        this.AGENT_PORT = port;
        this.time_out = time_out;
    }

    private class LisenerSpeaker implements Runnable {
        InputStream in;
        OutputStream out;
        BufferedReader buff_in;
        BufferedWriter buff_out;
        Socket client;
        String key;
        LisenerSpeaker(Socket client, String key) {
            this.key = key;
            this.client = client;
            try {
                this.in = this.client.getInputStream();
                this.buff_in = new BufferedReader(new InputStreamReader(this.in));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                this.out = this.client.getOutputStream();
                this.buff_out = new BufferedWriter(new OutputStreamWriter(this.out));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void run() {
            try {
                while (!client.isClosed()) {
                    //robotMessage = null;
                    robotMessage = buff_in.readLine();
                    //System.out.println(robotMessage);
                    if (robotMessage != null) {
                        if (!robotMessage.isEmpty()) {
                            robotSentMessage = true;
                        } else {
                            robotSentMessage = false;
                        }
                    } else {
                        robotSentMessage = false;
                    }
                    Thread.sleep(time_out);
                    //System.out.println(robotMessage);
                }
                in.close();
                buff_in.close();

                out.close();
                buff_out.close();

                client.close();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() throws InterruptedException {
        //port = 3345;
        workServer = true;

        try (ServerSocket server= new ServerSocket(this.AGENT_PORT)) {

            boolean work = true;

            Socket client = server.accept();
            String nickname = "Robot connection!";
            System.out.println("Connection accepted. " + '(' + nickname + ')');
            Thread lisenerRobot = new Thread(new LisenerSpeaker(client, nickname));
            //lisenerRobot.setDaemon(true);
            lisenerRobot.start();

            while (work) {
                //System.out.println(brainMessage);
                client.getOutputStream().write(this.brainMessage.getBytes(StandardCharsets.UTF_8));
                client.getOutputStream().flush();
                Thread.sleep(this.time_out);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}