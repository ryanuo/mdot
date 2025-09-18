import React, { useEffect, useState } from 'react';
import { Alert, StyleSheet, Text, TouchableOpacity, View } from 'react-native';
import { DraggablePoint } from '../components/DraggablePoint';
import pointsData from '../config/points.json';
import { loadPoints } from '../utils/storage';

interface Point {
  id: string;
  x: number;
  y: number;
}

export const HomeScreen = () => {
  const [points, setPoints] = useState<Point[]>([]);

  useEffect(() => {
    (async () => {
      const saved = await loadPoints();
      setPoints(saved.length ? saved : pointsData);
    })();
  }, []);

  const handleClickArea = () => {
    points.forEach(p => {
      Alert.alert(`触发点位: ${p.id}`);
    });
  };

  const handlePointClick = (id: string) => {
    Alert.alert(`点击了单个点位: ${id}`);
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.area} onPress={handleClickArea}>
        <Text style={{ color: '#fff' }}>点击这里触发所有点位</Text>
      </TouchableOpacity>

      {points.map(p => (
        <DraggablePoint
          key={p.id}
          id={p.id}
          x={p.x}
          y={p.y}
          onClick={handlePointClick}
        />
      ))}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  area: {
    position: 'absolute',
    bottom: 100,
    left: 50,
    right: 50,
    height: 60,
    backgroundColor: '#FF3B30',
    justifyContent: 'center',
    alignItems: 'center',
    borderRadius: 10,
    zIndex: 1,
  },
});
