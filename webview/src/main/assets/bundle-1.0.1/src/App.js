import React, { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import BottomNav from "./components/BottomNav";
import CameraTest from "./components/CameraTest";
import AudioTest from "./components/AudioTest";
import NotificationTest from "./components/NotificationTest";
import SensorDashboard from "./components/SensorDashboard";

const TABS = ["camera", "audio", "notifications", "sensors"];

export default function App() {
  const [tab, setTab] = useState("camera");

  const renderTab = () => {
    switch (tab) {
      case "camera":
        return <CameraTest />;
      case "audio":
        return <AudioTest />;
      case "notifications":
        return <NotificationTest />;
      case "sensors":
        return <SensorDashboard />;
      default:
        return null;
    }
  };

  return (
    <div className="app-container">
      <AnimatePresence mode="wait">
        <motion.div
          key={tab}
          initial={{ opacity: 0, transform: "translateY(10px)" }}
          animate={{ opacity: 1, transform: "translateY(0px)" }}
          exit={{ opacity: 0, transform: "translateY(-10px)" }}
          transition={{ duration: 0.2 }}
          className="content"
        >
          {renderTab()}
        </motion.div>
      </AnimatePresence>

      <BottomNav active={tab} onChange={setTab} />
    </div>
  );
}
