const content = document.getElementById("content");
const buttons = document.querySelectorAll(".bottom-nav button");

let currentStream = null;
let audioCtx = null;
let oscillator = null;

buttons.forEach(btn => {
  btn.addEventListener("click", () => {
    buttons.forEach(b => b.classList.remove("active"));
    btn.classList.add("active");

    loadTab(btn.dataset.tab);
  });
});

// ---------- Tabs ----------

function loadTab(tab) {
  content.innerHTML = "";
  content.classList.add("fade");

  switch (tab) {
    case "camera":
      renderCamera();
      break;
    case "audio":
      renderAudio();
      break;
    case "notifications":
      renderNotifications();
      break;
    case "sensors":
      renderSensors();
      break;
  }
}

// ---------- Camera ----------

function renderCamera() {
  const card = createCard("Camera Test");

  const btn = createButton("Enable Camera", async () => {
    try {
      currentStream = await navigator.mediaDevices.getUserMedia({ video: true });
      video.srcObject = currentStream;
    } catch {
      alert("Permission denied");
    }
  });

  const video = document.createElement("video");
  video.autoplay = true;
  video.playsInline = true;

  const capture = createButton("Capture", () => {
    console.log("Capture simulated");
  });

  card.append(btn, video, capture);
  content.appendChild(card);
}

// ---------- Audio ----------

function renderAudio() {
  const card = createCard("Speaker Test");

  const play = (freq) => {
    stopAudio();

    audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    oscillator = audioCtx.createOscillator();

    oscillator.type = "sine";
    oscillator.frequency.value = freq;

    oscillator.connect(audioCtx.destination);
    oscillator.start();
  };

  card.append(
    createButton("Low", () => play(200)),
    createButton("High", () => play(2000)),
    createButton("Stop", stopAudio)
  );

  content.appendChild(card);
}

function stopAudio() {
  oscillator?.stop();
  audioCtx?.close();
}

// ---------- Notifications ----------

function renderNotifications() {
  const card = createCard("Notification Test");

  const request = createButton("Request Permission", async () => {
    await Notification.requestPermission();
  });

  const trigger = createButton("Trigger Notification", () => {
    new Notification("Test Notification", {
      body: "It works",
    });
  });

  card.append(request, trigger);
  content.appendChild(card);
}

// ---------- Sensors ----------

function renderSensors() {
  const card = createCard("Sensor Dashboard");

  const x = document.createElement("div");
  const y = document.createElement("div");
  const z = document.createElement("div");

  window.addEventListener("devicemotion", (e) => {
    const acc = e.accelerationIncludingGravity;

    x.textContent = "X: " + acc.x.toFixed(2);
    y.textContent = "Y: " + acc.y.toFixed(2);
    z.textContent = "Z: " + acc.z.toFixed(2);
  });

  card.append(x, y, z);
  content.appendChild(card);
}

// ---------- Helpers ----------

function createCard(title) {
  const card = document.createElement("div");
  card.className = "card";

  const h = document.createElement("h2");
  h.textContent = title;

  card.appendChild(h);
  return card;
}

function createButton(text, handler) {
  const btn = document.createElement("button");
  btn.textContent = text;
  btn.onclick = handler;
  return btn;
}

// ---------- Init ----------

loadTab("camera");

// ---------- Service Worker ----------

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("/sw.js");
}