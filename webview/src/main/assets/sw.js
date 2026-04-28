const CACHE_NAME = 'sensor-tester-v1';
const ASSETS = [
  './index.html',
  './manifest.json',
  'https://cdn.jsdelivr.net/npm/@ionic/core/css/ionic.bundle.css',
  'https://cdn.jsdelivr.net/npm/@ionic/core/dist/ionic/ionic.esm.js',
  'https://cdn.jsdelivr.net/npm/ionicons@7.1.0/dist/ionicons/ionicons.esm.js'
];

// Install Event
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(ASSETS);
    })
  );
});

// Fetch Event
self.addEventListener('fetch', event => {
  event.respondWith(
    caches.match(event.request).then(response => {
      return response || fetch(event.request);
    })
  );
});
