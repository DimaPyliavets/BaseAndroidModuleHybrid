import { Camera, Volume2, Bell, Activity } from "lucide-react";

export default function BottomNav({ active, onChange }) {
  const items = [
    { id: "camera", icon: <Camera /> },
    { id: "audio", icon: <Volume2 /> },
    { id: "notifications", icon: <Bell /> },
    { id: "sensors", icon: <Activity /> },
  ];

  return (
    <div className="bottom-nav">
      {items.map((i) => (
        <button
          key={i.id}
          className={active === i.id ? "active" : ""}
          onClick={() => onChange(i.id)}
        >
          {i.icon}
        </button>
      ))}
    </div>
  );
}
