import React from "react";
import Card from "./Card";
import { useSensors } from "../hooks/useSensors";

export default function SensorDashboard() {
  const { x, y, z } = useSensors();

  return (
    <Card>
      <h2>Sensors</h2>

      <div>X: {x?.toFixed(2)}</div>
      <div>Y: {y?.toFixed(2)}</div>
      <div>Z: {z?.toFixed(2)}</div>
    </Card>
  );
}
