import { useState } from "react";

export function usePermissions() {
  const [status, setStatus] = useState("idle");

  const requestCamera = async () => {
    try {
      await navigator.mediaDevices.getUserMedia({ video: true });
      setStatus("granted");
    } catch {
      setStatus("denied");
    }
  };

  const requestNotifications = async () => {
    const res = await Notification.requestPermission();
    setStatus(res);
  };

  return { status, requestCamera, requestNotifications };
}
