import React, { useRef, useState } from "react";
import Card from "./Card";
import { usePermissions } from "../hooks/usePermissions";

export default function CameraTest() {
  const videoRef = useRef();
  const [stream, setStream] = useState(null);
  const { status, requestCamera } = usePermissions();

  const startCamera = async () => {
    await requestCamera();
    const s = await navigator.mediaDevices.getUserMedia({ video: true });
    videoRef.current.srcObject = s;
    setStream(s);
  };

  return (
    <Card>
      <h2>Camera Test</h2>

      {status !== "granted" && (
        <button onClick={startCamera}>Enable Camera</button>
      )}

      <video ref={videoRef} autoPlay playsInline className="video" />

      <button onClick={() => console.log("Capture simulated")}>Capture</button>
    </Card>
  );
}
