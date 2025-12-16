<p >
  <img src="app/src/main/launch-playstore.png" alt="Thor Logo" height="192dp">
</p>

<h1>Brokk - The Assembler</h1>

<div class="badges">
<img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
<img src="https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
<img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white" alt="Compose">
<img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge" alt="License">
</div>

<p><strong>Brokk</strong> is a ruthless, no-nonsense package installer for Android. It bridges the gap between modern split-APK formats (<code>.xapk</code>, <code>.apks</code>) and the system's <code>PackageInstaller</code>.</p>

<p>Unlike bloatware file managers that struggle with split binaries, Brokk parses, extracts, and streams installation data directly to the OS session using a clean, reactive architecture.</p>

<h2>⚡ Features</h2>
<ul>
<li><strong>Universal Installer:</strong> Handles standard <code>.apk</code>, split <code>.apks</code>, and bundled <code>.xapk</code> (Zip-based) files.</li>
<li><strong>Portable Mode:</strong> A floating bottom-sheet interface that layers over your file manager or browser. Install apps without leaving your current context.</li>
<li><strong>Smart Analysis:</strong> Pre-parses the package stream to display the App Icon, Name, Version, and Package ID before installation.</li>
<li><strong>Update Detection:</strong> Automatically detects if the app is already installed and switches context to "Update" mode.</li>
<li><strong>Rootless:</strong> Works entirely within standard Android <code>PackageInstaller</code> APIs. No root required.</li>
<li><strong>Material You:</strong> Fully adaptive UI using Jetpack Compose and Material 3.</li>
</ul>

<h2>🛠 Tech Stack</h2>
<p>Brokk is built using <strong>Clean Architecture</strong> and the modern Android development stack:</p>
<ul>
<li><strong>Language:</strong> Kotlin</li>
<li><strong>UI:</strong> Jetpack Compose (Material 3)</li>
<li><strong>DI:</strong> Koin</li>
<li><strong>Async:</strong> Kotlin Coroutines & Flow</li>
<li><strong>Architecture:</strong> MVVM + Clean (Domain, Data, Presentation layers)</li>
<li><strong>State Management:</strong> Reactive EventBus (SharedFlow)</li>
</ul>

<h2>🚀 How It Works</h2>
<p>Brokk bypasses the need to extract huge XAPK files to the file system (which wastes space and hits IO limits).</p>
<ol>
<li><strong>Stream Interception:</strong> It opens an <code>InputStream</code> from the <code>ContentResolver</code>.</li>
<li><strong>On-the-fly Parsing:</strong> It iterates through the Zip stream in memory, identifying valid <code>.apk</code> splits.</li>
<li><strong>Direct Piping:</strong> It opens a <code>PackageInstaller.Session</code> and pipes the bytes directly from the source file to the system installer.</li>
<li><strong>Verification:</strong> It listens for the <code>ACTION_PACKAGE_ADDED</code> broadcast to verify the install was successful and offers to launch the app immediately.</li>
</ol>

<h2>🔮 Roadmap & Upcoming Features</h2>
<p>We are currently forging the next set of tools for Brokk:</p>
<ul>
<li class="task-list-item"><input type="checkbox" disabled> <strong>App Sharing (The Bifrost Protocol):</strong>
<ul>
<li>Extract installed apps back into <code>.xapk</code> or <code>.apks</code> format.</li>
<li>Share apps directly to other devices.</li>
</ul>
</li>
<li class="task-list-item"><input type="checkbox" disabled> <strong>OBB Support:</strong> Handling legacy OBB expansion files for older games.</li>
<li class="task-list-item"><input type="checkbox" disabled> <strong>Batch Installer:</strong> Queue multiple installations.</li>
</ul>

<h2>📦 Installation</h2>
<p>Brokk is FOSS. You can download the latest APK from the <a href="https://github.com/trinadhthatakula/Brokk/releases">Releases</a> page or build it from source.</p>

<h3>Build from Source</h3>
<pre><code>git clone https://github.com/trinadhthatakula/Brokk.git
cd Brokk
./gradlew assembleDebug</code></pre>

<h2>🤝 Contributing</h2>
<p>Contributions are welcome. If you find a bug or have a feature request, please open an issue.</p>
<ol>
<li>Fork the Project</li>
<li>Create your Feature Branch (<code>git checkout -b feature/AmazingFeature</code>)</li>
<li>Commit your Changes (<code>git commit -m 'Add some AmazingFeature'</code>)</li>
<li>Push to the Branch (<code>git push origin feature/AmazingFeature</code>)</li>
<li>Open a Pull Request</li>
</ol>

<h2>📄 License</h2>
<p>Distributed under the Apache 2.0 License. See <code>LICENSE</code> for more information.</p>

<footer>
<p>Built with 🔨 by <strong>Trinadh Thatakula</strong></p>
</footer>
