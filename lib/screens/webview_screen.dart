import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:webview_flutter/webview_flutter.dart';
import '../services/user_service.dart';
import '../services/notification_service.dart';

class WebViewScreen extends StatefulWidget {
  const WebViewScreen({super.key});

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  late final WebViewController _controller;
  bool _isLoading = true;

  @override
  void initState() {
    super.initState();
    _initializeWebView();
    _setFullScreen();
  }

  void _setFullScreen() {
    // Keep system bars visible but with clean styling
    SystemChrome.setEnabledSystemUIMode(
      SystemUiMode.manual,
      overlays: SystemUiOverlay.values, // Show both status bar and navigation bar
    );
    
    // Set clean system UI overlay style
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.white,
        statusBarIconBrightness: Brightness.dark,
        systemNavigationBarColor: Colors.white,
        systemNavigationBarIconBrightness: Brightness.dark,
      ),
    );
  }

  @override
  void dispose() {
    // Restore system UI when leaving the screen
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.manual, overlays: SystemUiOverlay.values);
    super.dispose();
  }

  void _initializeWebView() {
    _controller = WebViewController()
      ..setJavaScriptMode(JavaScriptMode.unrestricted)
      ..setBackgroundColor(const Color(0x00000000))
      ..addJavaScriptChannel(
        'Flutter',
        onMessageReceived: (JavaScriptMessage message) async {
          await _handleWebMessage(message.message);
        },
      )
      ..setNavigationDelegate(
        NavigationDelegate(
          onProgress: (int progress) {
            // Update loading bar
          },
          onPageStarted: (String url) {
            setState(() {
              _isLoading = true;
            });
          },
          onPageFinished: (String url) {
            setState(() {
              _isLoading = false;
            });
          },
          onWebResourceError: (WebResourceError error) {
            // Handle errors silently in fullscreen mode
            setState(() {
              _isLoading = false;
            });
          },
        ),
      )
      ..loadRequest(Uri.parse('https://stopusing.klr.kr'));
  }

  Future<void> _handleWebMessage(String message) async {
    try {
      // Parse the message as JSON
      final Map<String, dynamic> data = jsonDecode(message);

      // Check if this is a SET_USER_UID action
      if (data['action'] == 'SET_USER_UID' && data['data'] != null) {
        final userUid = data['data'] as String;
        await _saveUserUid(userUid);
        
        // Send response back to web
        await _sendResponseToWeb({
          'data': true,
          'action': null,
        });
      }
    } catch (e) {
      // Handle parsing errors - ignore for now as this might not be JSON
    }
  }

  Future<void> _saveUserUid(String userUid) async {
    await UserService.instance.saveUserUid(userUid);
    // Update userUid in Android native code
    await NotificationService.instance.updateUserUid();
  }

  Future<void> _sendResponseToWeb(Map<String, dynamic> response) async {
    try {
      final jsonString = jsonEncode(response);
      await _controller.runJavaScript('window.postMessage($jsonString, "*");');
    } catch (e) {
      // Handle error silently
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      // Remove app bar completely
      body: SafeArea(
        // Adjust webview size to fit between system bars
        child: Stack(
          children: [
            // Webview that fits perfectly between status bar and navigation bar
            WebViewWidget(controller: _controller),
            
            // Loading indicator
            if (_isLoading)
              Container(
                color: Colors.white,
                child: const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      CircularProgressIndicator(),
                      SizedBox(height: 16),
                      Text(
                        '그만써! 로딩 중...',
                        style: TextStyle(
                          fontSize: 16,
                          color: Colors.grey,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}