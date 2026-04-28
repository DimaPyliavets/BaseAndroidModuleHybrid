import React, { useState } from "react";
import Card from "./Card";

export default function AudioTest() {
  const [ctx, setCtx] = useState(null);
  const [osc, setOsc] = useState(null);

  const play = (freq) => {
    const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    const oscillator = audioCtx.createOscillator();

    oscillator.type = "sine";
    oscillator.frequency.value = freq;

    oscillator.connect(audioCtx.destination);
    oscillator.start();

    setCtx(audioCtx);
    setOsc(oscillator);
  };

  const stop = () => {
    osc?.stop();
    ctx?.close();
  };

  return (
    <Card>
      <h2>Speaker Test</h2>

      <div className="row">
        <button onClick={() => play(200)}>Low</button>
        <button onClick={() => play(2000)}>High</button>
        <button onClick={stop}>Stop</button>
      </div>
    </Card>
  );
}
