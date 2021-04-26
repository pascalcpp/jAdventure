package com.jadventure.game;

import com.alibaba.fastjson.JSONObject;

import com.jadventure.game.menus.MainMenu;
import io.socket.client.IO;
import io.socket.client.Socket;


import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.Buffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.LockSupport;

/**
 * @author XPCF
 * @version 1.0
 * @date 4/26/2021 7:06 PM
 */
public class Bootstrap {

    private static String SERVER_ADDR = "192.168.20.10";
    private static String PORT = "8888";
    private static String ROBOT_QQ = "3432005295";
    private static String GROUP_ID = "891998488";
    private static String USER_ID;
    public static Boolean RUNNING = false;


    public static void main(String[] args) {

        if (args.length != 1) {
            System.out.println("error args");
            return;
        }
        SERVER_ADDR = args[0];
        try {
            // 建立 websocket 与 opq
            IO.Options options = new IO.Options();
            options.transports = new String[]{"websocket"};
            Socket socket = null;
            socket = IO.socket("http://" + SERVER_ADDR + ":" + PORT, options);

            // register
            socket.on("OnGroupMsgs", (Object... objs) -> {
                JSONObject jsonObject = JSONObject.parseObject(objs[0].toString());
                JSONObject currentPacket = jsonObject.getJSONObject("CurrentPacket");
                JSONObject data = currentPacket.getJSONObject("Data");
                if (RUNNING.equals(false)) {
                    if ("AtMsg".equals(data.getString("MsgType"))) {
                        String userId = getUserId(data);
                        if (ROBOT_QQ.equals(userId)) {
                            new Thread(() -> {
                                // 开始游戏
                                USER_ID = data.getString("FromUserId");
                                QueueProvider.startMessenger(GameModeType.STAND_ALONE);
                                GROUP_ID = data.getString("FromGroupId");
                                RUNNING = true;
                                new MainMenu();
                            }, "gameThread").start();
                        }
                    }
                } else {
                    if (data.getString("FromUserId").equals(USER_ID)) {
                        try {
                            QueueProvider.buffer.put(data.getString("Content"));
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            });


            socket.connect();


        } catch (Exception e) {
            e.printStackTrace();
        } finally {

        }

    }


    private static String getUserId(JSONObject data) {

        JSONObject content = JSONObject.parseObject(data.getString("Content").replace("\\", ""));
        return content.getJSONArray("UserID").getString(0);
    }

    private static void init() {

    }

    public static void sendMessage(String message) throws IOException {
        URL url = new URL("http://" + SERVER_ADDR + ":" + PORT + "/v1/LuaApiCaller?qq=" + ROBOT_QQ + "&funcname=SendMsgV2");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Charset", "UTF-8");
        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setUseCaches(false);
        connection.setDoOutput(true);

        connection.getOutputStream().write(getMessage(GROUP_ID, message).toString().getBytes(StandardCharsets.UTF_8));
        connection.getInputStream();
    }

    private static JSONObject getMessage(String groupId, String message) {
        JSONObject res = new JSONObject();
        res.put("ToUserUid", Integer.valueOf(groupId));
        res.put("SendToType", 2);
        res.put("SendMsgType", "TextMsg");
        res.put("Content", message);

//        res.put("toUser", Integer.valueOf(groupId));
//        res.put("SendToType", 2);
//        res.put("SendMsgType", "TextMsg");"
//        res.put("content", message);
//        res.put("groupid", 0);
//        res.put("atUser", 0);
        return res;
    }
}
