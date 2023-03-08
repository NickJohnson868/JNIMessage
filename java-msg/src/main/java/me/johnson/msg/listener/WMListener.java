package me.johnson.msg.listener;

import com.sun.jna.Callback;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.*;

import java.io.UnsupportedEncodingException;

import static com.sun.jna.platform.win32.WinUser.WM_COPYDATA;

public class WMListener extends Thread {
    private String className = "WMListener Listener";
    private String toWindowName = "GeoGebra Classic 5 Listener";
    private WinDef.HWND hWnd;

    public WMListener() {
    }

    public WMListener(String className, String toWindowName) {
        this.className = className;
        this.toWindowName = toWindowName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public WinDef.HWND gethWnd() {
        return hWnd;
    }

    public String getToWindowName() {
        return toWindowName;
    }

    public void setToWindowName(String toWindowName) {
        this.toWindowName = toWindowName;
    }

    private class WindowProc implements Callback {
        public WinDef.LRESULT callback(WinDef.HWND hWnd, WinDef.UINT uMsg, WinDef.WPARAM wParam,
                                       WinDef.LPARAM lParam) throws UnsupportedEncodingException {
            switch (uMsg.intValue()) {
                case WinUser.WM_DESTROY:
                    User32.INSTANCE.PostQuitMessage(0);
                    break;
                case WinUser.WM_COPYDATA:
                    handleCopyDataMsg(hWnd, uMsg, wParam, lParam);
                    break;
            }
            return User32.INSTANCE.DefWindowProc(hWnd, uMsg.intValue(), wParam, lParam);
        }
    }

    private void registerClass() {
        WinUser.WNDCLASSEX wndClassEx = new WinUser.WNDCLASSEX();
        wndClassEx.lpfnWndProc = new WindowProc();
        wndClassEx.cbClsExtra = 0;
        wndClassEx.cbWndExtra = 0;
        wndClassEx.style = 0;
        wndClassEx.lpszClassName = className;
        wndClassEx.hInstance = null;
        wndClassEx.hCursor = null;
        wndClassEx.hIcon = null;
        wndClassEx.hbrBackground = null;
        wndClassEx.lpszMenuName = null;
        User32.INSTANCE.RegisterClassEx(wndClassEx);
    }

    private void init() {
        registerClass();
        // WinDef.HWND hWnd = new WinDef.HWND(Native.getComponentPointer(frame)); 或者可以传入frame窗口
        hWnd = User32Util.createWindowEx(0, className, className,
                WinUser.WS_OVERLAPPEDWINDOW, 0, 0, 0, 0, null, null, null, null);
        if (hWnd == null) {
            return;
        }
        User32.INSTANCE.ShowWindow(hWnd, WinUser.SW_HIDE);
        User32.INSTANCE.UpdateWindow(hWnd);
        WinUser.MSG msg = new WinUser.MSG();
        while (User32.INSTANCE.PeekMessage(msg, hWnd, 0, 0, 0)) {
            User32.INSTANCE.TranslateMessage(msg);
            User32.INSTANCE.DispatchMessage(msg);
        }
    }

    private void handleCopyDataMsg(WinDef.HWND hWnd, WinDef.UINT uMsg, WinDef.WPARAM wParam,
                                   WinDef.LPARAM lParam) {
        WinUser.COPYDATASTRUCT copyDataStruct =
                new WinUser.COPYDATASTRUCT(new Pointer(lParam.longValue()));
        Pointer pointer = copyDataStruct.lpData;
        String message = pointer.getWideString(0);
        System.out.println(message);
    }

    public void sendCopyDataMsg(String message) {
        WinDef.HWND hWnd = User32.INSTANCE.FindWindow(null, toWindowName);
        if (hWnd != null) {
            Pointer memory = new Memory(message.length() * 3 + 1);
            long memoPeer = Pointer.nativeValue(memory);
            memory.setWideString(0, message);
            WinUser.COPYDATASTRUCT copyDataStruct = new WinUser.COPYDATASTRUCT();
            copyDataStruct.dwData = new BaseTSD.ULONG_PTR(0);
            copyDataStruct.cbData = message.length() * 2 + 1;
            copyDataStruct.lpData = memory;
            copyDataStruct.write();
            Pointer pointer = copyDataStruct.getPointer();
            long peer = Pointer.nativeValue(pointer);
            User32.INSTANCE.SendMessage(hWnd, WM_COPYDATA, new WinDef.WPARAM(0),
                    new WinDef.LPARAM(peer));
            // 手动释放内存
            Native.free(memoPeer);
            Native.free(peer);
            // 避免Memory对象被GC时重复执行Native.free()方法
            Pointer.nativeValue(pointer, 0);
            Pointer.nativeValue(memory, 0);
        }
    }

    @Override
    public void run() {
        init();
    }
}