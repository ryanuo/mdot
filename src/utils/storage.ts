import AsyncStorage from '@react-native-async-storage/async-storage';

const POINTS_KEY = 'user_points';

export async function savePoints(points: any[]) {
  await AsyncStorage.setItem(POINTS_KEY, JSON.stringify(points));
}

export async function loadPoints(): Promise<any[]> {
  const data = await AsyncStorage.getItem(POINTS_KEY);
  if (!data) return [];
  return JSON.parse(data);
}
