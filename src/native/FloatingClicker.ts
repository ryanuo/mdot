import { NativeModules, NativeEventEmitter } from 'react-native';

export interface FloatingPoint {
  id: string;
  x: number;
  y: number;
}

export interface FloatingClickerModule {
  startFloatingWindow(): Promise<boolean>;
  stopFloatingWindow(): Promise<boolean>;
  checkOverlayPermission(): Promise<boolean>;
  requestOverlayPermission(): Promise<boolean>;
  checkAccessibilityPermission(): Promise<boolean>;
  requestAccessibilityPermission(): Promise<boolean>;
  disableAccessibilityService(): Promise<boolean>;
  disableOverlayPermission(): Promise<boolean>;
  triggerClick(x: number, y: number): Promise<boolean>;
  triggerMultipleClicks(points: FloatingPoint[]): Promise<boolean>;
}

const { FloatingClicker } = NativeModules;

export const FloatingClickerNative: FloatingClickerModule = FloatingClicker;

export const FloatingClickerEvents = new NativeEventEmitter(FloatingClicker);
