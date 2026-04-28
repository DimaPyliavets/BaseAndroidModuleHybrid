import { useEffect, useState } from "react";

export function useSensors() {
  const [data, setData] = useState({ x: 0, y: 0, z: 0 });

  useEffect(() => {
    const handler = (e) => {
      setData({
        x: e.accelerationIncludingGravity.x,
        y: e.accelerationIncludingGravity.y,
        z: e.accelerationIncludingGravity.z,
      });
    };

    window.addEventListener("devicemotion", handler);

    return () => window.removeEventListener("devicemotion", handler);
  }, []);

  return data;
}
