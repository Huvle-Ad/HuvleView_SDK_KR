import React from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  NativeModules,
} from 'react-native';

const { BrowserModule } = NativeModules;

function App(): React.JSX.Element {
  const onHuvleON = () => {
    BrowserModule.openNotificationSettings();
  };

  const onHuvleOFF = () => {
    BrowserModule.turnOffNotification();
  };

  const onOpenBrowser = () => {
    BrowserModule.openSapMainActivity();
  };

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Huvle SDK Sample</Text>
      </View>
      <View style={styles.content}>
        <TouchableOpacity style={[styles.button, styles.btnOn]} onPress={onHuvleON}>
          <Text style={styles.buttonText}>Huvle ON / Update</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.button, styles.btnOff]} onPress={onHuvleOFF}>
          <Text style={styles.buttonText}>Huvle OFF</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.button, styles.btnBrowser]} onPress={onOpenBrowser}>
          <Text style={styles.buttonText}>Open Huvle Browser</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FAFAFA',
  },
  header: {
    paddingVertical: 25,
    paddingHorizontal: 20,
    alignItems: 'center',
    backgroundColor: '#FFFFFF',
    elevation: 3,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.1,
    shadowRadius: 2,
  },
  title: {
    fontSize: 22,
    fontWeight: '800',
    color: '#1A1A1A',
    letterSpacing: -0.5,
  },
  content: {
    flex: 1,
    alignItems: 'center',
    paddingTop: 40,
  },
  button: {
    width: '75%',
    paddingVertical: 14,
    borderRadius: 12,
    alignItems: 'center',
    marginBottom: 16,
    elevation: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 3 },
    shadowOpacity: 0.2,
    shadowRadius: 4.65,
  },
  btnOn: {
    backgroundColor: '#388E3C',
  },
  btnOff: {
    backgroundColor: '#D32F2F',
  },
  btnBrowser: {
    backgroundColor: '#1976D2',
  },
  buttonText: {
    color: '#FFF',
    fontSize: 15,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
});

export default App;