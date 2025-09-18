import React, { useState } from 'react';
import { Animated, PanResponder, StyleSheet, Text } from 'react-native';

interface PointProps {
  id: string;
  x: number;
  y: number;
  onClick?: (id: string) => void;
}

export const DraggablePoint: React.FC<PointProps> = ({ id, x, y, onClick }) => {
  const [position] = useState(new Animated.ValueXY({ x, y }));

  const panResponder = PanResponder.create({
    onStartShouldSetPanResponder: () => true,
    onPanResponderMove: (_, gestureState) => {
      position.setValue({ x: x + gestureState.dx, y: y + gestureState.dy });
    },
    onPanResponderRelease: (_, gestureState) => {
      if (Math.abs(gestureState.dx) < 5 && Math.abs(gestureState.dy) < 5) {
        onClick?.(id);
      }
    },
  });

  return (
    <Animated.View
      {...panResponder.panHandlers}
      style={[styles.point, position.getLayout()]}
    >
      <Text style={{ color: '#fff' }}>{id}</Text>
    </Animated.View>
  );
};

const styles = StyleSheet.create({
  point: {
    width: 60,
    height: 60,
    backgroundColor: '#007AFF',
    borderRadius: 30,
    position: 'absolute',
    justifyContent: 'center',
    alignItems: 'center',
  },
});
