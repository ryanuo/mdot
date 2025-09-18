import React, { useEffect, useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View, ScrollView, AppState } from 'react-native';
import pointsData from '../config/points.json';
import { loadPoints} from '../utils/storage';
import { FloatingClickerNative } from '../native/FloatingClicker';

interface Point {
  id: string;
  x: number;
  y: number;
}

export const HomeScreen = () => {
  const [points, setPoints] = useState<Point[]>([]);
  const [isFloatingActive, setIsFloatingActive] = useState(false);
  const [hasOverlayPermission, setHasOverlayPermission] = useState(false);
  const [hasAccessibilityPermission, setHasAccessibilityPermission] = useState(false);

  useEffect(() => {
    (async () => {
      const saved = await loadPoints();
      setPoints(saved.length ? saved : pointsData);
      await checkPermissions();
    })();
  }, []);

  // 监听应用状态变化，当用户从设置页面返回时重新检测权限
  useEffect(() => {
    const handleAppStateChange = (nextAppState: string) => {
      if (nextAppState === 'active') {
        // 应用重新激活时重新检测权限
        checkPermissions();
      }
    };

    const subscription = AppState.addEventListener('change', handleAppStateChange);
    return () => subscription?.remove();
  }, []);

  const checkPermissions = async () => {
    try {
      const overlayPermission = await FloatingClickerNative.checkOverlayPermission();
      const accessibilityPermission = await FloatingClickerNative.checkAccessibilityPermission();
      setHasOverlayPermission(overlayPermission);
      setHasAccessibilityPermission(accessibilityPermission);
    } catch (error) {
      console.error('Error checking permissions:', error);
    }
  };

  const requestOverlayPermission = async () => {
    try {
      await FloatingClickerNative.requestOverlayPermission();
      Alert.alert(
        '权限请求',
        '请在设置中允许显示在其他应用的上层\n\n设置完成后返回应用，权限状态将自动更新',
        [
          { text: '确定', onPress: () => { } },
          { text: '重新检测', onPress: checkPermissions }
        ]
      );
    } catch (error) {
      Alert.alert('错误', '无法请求悬浮窗权限');
    }
  };

  const requestAccessibilityPermission = async () => {
    try {
      await FloatingClickerNative.requestAccessibilityPermission();
      Alert.alert(
        '权限请求',
        '请在无障碍设置中启用 MultiPoint Floating Clicker\n\n设置完成后返回应用，权限状态将自动更新',
        [
          { text: '确定', onPress: () => { } },
          { text: '重新检测', onPress: checkPermissions }
        ]
      );
    } catch (error) {
      Alert.alert('错误', '无法请求无障碍权限');
    }
  };

  const disableAccessibilityService = async () => {
    try {
      await FloatingClickerNative.disableAccessibilityService();
      Alert.alert(
        '关闭无障碍服务',
        '已跳转到无障碍设置页面\n\n请在设置中关闭 MultiPoint Floating Clicker 服务',
        [
          { text: '确定', onPress: () => { } },
          { text: '重新检测', onPress: checkPermissions }
        ]
      );
    } catch (error) {
      Alert.alert('错误', '无法关闭无障碍服务');
    }
  };

  const disableOverlayPermission = async () => {
    try {
      await FloatingClickerNative.disableOverlayPermission();
    } catch (error) {
      Alert.alert('错误', '无法关闭悬浮窗权限');
    }
  };

  const startFloatingWindow = async () => {
    // 启动前重新检测权限状态
    await checkPermissions();

    if (!hasOverlayPermission) {
      Alert.alert('需要权限', '请先授予悬浮窗权限', [
        { text: '取消', style: 'cancel' },
        { text: '去设置', onPress: requestOverlayPermission }
      ]);
      return;
    }
    if (!hasAccessibilityPermission) {
      Alert.alert('需要权限', '请先启用无障碍服务', [
        { text: '取消', style: 'cancel' },
        { text: '去设置', onPress: requestAccessibilityPermission }
      ]);
      return;
    }

    try {
      await FloatingClickerNative.startFloatingWindow();
      setIsFloatingActive(true);
    } catch (error) {
      Alert.alert('错误', '无法启动悬浮窗');
    }
  };

  const stopFloatingWindow = async () => {
    try {
      await FloatingClickerNative.stopFloatingWindow();
      setIsFloatingActive(false);
      Alert.alert('成功', '悬浮窗已停止');
    } catch (error: any) {
      Alert.alert('错误', `无法停止悬浮窗: ${error.message}`);
    }
  };

  const handleTriggerAll = async () => {
    if (isFloatingActive) {
      try {
        await FloatingClickerNative.triggerMultipleClicks(points);
        Alert.alert('触发', `已触发所有 ${points.length} 个点位`);
      } catch (error: any) {
        console.error('Multiple clicks error:', error);
        Alert.alert('错误', `无法触发所有点位: ${error.message || '未知错误'}`);
      }
    } else {
      Alert.alert('触发', '触发所有点位');
    }
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>MultiPoint Floating Clicker</Text>
        <Text style={styles.subtitle}>配置和管理悬浮点击点位</Text>
      </View>

      <View style={styles.permissionSection}>
        <View style={styles.permissionHeader}>
          <Text style={styles.sectionTitle}>权限状态</Text>
          <TouchableOpacity style={styles.refreshButton} onPress={checkPermissions}>
            <Text style={styles.refreshButtonText}>🔄 重新检测</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.permissionItem}>
          <Text style={styles.permissionLabel}>悬浮窗权限:</Text>
          <Text style={[styles.permissionStatus, hasOverlayPermission ? styles.green : styles.red]}>
            {hasOverlayPermission ? '已授予' : '未授予'}
          </Text>
          {!hasOverlayPermission ? (
            <TouchableOpacity style={styles.permissionButton} onPress={requestOverlayPermission}>
              <Text style={styles.buttonText}>请求权限</Text>
            </TouchableOpacity>
          ) : <TouchableOpacity style={styles.disableButton} onPress={disableOverlayPermission}>
            <Text style={styles.buttonText}>关闭服务</Text>
          </TouchableOpacity>}
        </View>

        <View style={styles.permissionItem}>
          <Text style={styles.permissionLabel}>无障碍权限:</Text>
          <Text style={[styles.permissionStatus, hasAccessibilityPermission ? styles.green : styles.red]}>
            {hasAccessibilityPermission ? '已启用' : '未启用'}
          </Text>
          {!hasAccessibilityPermission ? (
            <TouchableOpacity style={styles.permissionButton} onPress={requestAccessibilityPermission}>
              <Text style={styles.buttonText}>启用服务</Text>
            </TouchableOpacity>
          ) : (
            <TouchableOpacity style={styles.disableButton} onPress={disableAccessibilityService}>
              <Text style={styles.buttonText}>关闭服务</Text>
            </TouchableOpacity>
          )}
        </View>
      </View>

      <View style={styles.controlSection}>
        <Text style={styles.sectionTitle}>控制面板</Text>

        <View style={styles.buttonRow}>
          <TouchableOpacity
            style={[styles.controlButton, isFloatingActive ? styles.stopButton : styles.startButton]}
            onPress={isFloatingActive ? stopFloatingWindow : startFloatingWindow}
          >
            <Text style={styles.buttonText}>
              {isFloatingActive ? '停止悬浮窗' : '启动悬浮窗'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity style={styles.controlButton} onPress={handleTriggerAll}>
            <Text style={styles.buttonText}>触发所有点位</Text>
          </TouchableOpacity>
        </View>
      </View>
    </ScrollView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 20,
    backgroundColor: '#007AFF',
    alignItems: 'center',
  },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginBottom: 5,
  },
  subtitle: {
    fontSize: 16,
    color: '#fff',
    opacity: 0.9,
  },
  permissionSection: {
    margin: 15,
    padding: 15,
    backgroundColor: '#fff',
    borderRadius: 10,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.22,
    shadowRadius: 2.22,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 15,
    color: '#333',
  },
  permissionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 15,
  },
  refreshButton: {
    backgroundColor: '#34C759',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 15,
  },
  refreshButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  permissionItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 10,
    flexWrap: 'wrap',
  },
  permissionLabel: {
    fontSize: 16,
    color: '#333',
    marginRight: 10,
    minWidth: 100,
  },
  permissionStatus: {
    fontSize: 16,
    fontWeight: 'bold',
    marginRight: 10,
  },
  green: {
    color: '#34C759',
  },
  red: {
    color: '#FF3B30',
  },
  permissionButton: {
    backgroundColor: '#007AFF',
    paddingHorizontal: 15,
    paddingVertical: 8,
    borderRadius: 5,
  },
  disableButton: {
    backgroundColor: '#FF3B30',
    paddingHorizontal: 10,
    paddingVertical: 3,
    borderRadius: 5,
  },
  controlSection: {
    margin: 15,
    padding: 15,
    backgroundColor: '#fff',
    borderRadius: 10,
    elevation: 2,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.22,
    shadowRadius: 2.22,
  },
  buttonRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  controlButton: {
    flex: 1,
    padding: 15,
    borderRadius: 8,
    alignItems: 'center',
    marginHorizontal: 5,
  },
  startButton: {
    backgroundColor: '#34C759',
  },
  stopButton: {
    backgroundColor: '#FF3B30',
  },
  buttonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: 'bold',
  },
});
