# PunchHole Indicator 📱

An elegant Android status overlay that turns your hole-punch camera into a live status hub:
- **Outer Circular Arc**: Real-time battery indicator with adaptive charging colors (green for charging, red for low battery, white/cyan for normal).
- **Status Contour Dots**: Positioned neatly along the bottom curve of the camera cutout:
  - 🔵 **Wi-Fi**: Blue when active & connected
  - 🟢 **Cellular Data**: Green when connected to mobile network
  - 🔷 **Bluetooth**: Cyan/Blue when connected to a device
  - 🟠 **Silent / DND**: Orange when muted or on vibrate
- **Zero Local Setup Required**: You don't need Android Studio or gigabytes of SDKs. GitHub Actions builds the installable .apk for you in the cloud!

---

## 🚀 How to Build Your APK (Using GitHub Actions)

### Step 1: Create a GitHub Repository
1. Go to [github.com/new](https://github.com/new) in your browser.
2. Name your repository (e.g. punchhole-indicator).
3. Set it to **Public** or **Private** (both are completely free).
4. Click **Create repository**.

### Step 2: Push This Code to Your GitHub Repository
Open PowerShell or Terminal in this folder (C:\Users\kahaa\.gemini\antigravity\scratch\holepunch-indicator) and run:

`ash
git init
git add .
git commit -m "Initial commit of PunchHole Indicator app"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/punchhole-indicator.git
git push -u origin main
`
*(Replace YOUR_USERNAME with your GitHub username)*

### Step 3: Download Your APK
1. Open your repository on GitHub.
2. Click on the **Actions** tab at the top.
3. You will see a workflow running named **Build Android APK** (takes about 60–90 seconds).
4. Once it finishes with a green checkmark, click on it.
5. Scroll down to the **Artifacts** section at the bottom and click **HolePunchIndicator-APK** to download your ready-to-install .apk!

---

## 📲 How to Install & Use on Your Android Phone

1. Transfer the .apk file to your phone (or download it directly from GitHub using Chrome on your phone).
2. Tap the .apk and choose **Install** (allow "Install unknown apps" if prompted).
3. Open **PunchHole Indicator**:
   - Tap **Grant Overlay Permission** -> Toggle **Allow display over other apps**.
   - Tap **Disable Battery Optimization** -> Choose **Allow** (so Android doesn't kill the background indicator).
   - Turn on the **Enable Indicator Overlay** switch!
4. **Calibration**:
   - Turn on **Preview Calibration Marks** to see the alignment crosshairs and guide circle.
   - Use the **Center X / Center Y Offset** sliders to align it to your camera hole.
   - Adjust **Camera Cutout Radius** and **Ring Thickness** to your liking.
   - Turn off calibration mode when satisfied!
