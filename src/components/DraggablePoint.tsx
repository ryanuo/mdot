import React, { useState, useEffect } from 'react';
import { Animated, PanResponder, StyleSheet, Text, View } from 'react-native';
import { FloatingPoint } from '../native/FloatingClicker';

interface PointProps {
  point: FloatingPoint;
  onPositionChange?: (point: FloatingPoint) => void;
  onPress?: (point: FloatingPoint) => void;
  isActive?: boolean;
}

export const DraggablePoint: React.FC<PointProps> = ({ 
  point, 
  onPositionChange, 
  onPress, 
  isActive = false 
}) => {
  const [position] = useState(new Animated.ValueXY({ x: point.x, y: point.y }));
  const [isDragging, setIsDragging] = useState(false);

  useEffect(() => {
    position.setValue({ x: point.x, y: point.y });
  }, [point.x, point.y]);

  const panResponder = PanResponder.create({
    onStartShouldSetPanResponder: () => true,
    onMoveShouldSetPanResponder: () => true,
    onPanResponderGrant: () => {
      setIsDragging(true);
    },
    onPanResponderMove: (_, gestureState) => {
      const newX = point.x + gestureState.dx;
      const newY = point.y + gestureState.dy;
      position.setValue({ x: newX, y: newY });
    },
    onPanResponderRelease: (_, gestureState) => {
      setIsDragging(false);
      const newX = point.x + gestureState.dx;
      const newY = point.y + gestureState.dy;
      
      const updatedPoint = { ...point, x: newX, y: newY };
      
      if (Math.abs(gestureState.dx) < 5 && Math.abs(gestureState.dy) < 5) {
        // It's a tap, not a drag
        onPress?.(updatedPoint);
      } else {
        // It's a drag
        onPositionChange?.(updatedPoint);
      }
    },
  });

  return (
    <Animated.View
      {...panResponder.panHandlers}
      style={[
        styles.point, 
        position.getLayout(),
        isActive && styles.activePoint,
        isDragging && styles.draggingPoint
      ]}
    >
      <Text style={styles.pointText}>{point.id}</Text>
      {isActive && <View style={styles.activeIndicator} />}
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
    elevation: 5,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
  },
  activePoint: {
    backgroundColor: '#FF3B30',
    borderWidth: 3,
    borderColor: '#FFD700',
  },
  draggingPoint: {
    backgroundColor: '#34C759',
    transform: [{ scale: 1.1 }],
  },
  pointText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  activeIndicator: {
    position: 'absolute',
    top: -5,
    right: -5,
    width: 15,
    height: 15,
    backgroundColor: '#FFD700',
    borderRadius: 7.5,
    borderWidth: 2,
    borderColor: '#fff',
  },
});
