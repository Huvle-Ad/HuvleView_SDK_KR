import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'HuvleSDK Flutter',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const HuvleSamplePage(),
    );
  }
}

class HuvleSamplePage extends StatelessWidget {
  const HuvleSamplePage({Key? key}) : super(key: key);

  static const _channel = MethodChannel('com.huvle.sdk/huvle');

  void _onHuvleOn() {
    _channel.invokeMethod('notiUpdate');
  }

  void _onHuvleOff() {
    _channel.invokeMethod('notiCancel');
  }

  void _onOpenBrowser() {
    _channel.invokeMethod('openBrowser');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFFAFAFA),
      appBar: AppBar(
        title: const Text(
          'Huvle SDK Sample',
          style: TextStyle(fontWeight: FontWeight.w800, letterSpacing: -0.5),
        ),
        centerTitle: true,
        elevation: 3,
      ),
      body: Center(
        child: Padding(
          padding: const EdgeInsets.only(top: 40),
          child: Column(
            children: [
              _buildButton(
                label: 'Huvle ON / Update',
                color: const Color(0xFF388E3C),
                onPressed: _onHuvleOn,
              ),
              const SizedBox(height: 16),
              _buildButton(
                label: 'Huvle OFF',
                color: const Color(0xFFD32F2F),
                onPressed: _onHuvleOff,
              ),
              const SizedBox(height: 16),
              _buildButton(
                label: 'Open Huvle Browser',
                color: const Color(0xFF1976D2),
                onPressed: _onOpenBrowser,
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildButton({
    required String label,
    required Color color,
    required VoidCallback onPressed,
  }) {
    return SizedBox(
      width: 280,
      height: 50,
      child: ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: color,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          elevation: 4,
        ),
        child: Text(
          label,
          style: const TextStyle(fontSize: 15, fontWeight: FontWeight.w700, letterSpacing: 0.5),
        ),
      ),
    );
  }
}
