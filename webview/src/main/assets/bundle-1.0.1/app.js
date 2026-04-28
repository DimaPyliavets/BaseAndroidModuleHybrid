let value = Number(localStorage.getItem("value")) || 0;
let limit = Number(localStorage.getItem("limit")) || 20;
let history = JSON.parse(localStorage.getItem("history")) || [];

const circle = document.getElementById("progressCircle");
const valueText = document.getElementById("value");
const limitText = document.getElementById("limitText");

function updateUI() {
  valueText.textContent = value;
  limitText.textContent = "of " + limit;

  const percent = value / limit;
  const offset = 565 - 565 * percent;

  circle.style.strokeDashoffset = offset;
}

function setLimit() {
  const newLimit = prompt("Enter limit");
  if (!newLimit) return;

  limit = Number(newLimit);
  value = 0;

  save();
  updateUI();
}

function useOne() {
  if (value <= 0) return;

  value--;

  history.push({
    date: new Date().toLocaleDateString(),
    value: value,
  });

  save();
  updateUI();
  renderHistory();
}

function renderHistory() {
  const container = document.getElementById("history");
  container.innerHTML = "";

  history
    .slice()
    .reverse()
    .forEach((item) => {
      const div = document.createElement("div");
      div.className = "card";

      div.innerHTML = `
      <div>${item.date}</div>
      <div>${item.value} left</div>
    `;

      container.appendChild(div);
    });
}

function switchTab(tab) {
  document
    .querySelectorAll(".screen")
    .forEach((s) => s.classList.remove("active"));

  document.getElementById("screen-" + tab).classList.add("active");
}

function save() {
  localStorage.setItem("value", value);
  localStorage.setItem("limit", limit);
  localStorage.setItem("history", JSON.stringify(history));
}

updateUI();
renderHistory();
