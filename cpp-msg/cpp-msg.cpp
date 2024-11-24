#include <windows.h>
#include <string>
#include <tchar.h>
#include <iostream>

const TCHAR WINDOW_CLASS_NAME[] = _T("CPP_MSG");
const TCHAR WINDOW_TITLE[] = _T("Win32 Message Handler");

LRESULT CALLBACK WindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam);
void SendMessageToJavaWindow(const std::wstring& message);

int APIENTRY WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow)
{
	WNDCLASS wc = {};
	wc.lpfnWndProc = WindowProc;
	wc.hInstance = hInstance;
	wc.lpszClassName = WINDOW_CLASS_NAME;
	RegisterClass(&wc);

	HWND hwnd = CreateWindowEx(
		0,
		WINDOW_CLASS_NAME,
		WINDOW_TITLE,
		WS_OVERLAPPEDWINDOW,
		CW_USEDEFAULT, CW_USEDEFAULT, 500, 400,
		nullptr, nullptr, hInstance, nullptr
	);

	if (!hwnd) return 0;

	// 创建按钮
	HWND hButton = CreateWindow(
		_T("BUTTON"),  // 按钮类名
		_T("点我发送消息给java-msg"),  // 按钮标题
		WS_VISIBLE | WS_CHILD,  // 按钮样式
		50, 50, 300, 50,  // 位置和大小
		hwnd,  // 父窗口
		(HMENU)1,  // 按钮ID
		hInstance,
		nullptr
	);

	ShowWindow(hwnd, nCmdShow);
	UpdateWindow(hwnd);

	MSG msg = {};
	while (GetMessage(&msg, nullptr, 0, 0)) {
		TranslateMessage(&msg);
		DispatchMessage(&msg);
	}

	return 0;
}

LRESULT CALLBACK WindowProc(HWND hwnd, UINT uMsg, WPARAM wParam, LPARAM lParam) {
	switch (uMsg)
	{
	case WM_COMMAND:
		if (LOWORD(wParam) == 1) { // 检查按钮ID
			// 按钮点击事件处理
			SendMessageToJavaWindow(L"{\"msg\":\"来自cpp-msg的消息\",\"className\":\"CPP_MSG\"}");
		}
		break;
	case WM_COPYDATA:
	{
		PCOPYDATASTRUCT pCopyData = reinterpret_cast<PCOPYDATASTRUCT>(lParam);
		if (pCopyData && pCopyData->lpData)
		{
			std::wstring receivedMessage(reinterpret_cast<wchar_t*>(pCopyData->lpData));
			MessageBox(hwnd, receivedMessage.c_str(), _T("Message Received"), MB_OK);
		}
		return TRUE;
	}
	case WM_DESTROY:
		PostQuitMessage(0);
		return 0;
	}
	return DefWindowProc(hwnd, uMsg, wParam, lParam);
}

void SendMessageToJavaWindow(const std::wstring& message)
{
	HWND hJavaWindow = FindWindow(nullptr, _T("JAVA_MSG"));
	if (hJavaWindow)
	{
		COPYDATASTRUCT copyData = {};
		copyData.cbData = (message.size() + 1) * sizeof(wchar_t);
		copyData.lpData = (void*)message.c_str();
		SendMessage(hJavaWindow, WM_COPYDATA, 0, (LPARAM)&copyData);
	}
}