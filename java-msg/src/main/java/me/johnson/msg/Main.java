package me.johnson.msg;

import me.johnson.msg.listener.WMListener;

public class Main {
    public static void main(String[] args) {
        WMListener wmListener = new WMListener();
        wmListener.start();
        wmListener.sendCopyDataMsg("{\"msg\":\"来自java-msg的消息\",\"className\":\""
                + wmListener.getClassName() + "\"}");
    }
}
